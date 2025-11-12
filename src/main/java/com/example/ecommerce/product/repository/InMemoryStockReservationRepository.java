package com.example.ecommerce.product.repository;

import com.example.ecommerce.product.domain.StockReservation;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryStockReservationRepository implements StockReservationRepository{
    @Override
    public StockReservation save(StockReservation stockReservation){
        return StockReservation.builder().build();
    }

    @Override
    public List<StockReservation> findPendingByOrderId(Long orderId){
        return List.of();
    }

    @Override
    public List<StockReservation> findExpiredReservations(LocalDateTime time){
        return List.of();
    }
}
