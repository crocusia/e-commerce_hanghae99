package com.example.ecommerce.product.service;

import com.example.ecommerce.common.exception.CustomException;
import com.example.ecommerce.common.exception.ErrorCode;
import com.example.ecommerce.product.domain.ProductStock;
import com.example.ecommerce.product.domain.StockReservation;
import com.example.ecommerce.product.domain.status.ReservationStatus;
import com.example.ecommerce.product.repository.ProductStockRepository;
import com.example.ecommerce.product.repository.StockReservationRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {
    private static final int RESERVATION_TIME_THRESHOLD = 10;

    private final ProductStockRepository stockRepository;
    private final StockReservationRepository reservationRepository;

    public StockReservation reserve(Long orderId, Long productId, int quantity) {
        ProductStock stock = stockRepository
            .findByIdOrElseThrow(productId);

        if(!stock.hasEnoughStockToReservation(quantity)){
            throw new CustomException(ErrorCode.PRODUCT_OUT_OF_STOCK);
        }

        StockReservation reservation = StockReservation.builder()
            .orderId(orderId)
            .productId(productId)
            .quantity(quantity)
            .ttl(Duration.ofMinutes(RESERVATION_TIME_THRESHOLD))
            .build();

        stock.increaseReservedStock(quantity);

        return reservationRepository.save(reservation);
    }

    public void confirm(Long orderId) {
        List<StockReservation> reservations =
            reservationRepository.findPendingByOrderId(orderId);

        reservations.forEach(reservation -> {
            ProductStock stock = stockRepository
                .findByIdOrElseThrow(reservation.getProductId());

            stock.decreaseStock(reservation.getQuantity());
            stock.decreaseReservedStock(reservation.getQuantity());
            reservation.updateStatus(ReservationStatus.CONFIRMED);

            stockRepository.save(stock);
            reservationRepository.save(reservation);
        });
    }

    public void release(Long orderId) {
        List<StockReservation> reservations =
            reservationRepository.findPendingByOrderId(orderId);

        reservations.forEach(reservation -> {
            ProductStock stock = stockRepository
                .findByIdOrElseThrow(reservation.getProductId());

            reservation.updateStatus(ReservationStatus.RELEASED);
            stock.decreaseReservedStock(reservation.getQuantity());

            stockRepository.save(stock);
            reservationRepository.save(reservation);
        });
    }

    public void expireReservations() {
        List<StockReservation> expired =
            reservationRepository.findExpiredReservations(LocalDateTime.now());

        expired.forEach(reservation -> {
            ProductStock stock = stockRepository
                .findByIdOrElseThrow(reservation.getProductId());

            reservation.updateStatus(ReservationStatus.RELEASED);
            stock.decreaseReservedStock(reservation.getQuantity());

            stockRepository.save(stock);
            reservationRepository.save(reservation);
        });
    }

}
