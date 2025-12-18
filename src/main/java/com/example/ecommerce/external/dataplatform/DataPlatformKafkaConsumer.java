package com.example.ecommerce.external.dataplatform;

import com.example.ecommerce.order.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataPlatformKafkaConsumer {

    private final DataPlatformClient dataPlatformClient;

    @KafkaListener(
        topics = "order-created-events",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeOrderCreatedEvent(
        @Payload OrderCreatedEvent event,
        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
        @Header(KafkaHeaders.OFFSET) long offset,
        Acknowledgment ack
    ) {
        log.info("[Kafka Consumer] 주문 이벤트 수신 - topic: order-created-events, partition: {}, offset: {}, orderId: {}",
            partition, offset, event.aggregateId());

        try {
            // 외부 데이터 플랫폼으로 전송
            dataPlatformClient.sendOrderData(event);

            // 수동 ACK: 메시지 처리 완료 확인
            if (ack != null) {
                ack.acknowledge();
                log.info("[Kafka Consumer] 메시지 처리 완료 (ACK) - orderId: {}, offset: {}",
                    event.aggregateId(), offset);
            }

        } catch (Exception e) {
            log.error("[Kafka Consumer] 외부 플랫폼 전송 실패 - orderId: {}, partition: {}, offset: {}, error: {}",
                event.aggregateId(), partition, offset, e.getMessage(), e);

            // 재시도 로직 (예: 3번까지 재시도)
            // 실패 시 Dead Letter Queue로 전송하거나 에러 로깅
            // 현재는 ACK를 하지 않아 Kafka가 자동으로 재시도함

            throw new DataPlatformException("외부 플랫폼 전송 실패 - 재처리 필요", e);
        }
    }
}