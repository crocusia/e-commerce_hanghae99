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
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {
    private static final int RESERVATION_TIME_THRESHOLD = 10;

    private final ProductStockRepository stockRepository;
    private final StockReservationRepository reservationRepository;

    @Transactional
    public StockReservation reserve(Long orderId, Long productId, int quantity) {
        ProductStock stock = stockRepository
            .findByIdOrElseThrow(productId);

        if(!stock.hasEnoughStockToReservation(quantity)){
            throw new CustomException(ErrorCode.PRODUCT_OUT_OF_STOCK);
        }

        StockReservation reservation = StockReservation.create(
            orderId,
            productId,
            quantity,
            LocalDateTime.now().plusMinutes(RESERVATION_TIME_THRESHOLD)
        );

        stock.increaseReservedStock(quantity);

        return reservationRepository.save(reservation);
    }

    @Transactional
    public void confirm(Long orderId) {
        List<StockReservation> reservations =
            reservationRepository.findPendingByOrderId(orderId);

        // 데드락 방지: Product ID 순으로 정렬하여 항상 같은 순서로 락 획득
        reservations.sort(Comparator.comparing(StockReservation::getProductId));

        reservations.forEach(reservation -> {
            ProductStock stock = stockRepository
                .findByProductIdWithLock(reservation.getProductId());  // 비관적 락 적용

            stock.decreaseStock(reservation.getQuantity());
            stock.decreaseReservedStock(reservation.getQuantity());
            reservation.updateStatus(ReservationStatus.CONFIRMED);

            stockRepository.save(stock);
            reservationRepository.save(reservation);
        });
    }

    @Transactional
    public void release(Long orderId) {
        List<StockReservation> reservations =
            reservationRepository.findPendingByOrderId(orderId);

        // 데드락 방지: Product ID 순으로 정렬하여 항상 같은 순서로 락 획득
        reservations.sort(Comparator.comparing(StockReservation::getProductId));

        reservations.forEach(reservation -> {
            ProductStock stock = stockRepository
                .findByProductIdWithLock(reservation.getProductId());  // 비관적 락 적용

            reservation.updateStatus(ReservationStatus.RELEASED);
            stock.decreaseReservedStock(reservation.getQuantity());

            stockRepository.save(stock);
            reservationRepository.save(reservation);
        });
    }

    @Transactional
    public void expireReservations() {
        List<StockReservation> expired =
            reservationRepository.findExpiredReservations(LocalDateTime.now());

        // 데드락 방지: Product ID 순으로 정렬하여 항상 같은 순서로 락 획득
        expired.sort(Comparator.comparing(StockReservation::getProductId));

        expired.forEach(reservation -> {
            ProductStock stock = stockRepository
                .findByProductIdWithLock(reservation.getProductId());  // 비관적 락 적용

            reservation.updateStatus(ReservationStatus.RELEASED);
            stock.decreaseReservedStock(reservation.getQuantity());

            stockRepository.save(stock);
            reservationRepository.save(reservation);
        });
    }
}
