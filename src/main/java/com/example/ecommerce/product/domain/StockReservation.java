package com.example.ecommerce.product.domain;

import com.example.ecommerce.common.domain.BaseEntity;
import com.example.ecommerce.product.domain.status.ReservationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "stock_reservations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class StockReservation extends BaseEntity {

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReservationStatus status;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    public static StockReservation create(Long orderId, Long productId, int quantity, LocalDateTime expiresAt) {
        return StockReservation.builder()
            .orderId(orderId)
            .productId(productId)
            .quantity(quantity)
            .status(ReservationStatus.RESERVED)
            .expiresAt(expiresAt)
            .build();
    }

    public LocalDateTime getReservedAt() {
        return getCreatedAt();
    }

    public void updateStatus(ReservationStatus status){
        this.status = status;
    }
}
