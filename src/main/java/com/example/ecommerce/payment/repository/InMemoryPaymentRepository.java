package com.example.ecommerce.payment.repository;

import com.example.ecommerce.common.exception.CustomException;
import com.example.ecommerce.common.exception.ErrorCode;
import com.example.ecommerce.payment.domain.Payment;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryPaymentRepository implements PaymentRepository{
    @Override
    public Payment save(Payment payment){
        return Payment.builder().build();
    }

    @Override
    public Optional<Payment> findById(Long id){
        return Optional.ofNullable(Payment.builder().build());
    }

    @Override
    public Payment findByIdOrElseThrow(Long id){
        return findById(id).orElseThrow(()-> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));
    }
}