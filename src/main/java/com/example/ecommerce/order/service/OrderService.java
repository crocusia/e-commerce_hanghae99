package com.example.ecommerce.order.service;

import com.example.ecommerce.common.aop.OptimisticLock;
import com.example.ecommerce.common.event.MessagePublisher;
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
import com.example.ecommerce.order.event.OrderCreatedEvent;
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
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserCouponRepository userCouponRepository;
    private final CouponRepository couponRepository;

    @Transactional
    public Order createOrderEntity(OrderRequest request) {
        List<OrderItem> orderItems = new ArrayList<>();

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

        return orderRepository.save(order);
    }

    @OptimisticLock(maxRetries = 3, retryDelay = 100)
    @Transactional
    public OrderResponse applyCoupon(Long orderId, Long userCouponId) {
        log.info("쿠폰 적용 시작 - orderId: {}, userCouponId: {}", orderId, userCouponId);

        Order order = orderRepository.findByIdOrElseThrow(orderId);

        // 기존 쿠폰이 있으면 복원
        Long previousCouponId = order.cancelCoupon();
        if (previousCouponId != null) {
            UserCoupon previousCoupon = userCouponRepository.findByIdOrElseThrow(previousCouponId);
            previousCoupon.cancelReservation();
            userCouponRepository.save(previousCoupon);
        }

        // 사용자 쿠폰 조회 (낙관적 락으로 조회)
        UserCoupon userCoupon = userCouponRepository.findByIdOrElseThrow(userCouponId);

        if (!userCoupon.getUserId().equals(order.getUserId())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "본인의 쿠폰만 사용할 수 있습니다.");
        }

        userCoupon.reserve();

        Coupon coupon = couponRepository.findByIdOrElseThrow(userCoupon.getCoupon().getId());

        Money orderAmount = Money.of(order.getTotalAmount());
        Money discountAmount = coupon.calculateDiscountAmount(orderAmount);

        order.applyCoupon(userCouponId, discountAmount.getAmount());

        Order savedOrder = orderRepository.save(order);
        userCouponRepository.save(userCoupon);

        log.info("쿠폰 적용 완료 - orderId: {}, userCouponId: {}, discountAmount: {}",
            orderId, userCouponId, discountAmount.getAmount());

        return OrderResponse.from(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long orderId) {
        Order order = orderRepository.findByIdOrElseThrow(orderId);
        return OrderResponse.from(order);
    }
}
