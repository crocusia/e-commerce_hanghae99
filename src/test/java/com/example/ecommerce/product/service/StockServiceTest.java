package com.example.ecommerce.product.service;

import com.example.ecommerce.common.exception.CustomException;
import com.example.ecommerce.product.domain.ProductStock;
import com.example.ecommerce.product.domain.StockReservation;
import com.example.ecommerce.product.domain.status.ReservationStatus;
import com.example.ecommerce.product.domain.vo.Stock;
import com.example.ecommerce.product.repository.ProductStockRepository;
import com.example.ecommerce.product.repository.StockReservationRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("StockService 테스트")
class StockServiceTest {

    @Mock
    private ProductStockRepository stockRepository;

    @Mock
    private StockReservationRepository reservationRepository;

    @InjectMocks
    private StockService stockService;

    private ProductStock createProductStock(Long productId, int stock, int reserved) {
        LocalDateTime now = LocalDateTime.now();
        ProductStock productStock = ProductStock.builder()
            .id(productId)
            .productId(productId)
            .currentStock(Stock.of(stock))
            .reservedStock(0)
            .createdAt(now)
            .updatedAt(now)
            .build();
        if (reserved > 0) {
            productStock.increaseReservedStock(reserved);
        }
        return productStock;
    }

    private StockReservation createReservation(Long id, Long orderId, Long productId, int quantity, ReservationStatus status) {
        LocalDateTime now = LocalDateTime.now();
        return StockReservation.builder()
            .id(id)
            .orderId(orderId)
            .productId(productId)
            .quantity(quantity)
            .status(status)
            .expiresAt(now.plusMinutes(10))
            .createdAt(now)
            .updatedAt(now)
            .build();
    }

    @Nested
    @DisplayName("재고 예약 테스트")
    class ReserveTest {

        @Test
        @DisplayName("재고를 정상적으로 예약한다")
        void reserve() {
            // given
            Long orderId = 1L;
            Long productId = 100L;
            int quantity = 5;
            ProductStock stock = createProductStock(productId, 100, 0);
            StockReservation savedReservation = createReservation(1L, orderId, productId, quantity, ReservationStatus.RESERVED);

            given(stockRepository.findByIdOrElseThrow(productId)).willReturn(stock);
            given(reservationRepository.save(any(StockReservation.class))).willReturn(savedReservation);

            // when
            StockReservation result = stockService.reserve(orderId, productId, quantity);

            // then
            assertAll(
                () -> assertThat(result.getOrderId()).isEqualTo(orderId),
                () -> assertThat(result.getProductId()).isEqualTo(productId),
                () -> assertThat(result.getQuantity()).isEqualTo(quantity),
                () -> assertThat(result.getStatus()).isEqualTo(ReservationStatus.RESERVED),
                () -> then(stockRepository).should().findByIdOrElseThrow(productId),
                () -> then(reservationRepository).should().save(any(StockReservation.class))
            );
        }

        @Test
        @DisplayName("예약 시 예약 재고가 증가한다")
        void reserveIncreasesReservedStock() {
            // given
            Long orderId = 1L;
            Long productId = 100L;
            int quantity = 10;
            ProductStock stock = createProductStock(productId, 100, 0);
            StockReservation savedReservation = createReservation(1L, orderId, productId, quantity, ReservationStatus.RESERVED);

            given(stockRepository.findByIdOrElseThrow(productId)).willReturn(stock);
            given(reservationRepository.save(any(StockReservation.class))).willReturn(savedReservation);

            int initialReserved = stock.getReservedStock();

            // when
            stockService.reserve(orderId, productId, quantity);

            // then
            assertThat(stock.getReservedStock()).isEqualTo(initialReserved + quantity);
        }

        @Test
        @DisplayName("재고가 부족하면 예약 실패한다")
        void reserveFailsWhenStockInsufficient() {
            // given
            Long orderId = 1L;
            Long productId = 100L;
            int quantity = 150;
            ProductStock stock = createProductStock(productId, 100, 0);

            given(stockRepository.findByIdOrElseThrow(productId)).willReturn(stock);

            // when & then
            assertThatThrownBy(() -> stockService.reserve(orderId, productId, quantity))
                .isInstanceOf(CustomException.class);
        }

        @Test
        @DisplayName("이미 예약된 재고가 있으면 남은 재고로만 예약 가능하다")
        void reserveWithExistingReservations() {
            // given
            Long orderId = 1L;
            Long productId = 100L;
            int quantity = 60;
            ProductStock stock = createProductStock(productId, 100, 50);

            given(stockRepository.findByIdOrElseThrow(productId)).willReturn(stock);

            // when & then
            assertThatThrownBy(() -> stockService.reserve(orderId, productId, quantity))
                .isInstanceOf(CustomException.class);
        }
    }

    @Nested
    @DisplayName("재고 확정 테스트")
    class ConfirmReservationTest {

        @Test
        @DisplayName("예약을 확정하고 재고를 차감한다")
        void confirmReservation() {
            // given
            Long reservationId = 1L;
            Long orderId = 1L;
            Long productId = 100L;
            int quantity = 10;

            StockReservation reservation = createReservation(reservationId, orderId, productId, quantity, ReservationStatus.RESERVED);
            ProductStock stock = createProductStock(productId, 100, quantity);

            given(reservationRepository.findByIdOrElseThrow(reservationId)).willReturn(reservation);
            given(stockRepository.findByIdOrElseThrow(productId)).willReturn(stock);

            int initialStock = stock.getCurrentStock().getQuantity();
            int initialReserved = stock.getReservedStock();

            // when
            stockService.confirmReservation(productId, reservationId);

            // then
            assertAll(
                () -> assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED),
                () -> assertThat(stock.getCurrentStock().getQuantity()).isEqualTo(initialStock - quantity),
                () -> assertThat(stock.getReservedStock()).isEqualTo(initialReserved - quantity),
                () -> then(reservationRepository).should().findByIdOrElseThrow(reservationId),
                () -> then(stockRepository).should().findByIdOrElseThrow(productId),
                () -> then(stockRepository).should().save(stock),
                () -> then(reservationRepository).should().save(reservation)
            );
        }

        @Test
        @DisplayName("확정 후 재고와 예약재고가 모두 감소한다")
        void confirmDecreasesStockAndReservedStock() {
            // given
            Long reservationId = 1L;
            Long orderId = 1L;
            Long productId = 100L;
            int quantity = 10;

            StockReservation reservation = createReservation(reservationId, orderId, productId, quantity, ReservationStatus.RESERVED);
            ProductStock stock = createProductStock(productId, 100, quantity);

            given(reservationRepository.findByIdOrElseThrow(reservationId)).willReturn(reservation);
            given(stockRepository.findByIdOrElseThrow(productId)).willReturn(stock);

            int initialStock = stock.getCurrentStock().getQuantity();
            int initialReserved = stock.getReservedStock();

            // when
            stockService.confirmReservation(productId, reservationId);

            // then
            assertAll(
                () -> assertThat(stock.getCurrentStock().getQuantity()).isEqualTo(initialStock - quantity),
                () -> assertThat(stock.getReservedStock()).isEqualTo(initialReserved - quantity)
            );
        }
    }

    @Nested
    @DisplayName("재고 예약 해제 테스트")
    class ReleaseReservationTest {

        @Test
        @DisplayName("예약을 해제하고 예약 재고를 감소시킨다")
        void releaseReservation() {
            // given
            Long reservationId = 1L;
            Long orderId = 1L;
            Long productId = 100L;
            int quantity = 10;

            StockReservation reservation = createReservation(reservationId, orderId, productId, quantity, ReservationStatus.RESERVED);
            ProductStock stock = createProductStock(productId, 100, quantity);

            given(reservationRepository.findByIdOrElseThrow(reservationId)).willReturn(reservation);
            given(stockRepository.findByIdOrElseThrow(productId)).willReturn(stock);

            int initialReserved = stock.getReservedStock();

            // when
            stockService.releaseReservation(productId, reservationId);

            // then
            assertAll(
                () -> assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.RELEASED),
                () -> assertThat(stock.getReservedStock()).isEqualTo(initialReserved - quantity),
                () -> then(reservationRepository).should().findByIdOrElseThrow(reservationId),
                () -> then(stockRepository).should().findByIdOrElseThrow(productId),
                () -> then(stockRepository).should().save(stock),
                () -> then(reservationRepository).should().save(reservation)
            );
        }

        @Test
        @DisplayName("해제 시 currentStock은 변경되지 않고 reservedStock만 감소한다")
        void releaseOnlyDecreasesReservedStock() {
            // given
            Long reservationId = 1L;
            Long orderId = 1L;
            Long productId = 100L;
            int quantity = 10;

            StockReservation reservation = createReservation(reservationId, orderId, productId, quantity, ReservationStatus.RESERVED);
            ProductStock stock = createProductStock(productId, 100, quantity);

            given(reservationRepository.findByIdOrElseThrow(reservationId)).willReturn(reservation);
            given(stockRepository.findByIdOrElseThrow(productId)).willReturn(stock);

            int initialStock = stock.getCurrentStock().getQuantity();
            int initialReserved = stock.getReservedStock();

            // when
            stockService.releaseReservation(productId, reservationId);

            // then
            assertAll(
                () -> assertThat(stock.getCurrentStock().getQuantity()).isEqualTo(initialStock),
                () -> assertThat(stock.getReservedStock()).isEqualTo(initialReserved - quantity),
                () -> assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.RELEASED)
            );
        }
    }

    @Nested
    @DisplayName("재고 예약 만료 테스트")
    class ExpireReservationTest {

        @Test
        @DisplayName("만료된 예약을 해제하고 예약 재고를 감소시킨다")
        void expireReservation() {
            // given
            Long reservationId = 1L;
            Long orderId = 1L;
            Long productId = 100L;
            int quantity = 10;

            StockReservation expiredReservation = createReservation(
                reservationId, orderId, productId, quantity, ReservationStatus.RESERVED
            );
            ProductStock stock = createProductStock(productId, 100, quantity);

            given(reservationRepository.findByIdOrElseThrow(reservationId)).willReturn(expiredReservation);
            given(stockRepository.findByIdOrElseThrow(productId)).willReturn(stock);

            int initialReserved = stock.getReservedStock();

            // when
            stockService.expireReservation(productId, reservationId);

            // then
            assertAll(
                () -> assertThat(expiredReservation.getStatus()).isEqualTo(ReservationStatus.RELEASED),
                () -> assertThat(stock.getReservedStock()).isEqualTo(initialReserved - quantity),
                () -> then(reservationRepository).should().findByIdOrElseThrow(reservationId),
                () -> then(stockRepository).should().findByIdOrElseThrow(productId),
                () -> then(stockRepository).should().save(stock),
                () -> then(reservationRepository).should().save(expiredReservation)
            );
        }

        @Test
        @DisplayName("만료 처리 시 currentStock은 변경되지 않는다")
        void expireDoesNotChangeCurrentStock() {
            // given
            Long reservationId = 1L;
            Long orderId = 1L;
            Long productId = 100L;
            int quantity = 10;

            StockReservation expiredReservation = createReservation(
                reservationId, orderId, productId, quantity, ReservationStatus.RESERVED
            );
            ProductStock stock = createProductStock(productId, 100, quantity);

            given(reservationRepository.findByIdOrElseThrow(reservationId)).willReturn(expiredReservation);
            given(stockRepository.findByIdOrElseThrow(productId)).willReturn(stock);

            int initialStock = stock.getCurrentStock().getQuantity();

            // when
            stockService.expireReservation(productId, reservationId);

            // then
            assertThat(stock.getCurrentStock().getQuantity()).isEqualTo(initialStock);
        }

        @Test
        @DisplayName("expireReservations는 만료된 예약들을 일괄 처리한다")
        void expireReservations() {
            // given
            Long productId = 100L;

            StockReservation reservation1 = createReservation(1L, 1L, productId, 5, ReservationStatus.RESERVED);
            StockReservation reservation2 = createReservation(2L, 2L, productId, 3, ReservationStatus.RESERVED);

            ProductStock stock = createProductStock(productId, 100, 8);

            given(reservationRepository.findExpiredReservations(any(LocalDateTime.class)))
                .willReturn(List.of(reservation1, reservation2));
            given(reservationRepository.findByIdOrElseThrow(1L)).willReturn(reservation1);
            given(reservationRepository.findByIdOrElseThrow(2L)).willReturn(reservation2);
            given(stockRepository.findByIdOrElseThrow(productId)).willReturn(stock);

            // when
            stockService.expireReservations();

            // then
            then(reservationRepository).should().findExpiredReservations(any(LocalDateTime.class));
            then(stockRepository).should(times(2)).findByIdOrElseThrow(productId);
            then(stockRepository).should(times(2)).save(stock);
        }
    }
}
