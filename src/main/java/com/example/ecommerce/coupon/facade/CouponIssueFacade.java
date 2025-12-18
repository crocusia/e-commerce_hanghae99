package com.example.ecommerce.coupon.facade;

import com.example.ecommerce.common.exception.CustomException;
import com.example.ecommerce.common.exception.ErrorCode;
import com.example.ecommerce.coupon.domain.Coupon;
import com.example.ecommerce.coupon.event.CouponIssueRequestEvent;
import com.example.ecommerce.coupon.repository.CouponRepository;
import com.example.ecommerce.coupon.service.CouponRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponIssueFacade {

    private final CouponRedisService redisService;
    private final CouponRepository couponRepository;
    private final KafkaTemplate<String, CouponIssueRequestEvent> couponIssueKafkaTemplate;

    private static final String COUPON_ISSUE_TOPIC = "coupon-issue-requests";

    @Transactional(readOnly = true)
    public void issueRequest(Long couponId, Long userId) {
        // 1. 쿠폰 유효성 검증 (DB)
        Coupon coupon = couponRepository.findByIdOrElseThrow(couponId);

        if (!coupon.canIssue()) {
            log.warn("발급 불가능한 쿠폰 - couponId: {}, userId: {}", couponId, userId);
            throw new CustomException(ErrorCode.COUPON_NOT_AVAILABLE);
        }

        int totalQuantity = coupon.getQuantity().getTotalQuantity();

        // 2. 수량 예약 (INCR) - 빠른 수량 제어
        boolean quantityReserved = redisService.reserveQuantity(couponId, totalQuantity);

        if (!quantityReserved) {
            log.warn("수량 초과로 발급 실패 - couponId: {}, userId: {}", couponId, userId);
            throw new CustomException(ErrorCode.COUPON_NOT_AVAILABLE, "선착순 마감되었습니다.");
        }

        // 3. 중복 체크 (SADD) - 빠른 중복 방지
        boolean isNew = redisService.checkDuplicate(couponId, userId);

        if (!isNew) {
            // 중복 발급 시도 시 수량 롤백 (DECR)
            redisService.rollbackQuantity(couponId);
            log.warn("중복 발급 시도 - couponId: {}, userId: {}", couponId, userId);
            throw new CustomException(ErrorCode.COUPON_ALREADY_ISSUED, "이미 발급 요청한 쿠폰입니다.");
        }

        // 4. Kafka 이벤트 발행 (비동기)
        CouponIssueRequestEvent event = CouponIssueRequestEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType("COUPON_ISSUE_REQUESTED")
            .occurredAt(LocalDateTime.now())
            .couponId(couponId)
            .userId(userId)
            .timestamp(System.currentTimeMillis())
            .build();

        try {
            // Key: couponId (파티셔닝 기준 - 동일 쿠폰은 같은 파티션으로)
            String key = couponId.toString();
            couponIssueKafkaTemplate.send(COUPON_ISSUE_TOPIC, key, event);

            // 5. 상태 저장 (SET with TTL 24시간)
            redisService.setUserStatus(couponId, userId, "PENDING");

            log.info("쿠폰 발급 요청 Kafka 발행 완료 - couponId: {}, userId: {}", couponId, userId);

        } catch (Exception e) {
            // Kafka 발행 실패 시 Redis 롤백
            redisService.rollbackQuantity(couponId);
            redisService.removeDuplicate(couponId, userId);
            log.error("Kafka 발행 실패 - couponId: {}, userId: {}", couponId, userId, e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "발급 요청 처리 중 오류가 발생했습니다.");
        }
    }
}
