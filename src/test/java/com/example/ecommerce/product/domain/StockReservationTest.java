package com.example.ecommerce.product.domain;

import com.example.ecommerce.product.domain.status.ReservationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@DisplayName("StockReservation 도메인 테스트")
class StockReservationTest {

    // 헬퍼 메서드
    private StockReservation create(Long orderId, Long productId, int quantity) {
        return StockReservation.builder()
            .orderId(orderId)
            .productId(productId)
            .quantity(quantity)
            .expiresAt(LocalDateTime.now().plusMinutes(10))
            .build();
    }

    @Nested
    @DisplayName("생성 테스트")
    class CreateTest {

        @Test
        @DisplayName("정상적으로 재고 예약을 생성한다")
        void createReservation() {
            // given
            Long orderId = 1L;
            Long productId = 100L;
            int quantity = 5;

            // when
            StockReservation reservation = create(orderId, productId, quantity);

            // then
            assertAll(
                () -> assertThat(reservation.getOrderId()).isEqualTo(orderId),
                () -> assertThat(reservation.getProductId()).isEqualTo(productId),
                () -> assertThat(reservation.getQuantity()).isEqualTo(quantity),
                () -> assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.RESERVED),
                () -> assertThat(reservation.getReservedAt()).isNotNull(),
                () -> assertThat(reservation.getExpiresAt()).isNotNull(),
                () -> assertThat(reservation.getConfirmedAt()).isNull()
            );
        }

        @Test
        @DisplayName("예약 생성 시 만료 시간이 설정된다")
        void reservationExpiresAtIsSetCorrectly() {
            // given
            LocalDateTime expectedExpiration = LocalDateTime.now().plusMinutes(15);

            // when
            StockReservation reservation = StockReservation.builder()
                .orderId(1L)
                .productId(100L)
                .quantity(5)
                .expiresAt(expectedExpiration)
                .build();

            // then
            assertThat(reservation.getExpiresAt()).isEqualTo(expectedExpiration);
        }

        @Test
        @DisplayName("예약 생성 시 초기 상태는 RESERVED이다")
        void initialStatusIsReserved() {
            // given & when
            StockReservation reservation = create(1L, 100L, 5);

            // then
            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.RESERVED);
        }
    }

    @Nested
    @DisplayName("상태 변경 테스트")
    class UpdateStatusTest {

        @ParameterizedTest
        @EnumSource(ReservationStatus.class)
        @DisplayName("예약 상태를 변경할 수 있다")
        void updateStatus(ReservationStatus newStatus) {
            // given
            StockReservation reservation = create(1L, 100L, 5);

            // when
            reservation.updateStatus(newStatus);

            // then
            assertThat(reservation.getStatus()).isEqualTo(newStatus);
        }

        @Test
        @DisplayName("RESERVED에서 CONFIRMED로 상태를 변경한다")
        void updateStatusToConfirmed() {
            // given
            StockReservation reservation = create(1L, 100L, 5);

            // when
            reservation.updateStatus(ReservationStatus.CONFIRMED);

            // then
            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        }

        @Test
        @DisplayName("RESERVED에서 RELEASED로 상태를 변경한다")
        void updateStatusToReleased() {
            // given
            StockReservation reservation = create(1L, 100L, 5);

            // when
            reservation.updateStatus(ReservationStatus.RELEASED);

            // then
            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.RELEASED);
        }
    }

    @Nested
    @DisplayName("예약 정보 조회 테스트")
    class GetReservationInfoTest {

        @Test
        @DisplayName("주문 ID를 조회할 수 있다")
        void getOrderId() {
            // given
            Long expectedOrderId = 123L;
            StockReservation reservation = create(expectedOrderId, 100L, 5);

            // when
            Long orderId = reservation.getOrderId();

            // then
            assertThat(orderId).isEqualTo(expectedOrderId);
        }

        @Test
        @DisplayName("상품 ID를 조회할 수 있다")
        void getProductId() {
            // given
            Long expectedProductId = 456L;
            StockReservation reservation = create(1L, expectedProductId, 5);

            // when
            Long productId = reservation.getProductId();

            // then
            assertThat(productId).isEqualTo(expectedProductId);
        }

        @Test
        @DisplayName("예약 수량을 조회할 수 있다")
        void getQuantity() {
            // given
            int expectedQuantity = 10;
            StockReservation reservation = create(1L, 100L, expectedQuantity);

            // when
            int quantity = reservation.getQuantity();

            // then
            assertThat(quantity).isEqualTo(expectedQuantity);
        }
    }
}
