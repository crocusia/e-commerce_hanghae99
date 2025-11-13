package com.example.ecommerce.product.domain;

import com.example.ecommerce.product.domain.status.StockStatus;
import com.example.ecommerce.product.domain.vo.Stock;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
public class ProductStock {

    private final Long productId;
    private Stock currentStock;
    private int reservedStock;
    private LocalDateTime updatedAt;

    @Builder
    private ProductStock(Long id, int stock) {
        this.productId = id;
        this.currentStock = Stock.of(stock);
        this.reservedStock = 0;
        this.updatedAt = LocalDateTime.now();
    }

    public void decreaseStock(int quantity) {
        this.currentStock = this.currentStock.decrease(quantity);
        this.updatedAt = LocalDateTime.now();
    }

    public void increaseStock(int quantity) {
        this.currentStock = this.currentStock.increase(quantity);
        this.updatedAt = LocalDateTime.now();
    }

    public void decreaseReservedStock(int quantity) {
        this.reservedStock -= quantity;
        this.updatedAt = LocalDateTime.now();
    }

    public void increaseReservedStock(int quantity) {
        this.reservedStock += quantity;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean hasEnoughStockToReservation(int quantity) {
        return this.currentStock.getQuantity() - this.reservedStock >= quantity;
    }

    public StockStatus getStockStatus(int threshold) {
        return this.currentStock.getStatus(threshold);
    }
}
