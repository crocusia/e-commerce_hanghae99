package com.example.ecommerce.order.event;

import com.example.ecommerce.external.dataplatform.DataPlatformClient;
import com.example.ecommerce.order.domain.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("데이터 플랫폼 이벤트 리스너 테스트")
class DataPlatformIntegrationTest {

    @Mock
    private DataPlatformClient dataPlatformClient;

    @Mock
    private com.example.ecommerce.order.repository.OrderRepository orderRepository;

    @InjectMocks
    private OrderEventListener orderEventListener;

    private Order testOrder;

    @BeforeEach
    void setUp() {
        testOrder = Order.builder()
            .id(1L)
            .userId(1L)
            .totalAmount(20000L)
            .discountAmount(0L)
            .finalAmount(20000L)
            .orderItems(new ArrayList<>())
            .build();
    }

    @Test
    @DisplayName("주문 생성 이벤트가 발생하면 데이터 플랫폼 클라이언트가 호출된다")
    void whenOrderCreatedEvent_thenDataPlatformClientCalled() {
        // given
        OrderCreatedEvent event = OrderCreatedEvent.from(testOrder);

        // when
        orderEventListener.handleOrderCreatedForDataPlatform(event);

        // then
        verify(dataPlatformClient, times(1)).sendOrderData(any(OrderCreatedEvent.class));
    }

    @Test
    @DisplayName("데이터 플랫폼 전송 실패 시 예외가 로깅되고 삼켜진다")
    void whenDataPlatformFails_thenExceptionIsLogged() {
        // given
        OrderCreatedEvent event = OrderCreatedEvent.from(testOrder);
        doThrow(new RuntimeException("데이터 플랫폼 오류"))
            .when(dataPlatformClient).sendOrderData(any(OrderCreatedEvent.class));

        // when & then
        // 예외가 발생해도 메서드가 정상 종료되어야 함 (부가 로직이므로)
        orderEventListener.handleOrderCreatedForDataPlatform(event);

        verify(dataPlatformClient, times(1)).sendOrderData(any(OrderCreatedEvent.class));
    }
}
