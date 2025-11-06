package com.example.ecommerce.coupon.repository;

import com.example.ecommerce.coupon.domain.UserCoupon;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class InMemoryUserCouponRepository implements UserCouponRepository {

    private final Map<Long, UserCoupon> storage = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public UserCoupon save(UserCoupon userCoupon) {
        if (userCoupon.getId() == null) {
            // 새로운 사용자 쿠폰 저장
            Long newId = idGenerator.getAndIncrement();
            try {
                var constructor = userCoupon.getClass().getDeclaredConstructor();
                constructor.setAccessible(true);
                UserCoupon newUserCoupon = constructor.newInstance();

                // 리플렉션으로 id 설정
                var idField = userCoupon.getClass().getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(newUserCoupon, newId);

                // 나머지 필드 복사
                copyFields(userCoupon, newUserCoupon);

                storage.put(newId, newUserCoupon);
                return newUserCoupon;
            } catch (Exception e) {
                throw new RuntimeException("사용자 쿠폰 저장 중 오류 발생", e);
            }
        } else {
            // 기존 사용자 쿠폰 업데이트
            storage.put(userCoupon.getId(), userCoupon);
            return userCoupon;
        }
    }

    @Override
    public Optional<UserCoupon> findById(Long id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public Optional<UserCoupon> findByUserIdAndCouponId(Long userId, Long couponId) {
        return storage.values().stream()
            .filter(uc -> uc.getUserId().equals(userId) && uc.getCoupon().getId().equals(couponId))
            .findFirst();
    }

    @Override
    public List<UserCoupon> findByUserId(Long userId) {
        return storage.values().stream()
            .filter(uc -> uc.getUserId().equals(userId))
            .collect(Collectors.toList());
    }

    @Override
    public boolean existsByUserIdAndCouponId(Long userId, Long couponId) {
        return storage.values().stream()
            .anyMatch(uc -> uc.getUserId().equals(userId) && uc.getCoupon().getId().equals(couponId));
    }

    @Override
    public List<UserCoupon> findByCouponIdsAndUnusedStatus(List<Long> couponIds) {
        return storage.values().stream()
            .filter(uc -> couponIds.contains(uc.getCoupon().getId()))
            .filter(uc -> uc.getStatus() == com.example.ecommerce.coupon.domain.UserCouponStatus.UNUSED)
            .collect(java.util.stream.Collectors.toList());
    }

    private void copyFields(UserCoupon source, UserCoupon target) throws Exception {
        var fields = source.getClass().getDeclaredFields();
        for (var field : fields) {
            if (field.getName().equals("id")) continue;
            field.setAccessible(true);
            field.set(target, field.get(source));
        }
    }

    // 테스트용 메서드
    public void clear() {
        storage.clear();
        idGenerator.set(1);
    }
}
