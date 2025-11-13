package com.example.ecommerce.order.repository;

import com.example.ecommerce.order.domain.Order;
import java.util.Optional;

public interface OrderRepository {
    Order save(Order order);

    Optional<Order> findById(Long id);

    Order findByIdOrElseThrow(Long id);

    void delete(Long id);
}
