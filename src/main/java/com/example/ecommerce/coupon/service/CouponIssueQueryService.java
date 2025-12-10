package com.example.ecommerce.coupon.service;

import com.example.ecommerce.coupon.domain.UserCoupon;
import com.example.ecommerce.coupon.repository.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 쿠폰 발급 상태 조회 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponIssueQueryService {

    private final CouponRedisService redisService;
    private final UserCouponRepository userCouponRepository;

    /**
     * 쿠폰 발급 상태 조회
     * 1. Redis에서 상태 조회 (빠른 응답)
     * 2. Redis에 없으면 DB에서 조회
     *
     * @param couponId 쿠폰 ID
     * @param userId   사용자 ID
     * @return 발급 상태 응답 DTO
     */
    @Transactional(readOnly = true)
    public CouponIssueStatusResponse getIssueStatus(Long couponId, Long userId) {
        // 1. Redis 상태 조회
        String redisStatus = redisService.getUserStatus(couponId, userId);

        if (redisStatus != null) {
            // PENDING 상태인 경우 대기 순번 포함
            if ("PENDING".equals(redisStatus)) {
                Long waitingRank = redisService.getWaitingRank(couponId, userId);
                long queueSize = redisService.getWaitingQueueSize(couponId);

                return CouponIssueStatusResponse.builder()
                    .status(redisStatus)
                    .waitingRank(waitingRank)
                    .queueSize(queueSize)
                    .message("발급 대기 중입니다.")
                    .build();
            }

            // ISSUED 또는 FAILED 상태
            return CouponIssueStatusResponse.builder()
                .status(redisStatus)
                .message(getMessageForStatus(redisStatus))
                .build();
        }

        // 2. Redis에 없으면 DB 조회
        Optional<UserCoupon> userCoupon = userCouponRepository.findByUserIdAndCouponId(userId, couponId);

        if (userCoupon.isPresent()) {
            return CouponIssueStatusResponse.builder()
                .status("ISSUED")
                .message("쿠폰이 발급되었습니다.")
                .userCouponId(userCoupon.get().getId())
                .build();
        }

        // 3. Redis, DB 모두 없으면 발급 요청 없음
        return CouponIssueStatusResponse.builder()
            .status("NOT_REQUESTED")
            .message("발급 요청 내역이 없습니다.")
            .build();
    }

    /**
     * 상태에 따른 메시지 반환
     *
     * @param status 상태
     * @return 메시지
     */
    private String getMessageForStatus(String status) {
        return switch (status) {
            case "ISSUED" -> "쿠폰이 발급되었습니다.";
            case "PENDING" -> "발급 대기 중입니다.";
            case "FAILED" -> "쿠폰 발급에 실패했습니다.";
            default -> "알 수 없는 상태입니다.";
        };
    }

    /**
     * 쿠폰 발급 상태 응답 DTO
     */
    public static class CouponIssueStatusResponse {
        private String status;          // PENDING, ISSUED, FAILED, NOT_REQUESTED
        private Long waitingRank;       // 대기 순번 (1-based, PENDING 일 때만)
        private Long queueSize;         // 대기열 크기 (PENDING 일 때만)
        private String message;         // 사용자 메시지
        private Long userCouponId;      // 발급된 UserCoupon ID (ISSUED 일 때만)

        public static CouponIssueStatusResponseBuilder builder() {
            return new CouponIssueStatusResponseBuilder();
        }

        public String getStatus() {
            return status;
        }

        public Long getWaitingRank() {
            return waitingRank;
        }

        public Long getQueueSize() {
            return queueSize;
        }

        public String getMessage() {
            return message;
        }

        public Long getUserCouponId() {
            return userCouponId;
        }

        public static class CouponIssueStatusResponseBuilder {
            private String status;
            private Long waitingRank;
            private Long queueSize;
            private String message;
            private Long userCouponId;

            public CouponIssueStatusResponseBuilder status(String status) {
                this.status = status;
                return this;
            }

            public CouponIssueStatusResponseBuilder waitingRank(Long waitingRank) {
                this.waitingRank = waitingRank;
                return this;
            }

            public CouponIssueStatusResponseBuilder queueSize(Long queueSize) {
                this.queueSize = queueSize;
                return this;
            }

            public CouponIssueStatusResponseBuilder message(String message) {
                this.message = message;
                return this;
            }

            public CouponIssueStatusResponseBuilder userCouponId(Long userCouponId) {
                this.userCouponId = userCouponId;
                return this;
            }

            public CouponIssueStatusResponse build() {
                CouponIssueStatusResponse response = new CouponIssueStatusResponse();
                response.status = this.status;
                response.waitingRank = this.waitingRank;
                response.queueSize = this.queueSize;
                response.message = this.message;
                response.userCouponId = this.userCouponId;
                return response;
            }
        }
    }
}
