package com.example.ecommerce.coupon.event;

import com.example.ecommerce.coupon.domain.UserCoupon;
import com.example.ecommerce.coupon.repository.UserCouponRepository;
import com.example.ecommerce.coupon.service.UserCouponService;
import com.example.ecommerce.order.domain.Order;
import com.example.ecommerce.order.repository.OrderRepository;
import com.example.ecommerce.payment.event.PaymentCompletedEvent;
import com.example.ecommerce.payment.event.PaymentFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
@org.springframework.core.annotation.Order(3)  // 마지막으로 실행
public class UserCouponEventListener {

    private final UserCouponService userCouponService;
    private final UserCouponRepository userCouponRepository;
    private final OrderRepository orderRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        log.info("결제 완료 이벤트 수신 - 쿠폰 사용 처리 시작, orderId: {}", event.orderId());

        try {
            Order order = orderRepository.findByIdOrElseThrow(event.orderId());

            // 쿠폰을 사용한 주문인 경우에만 처리
            if (order.getUserCouponId() != null) {
                userCouponService.useCoupon(order.getUserCouponId());
                log.info("쿠폰 사용 처리 완료 - orderId: {}, userCouponId: {}",
                    event.orderId(), order.getUserCouponId());
            } else {
                log.debug("쿠폰을 사용하지 않은 주문 - orderId: {}", event.orderId());
            }

        } catch (Exception e) {
            log.error("쿠폰 사용 처리 실패 - orderId: {}, error: {}", event.orderId(), e.getMessage(), e);
            throw e;
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePaymentFailed(PaymentFailedEvent event) {
        log.warn("결제 실패 이벤트 수신 - 쿠폰 예약 해제 시작, orderId: {}", event.orderId());

        try {
            Order order = orderRepository.findByIdOrElseThrow(event.orderId());

            // 쿠폰을 사용한 주문인 경우에만 처리
            if (order.getUserCouponId() != null) {
                UserCoupon userCoupon = userCouponRepository.findByIdOrElseThrow(order.getUserCouponId());
                userCoupon.cancelReservation();
                userCouponRepository.save(userCoupon);

                log.info("쿠폰 예약 해제 완료 - orderId: {}, userCouponId: {}",
                    event.orderId(), order.getUserCouponId());
            } else {
                log.debug("쿠폰을 사용하지 않은 주문 - orderId: {}", event.orderId());
            }

        } catch (Exception e) {
            log.error("쿠폰 예약 해제 실패 - orderId: {}, error: {}", event.orderId(), e.getMessage(), e);
            throw e;
        }
    }
}
