package com.example.ecommerce.product.repository;

import com.example.ecommerce.common.exception.CustomException;
import com.example.ecommerce.common.exception.ErrorCode;
import com.example.ecommerce.product.domain.StockReservation;
import com.example.ecommerce.product.domain.status.ReservationStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaStockReservationRepository extends JpaRepository<StockReservation, Long>, StockReservationRepository {

    @Override
    StockReservation save(StockReservation stockReservation);

    @Override
    default StockReservation findByIdOrElseThrow(Long id) {
        return findById(id)
            .orElseThrow(() -> new CustomException(ErrorCode.STOCK_RESERVATION_NOT_FOUND));
    }

    @Override
    @Query("SELECT sr FROM StockReservation sr WHERE sr.orderId = :orderId AND sr.status = 'RESERVED'")
    List<StockReservation> findPendingByOrderId(@Param("orderId") Long orderId);

    @Override
    @Query("SELECT sr FROM StockReservation sr WHERE sr.expiresAt < :time AND sr.status = 'RESERVED'")
    List<StockReservation> findExpiredReservations(@Param("time") LocalDateTime time);
}
