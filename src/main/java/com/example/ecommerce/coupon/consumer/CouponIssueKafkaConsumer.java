package com.example.ecommerce.coupon.consumer;

import com.example.ecommerce.coupon.domain.UserCoupon;
import com.example.ecommerce.coupon.event.CouponIssueDlqEvent;
import com.example.ecommerce.coupon.event.CouponIssueRequestEvent;
import com.example.ecommerce.coupon.service.CouponRedisService;
import com.example.ecommerce.coupon.service.UserCouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponIssueKafkaConsumer {

    private final UserCouponService userCouponService;
    private final CouponRedisService redisService;
    private final KafkaTemplate<String, Object> dlqKafkaTemplate;

    private static final String DLQ_TOPIC = "coupon-issue-dlq";
    private static final int MAX_RETRY_COUNT = 3;

    @KafkaListener(
        topics = "coupon-issue-requests",
        groupId = "coupon-issue-group",
        containerFactory = "couponIssueKafkaListenerContainerFactory"
    )
    public void consumeCouponIssueRequest(
        @Payload CouponIssueRequestEvent event,
        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
        @Header(KafkaHeaders.OFFSET) long offset,
        Acknowledgment ack
    ) {
        log.info("[Kafka Consumer] 쿠폰 발급 요청 수신 - couponId: {}, userId: {}, partition: {}, offset: {}",
            event.getCouponId(), event.getUserId(), partition, offset);

        int retryCount = 0;
        boolean success = false;
        Exception lastException = null;

        // 재시도 로직 (최대 3회)
        while (retryCount < MAX_RETRY_COUNT && !success) {
            try {
                // DB 쿠폰 발급 처리
                UserCoupon userCoupon = userCouponService.issueCouponAsync(
                    event.getCouponId(),
                    event.getUserId()
                );

                // Redis 상태 업데이트
                redisService.setUserStatus(event.getCouponId(), event.getUserId(), "ISSUED");

                log.info("[Kafka Consumer] 쿠폰 발급 완료 - couponId: {}, userId: {}, userCouponId: {}",
                    event.getCouponId(), event.getUserId(), userCoupon.getId());

                success = true;

            } catch (Exception e) {
                retryCount++;
                lastException = e;

                log.warn("[Kafka Consumer] 쿠폰 발급 실패 (재시도 {}/{}) - couponId: {}, userId: {}, error: {}",
                    retryCount, MAX_RETRY_COUNT, event.getCouponId(), event.getUserId(), e.getMessage());

                if (retryCount < MAX_RETRY_COUNT) {
                    // 지수 백오프 (Exponential Backoff)
                    try {
                        Thread.sleep(1000L * retryCount); // 1초, 2초, 3초
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        if (success) {
            // 성공 시 Offset Commit (수동 ACK)
            if (ack != null) {
                ack.acknowledge();
            }
            log.info("[Kafka Consumer] Offset Commit 완료 - partition: {}, offset: {}", partition, offset);

        } else {
            // 최종 실패 시 DLQ로 전송
            sendToDeadLetterQueue(event, lastException, retryCount);

            // Offset Commit (메시지 재처리 방지)
            if (ack != null) {
                ack.acknowledge();
            }
            log.error("[Kafka Consumer] 최종 실패, DLQ 전송 - couponId: {}, userId: {}",
                event.getCouponId(), event.getUserId());
        }
    }

    private void sendToDeadLetterQueue(CouponIssueRequestEvent event, Exception exception, int retryCount) {
        try {
            CouponIssueDlqEvent dlqEvent = CouponIssueDlqEvent.builder()
                .originalEvent(event)
                .failureReason(exception != null ? exception.getMessage() : "Unknown error")
                .retryCount(retryCount)
                .lastAttemptAt(LocalDateTime.now())
                .stackTrace(exception != null ? getStackTrace(exception) : "")
                .build();

            dlqKafkaTemplate.send(DLQ_TOPIC, event.getCouponId().toString(), dlqEvent);

            // Redis 상태 업데이트
            redisService.setUserStatus(event.getCouponId(), event.getUserId(), "FAILED");

        } catch (Exception e) {
            log.error("[Kafka Consumer] DLQ 전송 실패 - couponId: {}, userId: {}",
                event.getCouponId(), event.getUserId(), e);
        }
    }

    private String getStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
