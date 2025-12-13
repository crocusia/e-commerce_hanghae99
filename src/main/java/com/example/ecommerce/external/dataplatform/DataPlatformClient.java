package com.example.ecommerce.external.dataplatform;

import com.example.ecommerce.order.event.OrderCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 외부 데이터 플랫폼에 주문 정보를 전송하는 클라이언트
 * 실제 환경에서는 RestTemplate, WebClient, Kafka Producer 등을 사용
 */
@Slf4j
@Component
public class DataPlatformClient {

    /**
     * 데이터 플랫폼에 주문 생성 정보 전송 (Mock)
     *
     * @param event 주문 생성 이벤트
     */
    public void sendOrderData(OrderCreatedEvent event) {
        log.info("[데이터 플랫폼] 주문 정보 전송 시작 - orderId: {}, userId: {}, finalAmount: {}원",
            event.aggregateId(), event.userId(), event.finalAmount());

        try {
            // Mock API 호출 시뮬레이션
            // 실제 구현 시:
            // 1. HTTP 방식: restTemplate.postForEntity("http://data-platform-api/orders", event, Void.class)
            // 2. Kafka 방식: kafkaTemplate.send("order-events", event)
            // 3. Message Queue: rabbitTemplate.convertAndSend("order.exchange", "order.created", event)

            simulateApiCall(event);

            log.info("[데이터 플랫폼] 주문 정보 전송 완료 - orderId: {}, 상품 {}개",
                event.aggregateId(), event.orderItems().size());

        } catch (Exception e) {
            log.error("[데이터 플랫폼] 주문 정보 전송 실패 - orderId: {}, error: {}",
                event.aggregateId(), e.getMessage(), e);
            throw new DataPlatformException("데이터 플랫폼 전송 실패", e);
        }
    }

    /**
     * API 호출 시뮬레이션 (Mock)
     */
    private void simulateApiCall(OrderCreatedEvent event) {
        try {
            // 네트워크 지연 시뮬레이션 (50~150ms)
            long delay = 50 + (long) (Math.random() * 100);
            Thread.sleep(delay);

            log.debug("[데이터 플랫폼] Mock API 응답 - 소요시간: {}ms", delay);

            // 10% 확률로 재시도 가능한 에러 시뮬레이션 (실습용)
            if (Math.random() < 0.0) {  // 실제로는 비활성화
                throw new RuntimeException("Mock: 일시적 네트워크 오류");
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("API 호출 중단됨", e);
        }
    }
}
