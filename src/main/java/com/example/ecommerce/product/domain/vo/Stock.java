package com.example.ecommerce.product.domain.vo;

import com.example.ecommerce.common.exception.CustomException;
import com.example.ecommerce.common.exception.ErrorCode;
import com.example.ecommerce.product.domain.status.StockStatus;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stock {
    private int quantity;

    private Stock(int quantity) {
        validateQuantity(quantity);
        this.quantity = quantity;
    }

    public static Stock of(int quantity) {
        return new Stock(quantity);
    }

    public static Stock empty() {
        return new Stock(0);
    }

    //재고 차감
    public Stock decrease(int amount) {
        validateQuantity(amount);

        if (this.quantity < amount) {
            throw new CustomException(ErrorCode.PRODUCT_OUT_OF_STOCK);
        }

        return new Stock(this.quantity - amount);
    }

    //재고 추가 또는 복원
    public Stock increase(int amount) {
        validateQuantity(amount);
        return new Stock(this.quantity + amount);
    }

    //재고 충분 여부 확인
    public boolean hasEnough(int required) {
        return this.quantity >= required;
    }

    //재고 상태 반환
    public StockStatus getStatus(int threshold) {
        if (quantity <= 0) {
            return StockStatus.OUT_OF_STOCK;
        } else if (quantity <= threshold) {
            return StockStatus.LOW_STOCK;
        }
        return StockStatus.AVAILABLE;
    }

    //재고 검증
    private void validateQuantity(int quantity) {
        if (quantity < 0) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Stock stock = (Stock) o;
        return quantity == stock.quantity;
    }

    @Override
    public String toString() {
        return String.valueOf(quantity);
    }
}