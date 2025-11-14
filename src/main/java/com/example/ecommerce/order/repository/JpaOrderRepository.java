package com.example.ecommerce.order.repository;

import com.example.ecommerce.common.exception.CustomException;
import com.example.ecommerce.common.exception.ErrorCode;
import com.example.ecommerce.order.domain.Order;
import com.example.ecommerce.order.domain.status.OrderStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaOrderRepository extends JpaRepository<Order, Long>, OrderRepository {

    @Override
    Order save(Order order);

    @Override
    Optional<Order> findById(Long id);

    @Override
    default Order findByIdOrElseThrow(Long id) {
        return findById(id)
            .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));
    }

    @Override
    default void delete(Long id) {
        Order order = findByIdOrElseThrow(id);
        delete(order);
    }

    @Override
    List<Order> findByStatus(OrderStatus status);
}
