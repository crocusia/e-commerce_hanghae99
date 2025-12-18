package com.example.ecommerce.common.event;

import com.example.ecommerce.common.exception.EventPublishException;
import com.example.ecommerce.order.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventPublisher {

    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;

    public void publishOrderCreatedEvent(String topic, OrderCreatedEvent event) {
        String key = event.getAggregateId().toString();

        log.info("[Kafka Producer] 주문 이벤트 발행 시작 - topic: {}, orderId: {}", topic, event.aggregateId());

        try {
            CompletableFuture<SendResult<String, OrderCreatedEvent>> future =
                kafkaTemplate.send(topic, key, event);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("[Kafka Producer] 주문 이벤트 발행 성공 - topic: {}, partition: {}, offset: {}, orderId: {}",
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset(),
                        event.aggregateId());
                } else {
                    log.error("[Kafka Producer] 주문 이벤트 발행 실패 - topic: {}, orderId: {}, error: {}",
                        topic, event.aggregateId(), ex.getMessage(), ex);
                    throw new EventPublishException("Kafka 이벤트 발행 실패", ex);
                }
            });
        } catch (Exception e) {
            log.error("[Kafka Producer] 주문 이벤트 발행 중 예외 발생 - topic: {}, orderId: {}, error: {}",
                topic, event.aggregateId(), e.getMessage(), e);
            throw new EventPublishException("Kafka 이벤트 발행 중 예외 발생", e);
        }
    }

    public void publishOrderCreatedEventSync(String topic, OrderCreatedEvent event) {
        String key = event.getAggregateId().toString();

        log.info("[Kafka Producer] 주문 이벤트 발행 시작 (동기) - topic: {}, orderId: {}", topic, event.aggregateId());

        try {
            SendResult<String, OrderCreatedEvent> result = kafkaTemplate.send(topic, key, event).get();

            log.info("[Kafka Producer] 주문 이벤트 발행 성공 (동기) - topic: {}, partition: {}, offset: {}, orderId: {}",
                result.getRecordMetadata().topic(),
                result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset(),
                event.aggregateId());
        } catch (Exception e) {
            log.error("[Kafka Producer] 주문 이벤트 발행 실패 (동기) - topic: {}, orderId: {}, error: {}",
                topic, event.aggregateId(), e.getMessage(), e);
            throw new EventPublishException("Kafka 이벤트 발행 실패", e);
        }
    }
}