package com.example.ecommerce.product.service;

import com.example.ecommerce.common.aop.DistributedLock;
import com.example.ecommerce.common.exception.CustomException;
import com.example.ecommerce.common.exception.ErrorCode;
import com.example.ecommerce.product.domain.ProductStock;
import com.example.ecommerce.product.domain.StockReservation;
import com.example.ecommerce.product.domain.status.ReservationStatus;
import com.example.ecommerce.product.repository.ProductStockRepository;
import com.example.ecommerce.product.repository.StockReservationRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {
    private static final int RESERVATION_TIME_THRESHOLD = 10;

    private final ProductStockRepository stockRepository;
    private final StockReservationRepository reservationRepository;

    @DistributedLock(key = "'stock:lock:' + #productId", waitTime = 30, leaseTime = 10)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
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

    @DistributedLock(key = "'stock:lock:' + #productId", waitTime = 30, leaseTime = 10)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void confirmReservation(Long productId, Long reservationId) {
        StockReservation reservation = reservationRepository.findByIdOrElseThrow(reservationId);
        ProductStock stock = stockRepository.findByIdOrElseThrow(productId);

        stock.decreaseStock(reservation.getQuantity());
        stock.decreaseReservedStock(reservation.getQuantity());
        reservation.updateStatus(ReservationStatus.CONFIRMED);

        stockRepository.save(stock);
        reservationRepository.save(reservation);
    }

    @DistributedLock(key = "'stock:lock:' + #productId", waitTime = 30, leaseTime = 10)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void releaseReservation(Long productId, Long reservationId) {
        StockReservation reservation = reservationRepository.findByIdOrElseThrow(reservationId);
        ProductStock stock = stockRepository.findByIdOrElseThrow(productId);

        reservation.updateStatus(ReservationStatus.RELEASED);
        stock.decreaseReservedStock(reservation.getQuantity());

        stockRepository.save(stock);
        reservationRepository.save(reservation);
    }

    @Transactional
    public void expireReservations() {
        List<StockReservation> expired =
            reservationRepository.findExpiredReservations(LocalDateTime.now());

        expired.forEach(reservation -> {
            expireReservation(reservation.getProductId(), reservation.getId());
        });
    }

    @DistributedLock(key = "'stock:lock:' + #productId", waitTime = 30, leaseTime = 10)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void expireReservation(Long productId, Long reservationId) {
        StockReservation reservation = reservationRepository.findByIdOrElseThrow(reservationId);
        ProductStock stock = stockRepository.findByIdOrElseThrow(productId);

        reservation.updateStatus(ReservationStatus.RELEASED);
        stock.decreaseReservedStock(reservation.getQuantity());

        stockRepository.save(stock);
        reservationRepository.save(reservation);
    }
}
