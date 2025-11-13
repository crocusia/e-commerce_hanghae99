package com.example.ecommerce.coupon.repository;

import com.example.ecommerce.common.exception.CustomException;
import com.example.ecommerce.common.exception.ErrorCode;
import com.example.ecommerce.coupon.domain.UserCoupon;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryUserCouponRepository implements UserCouponRepository {
    private final Map<Long, UserCoupon> storage = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public UserCoupon save(UserCoupon userCoupon) {
        if (userCoupon.getId() == null) {
            // 새로운 사용자 쿠폰 저장
            Long newId = idGenerator.getAndIncrement();
            UserCoupon newUserCoupon = UserCoupon.builder()
                .id(newId)
                .userId(userCoupon.getUserId())
                .couponId(userCoupon.getCouponId())
                .expiresAt(userCoupon.getExpiresAt())
                .build();

            storage.put(newId, newUserCoupon);
            return newUserCoupon;
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
    public Optional<UserCoupon> findByUserIdAndCouponId(Long userId, Long couponId){
        return storage.values().stream()
            .filter(uc -> uc.getUserId().equals(userId)) // 💡 userId 추가
            .filter(uc -> uc.getCouponId().equals(couponId))
            .findFirst();
    }


    @Override
    public UserCoupon findByIdOrElseThrow(Long id){
        return findById(id).orElseThrow(() -> new CustomException(ErrorCode.COUPON_NOT_FOUND));
    }

    @Override
    public List<UserCoupon> findByUserId(Long userId) {
        return storage.values().stream()
            .filter(uc -> uc.getUserId().equals(userId))
            .collect(Collectors.toList());
    }

    // 테스트용 메서드
    public void clear() {
        storage.clear();
        idGenerator.set(1);
    }
}
