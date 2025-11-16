package com.example.ecommerce.payment.repository;

import com.example.ecommerce.common.exception.CustomException;
import com.example.ecommerce.common.exception.ErrorCode;
import com.example.ecommerce.payment.domain.Payment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaPaymentRepository extends JpaRepository<Payment, Long>, PaymentRepository {

    @Override
    Payment save(Payment payment);

    @Override
    Optional<Payment> findById(Long id);

    @Override
    default Payment findByIdOrElseThrow(Long id) {
        return findById(id)
            .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));
    }
}
