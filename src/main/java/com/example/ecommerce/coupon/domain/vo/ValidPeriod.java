package com.example.ecommerce.coupon.domain.vo;

import com.example.ecommerce.common.exception.CustomException;
import com.example.ecommerce.common.exception.ErrorCode;
import jakarta.persistence.Embeddable;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ValidPeriod {

    private LocalDateTime validFrom;    // 유효 시작일
    private LocalDateTime validUntil;   // 유효 종료일

    private ValidPeriod(LocalDateTime validFrom, LocalDateTime validUntil) {
        validatePeriod(validFrom, validUntil);
        this.validFrom = validFrom;
        this.validUntil = validUntil;
    }

    public static ValidPeriod of(LocalDateTime validFrom, LocalDateTime validUntil) {
        return new ValidPeriod(validFrom, validUntil);
    }

    public boolean isValid() {
        return isValidAt(LocalDateTime.now());
    }

    public boolean isValidAt(LocalDateTime date) {
        return !date.isBefore(validFrom) && !date.isAfter(validUntil);
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(validUntil);
    }

    public boolean isNotStarted() {
        return LocalDateTime.now().isBefore(validFrom);
    }

    public long getDays() {
        return ChronoUnit.DAYS.between(validFrom, validUntil);
    }

    private static void validatePeriod(LocalDateTime validFrom, LocalDateTime validUntil) {
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
