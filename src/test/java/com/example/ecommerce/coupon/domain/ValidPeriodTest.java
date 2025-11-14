package com.example.ecommerce.coupon.domain;


import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.ecommerce.common.exception.CustomException;
import com.example.ecommerce.coupon.domain.vo.ValidPeriod;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ValidPeriod VO 테스트")
class ValidPeriodTest {

    @Test
    @DisplayName("ValidPeriod를 생성할 수 있다")
    void create() {
        // given
        LocalDateTime validFrom = LocalDateTime.now();
        LocalDateTime validUntil = LocalDateTime.now().plusDays(30);

        // when
        ValidPeriod period = ValidPeriod.of(validFrom, validUntil);

        // then
        assertThat(period).isNotNull();
        assertThat(period.getValidFrom()).isEqualTo(validFrom);
        assertThat(period.getValidUntil()).isEqualTo(validUntil);
    }

    @Test
    @DisplayName("시작일이 null이면 생성할 수 없다")
    void create_NullValidFrom() {
        // given
        LocalDateTime validUntil = LocalDateTime.now().plusDays(30);

        // when & then
        assertThrows(CustomException.class, () -> ValidPeriod.of(null, validUntil));
    }

    @Test
    @DisplayName("종료일이 null이면 생성할 수 없다")
    void create_NullValidUntil() {
        // given
        LocalDateTime validFrom = LocalDateTime.now();

        // when & then
        assertThrows(CustomException.class, () -> ValidPeriod.of(validFrom, null));
    }

    @Test
    @DisplayName("시작일이 종료일보다 늦으면 생성할 수 없다")
    void create_InvalidPeriod() {
        // given
        LocalDateTime validFrom = LocalDateTime.now().plusDays(30);
        LocalDateTime validUntil = LocalDateTime.now();

        // when & then
        assertThrows(CustomException.class, () -> ValidPeriod.of(validFrom, validUntil));
    }

    @Test
    @DisplayName("시작일과 종료일이 같으면 생성할 수 있다")
    void create_SameDay() {
        // given
        LocalDateTime today = LocalDateTime.now();

        // when
        ValidPeriod period = ValidPeriod.of(today, today);

        // then
        assertThat(period).isNotNull();
        assertThat(period.getValidFrom()).isEqualTo(today);
        assertThat(period.getValidUntil()).isEqualTo(today);
    }

    @Test
    @DisplayName("현재 시각이 유효 기간 내인지 확인할 수 있다")
    void isValid() {
        // given
        ValidPeriod validPeriod = ValidPeriod.of(
            LocalDateTime.now().minusDays(10),
            LocalDateTime.now().plusDays(10)
        );

        // when & then
        assertThat(validPeriod.isValid()).isTrue();
    }

    @Test
    @DisplayName("현재 시각이 시작일 이전이면 유효하지 않다")
    void isValid_BeforeStart() {
        // given
        ValidPeriod validPeriod = ValidPeriod.of(
            LocalDateTime.now().plusDays(1),
            LocalDateTime.now().plusDays(30)
        );

        // when & then
        assertThat(validPeriod.isValid()).isFalse();
    }

    @Test
    @DisplayName("현재 시각이 종료일 이후면 유효하지 않다")
    void isValid_AfterEnd() {
        // given
        ValidPeriod validPeriod = ValidPeriod.of(
            LocalDateTime.now().minusDays(30),
            LocalDateTime.now().minusDays(1)
        );

        // when & then
        assertThat(validPeriod.isValid()).isFalse();
    }

    @Test
    @DisplayName("특정 시각이 유효 기간 내인지 확인할 수 있다")
    void isValidAt() {
        // given
        ValidPeriod validPeriod = ValidPeriod.of(
            LocalDateTime.of(2025, 1, 1, 0, 0),
            LocalDateTime.of(2025, 12, 31, 23, 59)
        );

        // when & then
        assertThat(validPeriod.isValidAt(LocalDateTime.of(2025, 6, 15, 12, 0))).isTrue();
        assertThat(validPeriod.isValidAt(LocalDateTime.of(2024, 12, 31, 23, 59))).isFalse();
        assertThat(validPeriod.isValidAt(LocalDateTime.of(2026, 1, 1, 0, 0))).isFalse();
    }

    @Test
    @DisplayName("기간이 만료되었는지 확인할 수 있다")
    void isExpired() {
        // given
        ValidPeriod expiredPeriod = ValidPeriod.of(
            LocalDateTime.now().minusDays(30),
            LocalDateTime.now().minusDays(1)
        );
        ValidPeriod validPeriod = ValidPeriod.of(
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(30)
        );

        // when & then
        assertThat(expiredPeriod.isExpired()).isTrue();
        assertThat(validPeriod.isExpired()).isFalse();
    }

    @Test
    @DisplayName("기간이 아직 시작되지 않았는지 확인할 수 있다")
    void isNotStarted() {
        // given
        ValidPeriod notStartedPeriod = ValidPeriod.of(
            LocalDateTime.now().plusDays(1),
            LocalDateTime.now().plusDays(30)
        );
        ValidPeriod startedPeriod = ValidPeriod.of(
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now().plusDays(30)
        );

        // when & then
        assertThat(notStartedPeriod.isNotStarted()).isTrue();
        assertThat(startedPeriod.isNotStarted()).isFalse();
    }

    @Test
    @DisplayName("같은 값을 가진 ValidPeriod는 동등하다")
    void testEquals() {
        // given
        LocalDateTime validFrom = LocalDateTime.now();
        LocalDateTime validUntil = LocalDateTime.now().plusDays(30);
        ValidPeriod period1 = ValidPeriod.of(validFrom, validUntil);
        ValidPeriod period2 = ValidPeriod.of(validFrom, validUntil);
        ValidPeriod period3 = ValidPeriod.of(validFrom, validUntil.plusDays(10));

        // when & then
        assertThat(period1).isEqualTo(period2);
        assertThat(period1).isNotEqualTo(period3);
        assertThat(period1.hashCode()).isEqualTo(period2.hashCode());
    }

    @Test
    @DisplayName("기간의 일수를 계산할 수 있다")
    void getDays() {
        // given
        ValidPeriod period = ValidPeriod.of(
            LocalDateTime.of(2025, 1, 1, 0, 0),
            LocalDateTime.of(2025, 1, 31, 0, 0)
        );

        // when
        long days = period.getDays();

        // then
        assertThat(days).isEqualTo(30); // 1일부터 31일까지는 30일 차이
    }
}
