package com.example.ecommerce.payment.repository;

import com.example.ecommerce.payment.domain.Payment;
import java.util.Optional;

public interface PaymentRepository {
    Payment save(Payment payment);

    Optional<Payment> findById(Long id);

    Payment findByIdOrElseThrow(Long id);

    Optional<Payment> findByOrderId(Long orderId);

    Payment findByOrderIdOrElseThrow(Long orderId);

    void deleteAllInBatch();
}
