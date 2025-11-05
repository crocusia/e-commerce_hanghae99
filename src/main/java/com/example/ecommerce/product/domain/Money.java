package com.example.ecommerce.product.domain;

import com.example.ecommerce.common.exception.CustomException;
import com.example.ecommerce.common.exception.ErrorCode;
import java.util.Objects;

public class Money {
    private final long amount;

    private Money(long amount) {
        validateAmount(amount);
        this.amount = amount;
    }

    public static Money of(long amount) {
        return new Money(amount);
    }

    public static Money zero() {
        return new Money(0L);
    }

    public Money add(Money other) {
        return new Money(this.amount + other.amount);
    }

    public Money subtract(Money other) {
        return new Money(this.amount - other.amount);
    }

    public Money multiply(int quantity) {
        return new Money(this.amount * quantity);
    }

    public Money discountRate(double rate) {
        long discountedAmount = (long) (this.amount * (1 - rate));
        return new Money(discountedAmount);
    }

    public boolean isGreaterThan(Money other) {
        return this.amount > other.amount;
    }

    public boolean isGreaterThanOrEqual(Money other) {
        return this.amount >= other.amount;
    }

    public boolean isLessThan(Money other) {
        return this.amount < other.amount;
    }

    public long getAmount() {
        return amount;
    }

    private void validateAmount(long amount) {
        if (amount < 0) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Money money = (Money) o;
        return amount == money.amount;
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount);
    }

    @Override
    public String toString() {
        return String.valueOf(amount);
    }
}
