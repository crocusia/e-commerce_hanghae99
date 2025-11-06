package com.example.ecommerce.coupon.repository;

import com.example.ecommerce.coupon.domain.Coupon;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryCouponRepository implements CouponRepository {

    private final Map<Long, Coupon> storage = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public Coupon save(Coupon coupon) {
        if (coupon.getId() == null) {
            // 새로운 쿠폰 저장
            Long newId = idGenerator.getAndIncrement();
            try {
                var constructor = coupon.getClass().getDeclaredConstructor();
                constructor.setAccessible(true);
                Coupon newCoupon = constructor.newInstance();

                // 리플렉션으로 id 설정
                var idField = coupon.getClass().getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(newCoupon, newId);

                // 나머지 필드 복사
                copyFields(coupon, newCoupon);

                storage.put(newId, newCoupon);
                return newCoupon;
            } catch (Exception e) {
                throw new RuntimeException("쿠폰 저장 중 오류 발생", e);
            }
        } else {
            // 기존 쿠폰 업데이트
            storage.put(coupon.getId(), coupon);
            return coupon;
        }
    }

    @Override
    public Optional<Coupon> findById(Long id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<Coupon> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public List<Long> findExpiredCouponIds() {
        return storage.values().stream()
            .filter(coupon -> !coupon.isValid())
            .map(Coupon::getId)
            .toList();
    }

    private void copyFields(Coupon source, Coupon target) throws Exception {
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
