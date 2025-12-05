package com.example.ecommerce.coupon.facade;

import com.example.ecommerce.common.exception.CustomException;
import com.example.ecommerce.common.exception.ErrorCode;
import com.example.ecommerce.coupon.domain.Coupon;
import com.example.ecommerce.coupon.repository.CouponRepository;
import com.example.ecommerce.coupon.service.CouponRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponIssueFacade {

    private final CouponRedisService redisService;
    private final CouponRepository couponRepository;

    public void issueRequest(Long couponId, Long userId) {
        // 1. 쿠폰 유효성 검증
        Coupon coupon = couponRepository.findByIdOrElseThrow(couponId);

        if (!coupon.canIssue()) {
            log.warn("발급 불가능한 쿠폰 - couponId: {}, userId: {}", couponId, userId);
            throw new CustomException(ErrorCode.COUPON_NOT_AVAILABLE);
        }

        int totalQuantity = coupon.getQuantity().getTotalQuantity();

        // 2. 수량 예약 (INCR)
        boolean quantityReserved = redisService.reserveQuantity(couponId, totalQuantity);

        if (!quantityReserved) {
            log.warn("수량 초과로 발급 실패 - couponId: {}, userId: {}", couponId, userId);
            throw new CustomException(ErrorCode.COUPON_NOT_AVAILABLE, "선착순 마감되었습니다.");
        }

        // 3. 중복 체크 (SADD)
        boolean isNew = redisService.checkDuplicate(couponId, userId);

        if (!isNew) {
            // 중복 발급 시도 시 수량 롤백 (DECR)
            redisService.rollbackQuantity(couponId);
            log.warn("중복 발급 시도 - couponId: {}, userId: {}", couponId, userId);
            throw new CustomException(ErrorCode.COUPON_ALREADY_ISSUED, "이미 발급 요청한 쿠폰입니다.");
        }

        // 4. 대기열 추가 (ZADD) - timestamp를 score로 사용하여 FIFO 보장
        long timestamp = System.currentTimeMillis();
        boolean addedToQueue = redisService.addToWaitingQueue(couponId, userId, timestamp);

        if (!addedToQueue) {
            // 대기열 추가 실패 시 수량 롤백 (DECR)
            redisService.rollbackQuantity(couponId);
            log.error("대기열 추가 실패 - couponId: {}, userId: {}", couponId, userId);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "발급 요청 처리 중 오류가 발생했습니다.");
        }

        // 5. 상태 저장 (SET with TTL 24시간)
        redisService.setUserStatus(couponId, userId, "PENDING");

        log.info("쿠폰 발급 요청 접수 완료 - couponId: {}, userId: {}, timestamp: {}",
            couponId, userId, timestamp);
    }
}
