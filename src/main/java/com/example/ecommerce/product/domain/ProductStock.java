package com.example.ecommerce.product.domain;

import com.example.ecommerce.common.domain.BaseEntity;
import com.example.ecommerce.product.domain.status.StockStatus;
import com.example.ecommerce.product.domain.vo.Stock;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "product_stocks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class ProductStock extends BaseEntity {

    @Column(name = "product_id", nullable = false, unique = true)
    private Long productId;

    @Embedded
    @AttributeOverride(name = "quantity", column = @Column(name = "current_stock", nullable = false))
    private Stock currentStock;

    @Column(name = "reserved_stock", nullable = false)
    private int reservedStock;


    public static ProductStock create(Long productId, int stock) {
        return ProductStock.builder()
            .productId(productId)
            .currentStock(Stock.of(stock))
            .reservedStock(0)
            .build();
    }

    public void decreaseStock(int quantity) {
        this.currentStock = this.currentStock.decrease(quantity);
    }

    public void increaseStock(int quantity) {
        this.currentStock = this.currentStock.increase(quantity);
    }

    public void decreaseReservedStock(int quantity) {
        this.reservedStock -= quantity;
    }

    public void increaseReservedStock(int quantity) {
        this.reservedStock += quantity;
    }

    public boolean hasEnoughStockToReservation(int quantity) {
        return this.currentStock.getQuantity() - this.reservedStock >= quantity;
    }

    public StockStatus getStockStatus(int threshold) {
        return this.currentStock.getStatus(threshold);
    }
}
