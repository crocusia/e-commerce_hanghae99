package com.example.ecommerce.coupon.consumer;

import com.example.ecommerce.coupon.event.CouponIssueDlqEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponIssueDlqConsumer {

    @KafkaListener(
        topics = "coupon-issue-dlq",
        groupId = "coupon-issue-dlq-monitor-group"
    )
    public void consumeDeadLetterQueue(
        @Payload CouponIssueDlqEvent event,
        Acknowledgment ack
    ) {
        log.error("[DLQ Monitor] 쿠폰 발급 최종 실패 - couponId: {}, userId: {}, reason: {}, retryCount: {}",
            event.getOriginalEvent().getCouponId(),
            event.getOriginalEvent().getUserId(),
            event.getFailureReason(),
            event.getRetryCount());

        // TODO: Slack 알림 전송
        // slackService.sendAlert(...)

        // TODO: 관리자 대시보드에 표시 또는 DB에 저장

        if (ack != null) {
            ack.acknowledge();
        }
    }
}
