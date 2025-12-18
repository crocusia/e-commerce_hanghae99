package com.example.ecommerce.coupon.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponRedisService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String COUNTER_KEY_PREFIX = "coupon:";
    private static final String COUNTER_KEY_SUFFIX = ":counter";
    private static final String ISSUED_USERS_KEY_SUFFIX = ":issued:users";
    private static final String USER_STATUS_KEY_PREFIX = "coupon:";
    private static final String USER_STATUS_KEY_SUFFIX = ":user:";
    private static final String STATUS_KEY_SUFFIX = ":status";

    private static final long STATUS_TTL_SECONDS = 86400L; // 24시간

    public boolean reserveQuantity(Long couponId, int limit) {
        try {
            String key = getCounterKey(couponId);
            Long count = redisTemplate.opsForValue().increment(key);

            if (count != null && count <= limit) {
                log.debug("수량 예약 성공 - couponId: {}, count: {}/{}", couponId, count, limit);
                return true;
            }

            // 수량 초과 시 롤백 (DECR)
            redisTemplate.opsForValue().decrement(key);
            log.warn("수량 초과로 예약 실패 - couponId: {}, count: {}/{}", couponId, count, limit);
            return false;

        } catch (Exception e) {
            log.error("수량 예약 실패 - couponId: {}", couponId, e);
            return false;
        }
    }

    public void rollbackQuantity(Long couponId) {
        try {
            String key = getCounterKey(couponId);
            Long count = redisTemplate.opsForValue().decrement(key);
            log.debug("수량 예약 롤백 - couponId: {}, remaining: {}", couponId, count);

        } catch (Exception e) {
            log.error("수량 예약 롤백 실패 - couponId: {}", couponId, e);
        }
    }

    public boolean checkDuplicate(Long couponId, Long userId) {
        try {
            String key = getIssuedUsersKey(couponId);
            Long result = redisTemplate.opsForSet().add(key, userId.toString());

            boolean isNew = result != null && result == 1;
            if (isNew) {
                log.debug("중복 체크 통과 - couponId: {}, userId: {}", couponId, userId);
            } else {
                log.warn("중복 발급 시도 차단 - couponId: {}, userId: {}", couponId, userId);
            }

            return isNew;

        } catch (Exception e) {
            log.error("중복 체크 실패 - couponId: {}, userId: {}", couponId, userId, e);
            return false;
        }
    }

    /**
     * Set에서 중복 체크 제거 (Kafka 발행 실패 시 롤백용)
     *
     * @param couponId 쿠폰 ID
     * @param userId   사용자 ID
     */
    public void removeDuplicate(Long couponId, Long userId) {
        try {
            String key = getIssuedUsersKey(couponId);
            redisTemplate.opsForSet().remove(key, userId.toString());
            log.debug("중복 체크 롤백 - couponId: {}, userId: {}", couponId, userId);

        } catch (Exception e) {
            log.error("중복 체크 롤백 실패 - couponId: {}, userId: {}", couponId, userId, e);
        }
    }

    public void setUserStatus(Long couponId, Long userId, String status) {
        try {
            String key = getUserStatusKey(couponId, userId);
            redisTemplate.opsForValue().set(key, status, Duration.ofSeconds(STATUS_TTL_SECONDS));
            log.debug("사용자 상태 저장 - couponId: {}, userId: {}, status: {}", couponId, userId, status);

        } catch (Exception e) {
            log.error("사용자 상태 저장 실패 - couponId: {}, userId: {}", couponId, userId, e);
        }
    }

    public String getUserStatus(Long couponId, Long userId) {
        try {
            String key = getUserStatusKey(couponId, userId);
            return redisTemplate.opsForValue().get(key);

        } catch (Exception e) {
            log.error("사용자 상태 조회 실패 - couponId: {}, userId: {}", couponId, userId, e);
            return null;
        }
    }

    /**
     * 현재 발급된 수량 조회 (GET)
     *
     * @param couponId 쿠폰 ID
     * @return 현재 카운터 값
     */
    public long getCurrentCount(Long couponId) {
        try {
            String key = getCounterKey(couponId);
            String value = redisTemplate.opsForValue().get(key);
            return value != null ? Long.parseLong(value) : 0L;

        } catch (Exception e) {
            log.error("현재 수량 조회 실패 - couponId: {}", couponId, e);
            return 0L;
        }
    }

    /**
     * Redis 카운터 초기화
     * 테스트 또는 관리 목적
     *
     * @param couponId 쿠폰 ID
     */
    public void resetCounter(Long couponId) {
        try {
            String counterKey = getCounterKey(couponId);
            String usersKey = getIssuedUsersKey(couponId);

            redisTemplate.delete(counterKey);
            redisTemplate.delete(usersKey);

            log.info("Redis 카운터 초기화 완료 - couponId: {}", couponId);

        } catch (Exception e) {
            log.error("Redis 카운터 초기화 실패 - couponId: {}", couponId, e);
        }
    }

    // Key 생성 헬퍼 메서드
    private String getCounterKey(Long couponId) {
        return COUNTER_KEY_PREFIX + couponId + COUNTER_KEY_SUFFIX;
    }

    private String getIssuedUsersKey(Long couponId) {
        return COUNTER_KEY_PREFIX + couponId + ISSUED_USERS_KEY_SUFFIX;
    }

    private String getUserStatusKey(Long couponId, Long userId) {
        return USER_STATUS_KEY_PREFIX + couponId + USER_STATUS_KEY_SUFFIX + userId + STATUS_KEY_SUFFIX;
    }
}
