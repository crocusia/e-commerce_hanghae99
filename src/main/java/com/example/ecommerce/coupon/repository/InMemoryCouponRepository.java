package com.example.ecommerce.coupon.repository;

import com.example.ecommerce.common.exception.CustomException;
import com.example.ecommerce.common.exception.ErrorCode;
import com.example.ecommerce.coupon.domain.Coupon;
import com.example.ecommerce.coupon.domain.status.CouponStatus;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryCouponRepository implements CouponRepository {
    private final Map<Long, Coupon> storage = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public Coupon save(Coupon coupon) {
        if (coupon.getId() == null) {
            // 새로운 쿠폰 저장
            Long newId = idGenerator.getAndIncrement();
            Coupon newCoupon = Coupon.builder()
                .id(newId)
                .name(coupon.getName())
                .discountValue(coupon.getDiscountValue())
                .quantity(coupon.getQuantity())
                .validPeriod(coupon.getValidPeriod())
                .minOrderAmount(coupon.getMinOrderAmount())
                .build();

            storage.put(newId, newCoupon);
            return newCoupon;
        } else {
            storage.put(coupon.getId(), coupon);
            return coupon;
        }
    }

    @Override
    public Optional<Coupon> findById(Long id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public Coupon findByIdOrElseThrow(Long id) {
        return findById(id).orElseThrow(()-> new CustomException(ErrorCode.COUPON_NOT_FOUND));
    }


    @Override
    public List<Coupon> findByStatus(CouponStatus status) {
        return List.of();
    }

    // 테스트용 메서드
    public void clear() {
        storage.clear();
        idGenerator.set(1);
    }
}
