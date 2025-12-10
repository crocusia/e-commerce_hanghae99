package com.example.ecommerce.product.repository;

import com.example.ecommerce.product.domain.StockReservation;
import java.time.LocalDateTime;
import java.util.List;

public interface StockReservationRepository {

    StockReservation save(StockReservation stockReservation);

    StockReservation findByIdOrElseThrow(Long id);

    List<StockReservation> findPendingByOrderId(Long orderId);

    List<StockReservation> findExpiredReservations(LocalDateTime time);

    void deleteAllInBatch();
}
