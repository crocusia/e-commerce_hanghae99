package com.example.ecommerce.coupon.scheduler;

import com.example.ecommerce.coupon.domain.Coupon;
import com.example.ecommerce.coupon.domain.UserCoupon;
import com.example.ecommerce.coupon.repository.CouponRepository;
import com.example.ecommerce.coupon.service.CouponRedisService;
import com.example.ecommerce.coupon.service.UserCouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponIssueScheduler {

    private final CouponRedisService redisService;
    private final CouponRepository couponRepository;
    private final UserCouponService userCouponService; // 기존 Service 재사용
    private final RedisTemplate<String, String> redisTemplate;

    private static final int MAX_BATCH_SIZE = 100; // 최대 배치 크기

    @Scheduled(fixedDelay = 5000) // 5초마다 실행
    public void processCouponIssue() {
        log.debug("쿠폰 발급 스케줄러 시작");

        try {
            // 활성화된 쿠폰 목록 조회
            List<Coupon> activeCoupons = couponRepository.findByStatus(
                com.example.ecommerce.coupon.domain.status.CouponStatus.ACTIVE);

            if (activeCoupons.isEmpty()) {
                log.debug("활성화된 쿠폰 없음");
                return;
            }

            for (Coupon coupon : activeCoupons) {
                processCouponQueue(coupon);
            }

        } catch (Exception e) {
            log.error("쿠폰 발급 스케줄러 실행 중 오류", e);
        }

        log.debug("쿠폰 발급 스케줄러 종료");
    }

    /**
     * 특정 쿠폰의 대기열 처리
     *
     * @param coupon 쿠폰
     */
    private void processCouponQueue(Coupon coupon) {
        Long couponId = coupon.getId();

        try {
            // 1. 대기열 크기 확인
            long queueSize = redisService.getWaitingQueueSize(couponId);
            if (queueSize == 0) {
                log.debug("대기열 없음 - couponId: {}", couponId);
                return;
            }

            // 2. 남은 수량 계산 (Redis 카운터와 DB 발급 수량 동기화)
            long redisCount = redisService.getCurrentCount(couponId);
            int dbIssuedCount = coupon.getQuantity().getIssuedQuantity();
            int totalQuantity = coupon.getQuantity().getTotalQuantity();

            // Redis와 DB 동기화 (Redis가 더 정확한 실시간 값)
            int remainingCount = Math.max(0, totalQuantity - (int) redisCount);

            if (remainingCount == 0) {
                log.debug("발급 가능 수량 없음 - couponId: {}, total: {}, redis: {}, db: {}",
                    couponId, totalQuantity, redisCount, dbIssuedCount);
                return;
            }

            // 3. 배치 크기 결정 (남은 수량과 최대 배치 크기 중 작은 값)
            int batchSize = Math.min(remainingCount, MAX_BATCH_SIZE);
            batchSize = Math.min(batchSize, (int) queueSize);

            log.info("쿠폰 발급 배치 시작 - couponId: {}, batchSize: {}, remaining: {}, queueSize: {}",
                couponId, batchSize, remainingCount, queueSize);

            // 4. 대기열에서 배치 추출 (ZPOPMIN)
            List<Long> userIds = redisService.popFromWaitingQueue(couponId, batchSize);

            if (userIds.isEmpty()) {
                log.warn("대기열 추출 실패 - couponId: {}", couponId);
                return;
            }

            // 5. 기존 UserCouponService로 DB 발급 (재사용!)
            List<UserCoupon> issuedCoupons = bulkIssueCouponsUsingService(coupon.getId(), userIds);

            // 6. Redis 상태 업데이트 (Pipeline 사용)
            updateUserStatusBatch(couponId, userIds, "ISSUED");

            log.info("쿠폰 발급 배치 완료 - couponId: {}, issued: {}, failed: {}",
                couponId, issuedCoupons.size(), userIds.size() - issuedCoupons.size());

        } catch (Exception e) {
            log.error("쿠폰 발급 처리 실패 - couponId: {}", couponId, e);
            // TODO: 실패한 요청을 Dead Letter Queue에 기록하거나 재시도 로직 추가
        }
    }

    /**
     * 벌크 쿠폰 발급 (기존 UserCouponService 재사용)
     *
     * @param couponId 쿠폰 ID
     * @param userIds  사용자 ID 목록
     * @return 발급된 UserCoupon 목록
     */
    private List<UserCoupon> bulkIssueCouponsUsingService(Long couponId, List<Long> userIds) {
        List<UserCoupon> userCoupons = new ArrayList<>();

        for (Long userId : userIds) {
            try {
                // 기존 UserCouponService 재사용!
                UserCoupon userCoupon = userCouponService.issueCouponAsync(couponId, userId);
                userCoupons.add(userCoupon);

            } catch (Exception e) {
                log.error("쿠폰 발급 실패 - couponId: {}, userId: {}", couponId, userId, e);
                // TODO: 실패한 개별 발급을 Dead Letter Queue에 기록
            }
        }

        return userCoupons;
    }

    /**
     * Redis Pipeline을 사용한 상태 일괄 업데이트
     *
     * @param couponId 쿠폰 ID
     * @param userIds  사용자 ID 목록
     * @param status   상태 ("ISSUED", "FAILED")
     */
    private void updateUserStatusBatch(Long couponId, List<Long> userIds, String status) {
        try {
            redisTemplate.executePipelined((org.springframework.data.redis.core.RedisCallback<?>) connection -> {
                for (Long userId : userIds) {
                    redisService.setUserStatus(couponId, userId, status);
                }
                return null;
            });

            log.debug("상태 일괄 업데이트 완료 - couponId: {}, count: {}, status: {}",
                couponId, userIds.size(), status);

        } catch (Exception e) {
            log.error("상태 일괄 업데이트 실패 - couponId: {}", couponId, e);
        }
    }
}
