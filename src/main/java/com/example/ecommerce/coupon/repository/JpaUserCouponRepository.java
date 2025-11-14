package com.example.ecommerce.coupon.repository;

import com.example.ecommerce.common.exception.CustomException;
import com.example.ecommerce.common.exception.ErrorCode;
import com.example.ecommerce.coupon.domain.UserCoupon;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaUserCouponRepository extends JpaRepository<UserCoupon, Long>, UserCouponRepository {

    @Override
    UserCoupon save(UserCoupon userCoupon);

    @Override
    Optional<UserCoupon> findById(Long id);

    @Override
    default UserCoupon findByIdOrElseThrow(Long id) {
        return findById(id)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_COUPON_NOT_FOUND));
    }

    @Override
    Optional<UserCoupon> findByUserIdAndCouponId(Long userId, Long couponId);

    @Override
    List<UserCoupon> findByUserId(Long userId);
}
