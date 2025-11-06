package com.example.ecommerce.coupon.domain;

import com.example.ecommerce.common.exception.CustomException;
import com.example.ecommerce.common.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ValidPeriod {

    private LocalDate validFrom;    // 유효 시작일
    private LocalDate validUntil;   // 유효 종료일

    private ValidPeriod(LocalDate validFrom, LocalDate validUntil) {
        validatePeriod(validFrom, validUntil);
        this.validFrom = validFrom;
        this.validUntil = validUntil;
    }

    public static ValidPeriod of(LocalDate validFrom, LocalDate validUntil) {
        return new ValidPeriod(validFrom, validUntil);
    }

    public boolean isValid() {
        return isValidAt(LocalDate.now());
    }

    public boolean isValidAt(LocalDate date) {
        return !date.isBefore(validFrom) && !date.isAfter(validUntil);
    }

    public boolean isExpired() {
        return LocalDate.now().isAfter(validUntil);
    }

    public boolean isNotStarted() {
        return LocalDate.now().isBefore(validFrom);
    }

    public long getDays() {
        return ChronoUnit.DAYS.between(validFrom, validUntil);
    }

    private static void validatePeriod(LocalDate validFrom, LocalDate validUntil) {
        if (validFrom == null || validUntil == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (validFrom.isAfter(validUntil)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ValidPeriod that = (ValidPeriod) o;
        return Objects.equals(validFrom, that.validFrom) && Objects.equals(validUntil, that.validUntil);
    }

    @Override
    public int hashCode() {
        return Objects.hash(validFrom, validUntil);
    }

    @Override
    public String toString() {
        return "ValidPeriod{" + validFrom + " ~ " + validUntil + "}";
    }
}
