package com.example.ecommerce.product.domain;

import com.example.ecommerce.product.domain.status.ReservationStatus;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
public class StockReservation {
    private final Long id;

    private final Long orderId;
    private final Long productId;
    private final int quantity;

    private ReservationStatus status;

    private final LocalDateTime reservedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime confirmedAt;


    @Builder
    private StockReservation(Long id, Long orderId, Long productId, int quantity, Duration ttl) {
        this.id = id;
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.status = ReservationStatus.RESERVED;
        this.reservedAt = LocalDateTime.now();
        this.expiresAt = LocalDateTime.now().plus(ttl);
        this.confirmedAt = null;
    }

    public void updateStatus(ReservationStatus status){
        this.status = status;
    }
}
