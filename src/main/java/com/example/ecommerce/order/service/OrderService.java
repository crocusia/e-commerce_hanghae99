package com.example.ecommerce.order.service;

import com.example.ecommerce.common.exception.CustomException;
import com.example.ecommerce.common.exception.ErrorCode;
import com.example.ecommerce.coupon.domain.Coupon;
import com.example.ecommerce.coupon.domain.UserCoupon;
import com.example.ecommerce.coupon.repository.CouponRepository;
import com.example.ecommerce.coupon.repository.UserCouponRepository;
import com.example.ecommerce.order.domain.Order;
import com.example.ecommerce.order.domain.OrderItem;
import com.example.ecommerce.order.dto.OrderItemRequest;
import com.example.ecommerce.order.dto.OrderRequest;
import com.example.ecommerce.order.dto.OrderResponse;
import com.example.ecommerce.order.repository.OrderRepository;
import com.example.ecommerce.product.domain.Product;
import com.example.ecommerce.product.domain.vo.Money;
import com.example.ecommerce.product.repository.ProductRepository;
import com.example.ecommerce.product.service.StockService;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final StockService stockService;
    private final UserCouponRepository userCouponRepository;
    private final CouponRepository couponRepository;

    public OrderResponse createOrder(OrderRequest request) {
        List<OrderItem> orderItems = new ArrayList<>();
        //상품 스냅샷
        for (OrderItemRequest itemRequest : request.orderItems()) {
            Product product = productRepository.findByIdOrElseThrow(itemRequest.productId());

            if (!product.isAvailable()) {
                throw new CustomException(ErrorCode.INVALID_PRODUCT_STATUS);
            }
            OrderItem orderItem = OrderItem.create(
                product.getProductId(),
                product.getName(),
                itemRequest.quantity(),
                product.getPrice().getAmount()
            );
            orderItems.add(orderItem);
        }

        Order order = Order.create(request.userId(), orderItems);
        Order savedOrder = orderRepository.save(order);

        //상품 예약
        try {
            for (OrderItemRequest itemRequest : request.orderItems()) {
                stockService.reserve(
                    savedOrder.getId(),
                    itemRequest.productId(),
                    itemRequest.quantity()
                );
            }
        } catch (CustomException e) {
            //예약 실패 시 이미 예약한 재고 해제 및 주문 삭제
            try {
                stockService.release(savedOrder.getId());
            } catch (Exception releaseException) {
                log.error("Failed to release stock reservations for order: {}",
                    savedOrder.getId(), releaseException);
            }

            try {
                orderRepository.delete(savedOrder.getId());
            } catch (Exception deleteException) {
                log.error("Failed to delete order: {}", savedOrder.getId(), deleteException);
            }

            throw e;
        }
        //주문 응답 반환
        return OrderResponse.from(savedOrder);
    }

    public OrderResponse applyCoupon(Long orderId, Long userCouponId) {
        Order order = orderRepository.findByIdOrElseThrow(orderId);
        UserCoupon userCoupon = userCouponRepository.findByIdOrElseThrow(userCouponId);

        if (!userCoupon.getUserId().equals(order.getUserId())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        if (!userCoupon.canUse()) {
            throw new CustomException(ErrorCode.COUPON_NOT_AVAILABLE);
        }

        Coupon coupon = couponRepository.findByIdOrElseThrow(userCoupon.getCouponId());

        Money orderAmount = Money.of(order.getTotalAmount());
        Money discountAmount = coupon.calculateDiscountAmount(orderAmount);

        order.applyCoupon(userCouponId, discountAmount.getAmount());

        userCoupon.use();

        Order savedOrder = orderRepository.save(order);
        userCouponRepository.save(userCoupon);

        return OrderResponse.from(savedOrder);
    }

    public OrderResponse getOrder(Long orderId) {
        Order order = orderRepository.findByIdOrElseThrow(orderId);
        return OrderResponse.from(order);
    }
}
