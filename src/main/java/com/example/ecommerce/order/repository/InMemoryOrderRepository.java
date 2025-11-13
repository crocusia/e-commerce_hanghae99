package com.example.ecommerce.order.repository;

import com.example.ecommerce.common.exception.CustomException;
import com.example.ecommerce.common.exception.ErrorCode;
import com.example.ecommerce.order.domain.Order;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryOrderRepository implements OrderRepository {

    private final Map<Long, Order> store = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1L);

    @Override
    public Order save(Order order) {
        if (order.getId() == null) {
            Long newId = idGenerator.getAndIncrement();
            Order newOrder = Order.builder()
                .id(newId)
                .userId(order.getUserId())
                .userCouponId(order.getUserCouponId())
                .discountAmount(order.getDiscountAmount())
                .orderItems(order.getOrderItems())
                .build();
            store.put(newId, newOrder);
            return newOrder;
        }

        store.put(order.getId(), order);
        return order;
    }

    @Override
    public Optional<Order> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Order findByIdOrElseThrow(Long id) {
        return findById(id)
            .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));
    }

    @Override
    public void delete(Long id) {
        store.remove(id);
    }
}
