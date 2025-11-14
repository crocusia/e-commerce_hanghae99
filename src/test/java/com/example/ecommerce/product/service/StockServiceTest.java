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
        ProductStock productStock = ProductStock.builder()
            .id(productId)
            .currentStock(Stock.of(stock))
            .build();
        if (reserved > 0) {
            productStock.increaseReservedStock(reserved);
        }
        return productStock;
    }

    private StockReservation createReservation(Long id, Long orderId, Long productId, int quantity, ReservationStatus status) {
        StockReservation reservation = StockReservation.builder()
            .id(id)
            .orderId(orderId)
            .productId(productId)
            .quantity(quantity)
            .expiresAt(LocalDateTime.now().plusMinutes(10))
            .build();
        if (status != ReservationStatus.RESERVED) {
            reservation.updateStatus(status);
        }
        return reservation;
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
    class ConfirmTest {

        @Test
        @DisplayName("예약을 확정하고 재고를 차감한다")
        void confirm() {
            // given
            Long orderId = 1L;
            Long productId = 100L;
            int quantity = 10;

            StockReservation reservation = createReservation(1L, orderId, productId, quantity, ReservationStatus.RESERVED);
            ProductStock stock = createProductStock(productId, 100, quantity);

            given(reservationRepository.findPendingByOrderId(orderId)).willReturn(Collections.singletonList(reservation));
            given(stockRepository.findByIdOrElseThrow(productId)).willReturn(stock);

            int initialStock = stock.getCurrentStock().getQuantity();
            int initialReserved = stock.getReservedStock();

            // when
            stockService.confirm(orderId);

            // then
            assertAll(
                () -> assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED),
                () -> assertThat(stock.getCurrentStock().getQuantity()).isEqualTo(initialStock - quantity),
                () -> assertThat(stock.getReservedStock()).isEqualTo(initialReserved - quantity),
                () -> then(reservationRepository).should().findPendingByOrderId(orderId),
                () -> then(stockRepository).should().save(stock),
                () -> then(reservationRepository).should().save(reservation)
            );
        }

        @Test
        @DisplayName("확정할 예약이 없으면 아무 작업도 수행하지 않는다")
        void confirmWithNoReservations() {
            // given
            Long orderId = 1L;
            given(reservationRepository.findPendingByOrderId(orderId)).willReturn(Collections.emptyList());

            // when
            stockService.confirm(orderId);

            // then
            then(stockRepository).should(times(0)).save(any(ProductStock.class));
        }
    }

    @Nested
    @DisplayName("재고 예약 해제 테스트")
    class ReleaseTest {

        @Test
        @DisplayName("예약을 해제하고 예약 재고를 감소시킨다")
        void release() {
            // given
            Long orderId = 1L;
            Long productId = 100L;
            int quantity = 10;

            StockReservation reservation = createReservation(1L, orderId, productId, quantity, ReservationStatus.RESERVED);
            ProductStock stock = createProductStock(productId, 100, quantity);

            given(reservationRepository.findPendingByOrderId(orderId)).willReturn(Collections.singletonList(reservation));
            given(stockRepository.findByIdOrElseThrow(productId)).willReturn(stock);

            int initialReserved = stock.getReservedStock();

            // when
            stockService.release(orderId);

            // then
            assertAll(
                () -> assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.RELEASED),
                () -> assertThat(stock.getReservedStock()).isEqualTo(initialReserved - quantity),
                () -> then(reservationRepository).should().findPendingByOrderId(orderId),
                () -> then(stockRepository).should().save(stock),
                () -> then(reservationRepository).should().save(reservation)
            );
        }

        @Test
        @DisplayName("해제할 예약이 없으면 아무 작업도 수행하지 않는다")
        void releaseWithNoReservations() {
            // given
            Long orderId = 1L;
            given(reservationRepository.findPendingByOrderId(orderId)).willReturn(Collections.emptyList());

            // when
            stockService.release(orderId);

            // then
            then(stockRepository).should(times(0)).save(any(ProductStock.class));
        }
    }
}
