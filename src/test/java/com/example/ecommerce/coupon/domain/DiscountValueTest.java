package com.example.ecommerce.coupon.domain;

import com.example.ecommerce.common.exception.CustomException;
import com.example.ecommerce.coupon.domain.status.DiscountType;
import com.example.ecommerce.coupon.domain.vo.DiscountValue;
import com.example.ecommerce.product.domain.vo.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("DiscountValue VO 테스트")
class DiscountValueTest {

    @Test
    @DisplayName("정액 할인 DiscountValue를 생성할 수 있다")
    void createFixedDiscount() {
        // given
        Long discountPrice = 5000L;

        // when
        DiscountValue discountValue = DiscountValue.fixed(discountPrice);

        // then
        assertThat(discountValue).isNotNull();
        assertThat(discountValue.getDiscountType()).isEqualTo(DiscountType.FIXED);
        assertThat(discountValue.getDiscountPrice()).isEqualTo(discountPrice);
        assertThat(discountValue.getDiscountRate()).isNull();
    }

    @Test
    @DisplayName("정률 할인 DiscountValue를 생성할 수 있다")
    void createPercentageDiscount() {
        // given
        Double discountRate = 10.0;

        // when
        DiscountValue discountValue = DiscountValue.percentage(discountRate);

        // then
        assertThat(discountValue).isNotNull();
        assertThat(discountValue.getDiscountType()).isEqualTo(DiscountType.PERCENTAGE);
        assertThat(discountValue.getDiscountRate()).isEqualTo(discountRate);
        assertThat(discountValue.getDiscountPrice()).isNull();
    }

    @Test
    @DisplayName("정액 할인 금액이 null이면 생성할 수 없다")
    void createFixedDiscount_NullPrice() {
        // when & then
        assertThrows(CustomException.class, () -> DiscountValue.fixed(null));
    }

    @Test
    @DisplayName("정액 할인 금액이 0 이하면 생성할 수 없다")
    void createFixedDiscount_ZeroOrNegativePrice() {
        // when & then
        assertThrows(CustomException.class, () -> DiscountValue.fixed(0L));
    }

    @Test
    @DisplayName("정률 할인율이 null이면 생성할 수 없다")
    void createPercentageDiscount_NullRate() {
        // when & then
        assertThrows(CustomException.class, () -> DiscountValue.percentage(null));
    }

    @Test
    @DisplayName("정률 할인율이 0 이하면 생성할 수 없다")
    void createPercentageDiscount_ZeroOrNegativeRate() {
        // when & then
        assertThrows(CustomException.class, () -> DiscountValue.percentage(0.0));
        assertThrows(CustomException.class, () -> DiscountValue.percentage(-5.0));
    }

    @Test
    @DisplayName("정률 할인율이 100을 초과하면 생성할 수 없다")
    void createPercentageDiscount_OverHundredRate() {
        // when & then
        assertThrows(CustomException.class, () -> DiscountValue.percentage(101.0));
    }

    @Test
    @DisplayName("정액 할인 금액을 계산할 수 있다")
    void calculateFixedDiscountAmount() {
        // given
        DiscountValue discountValue = DiscountValue.fixed(5000L);
        Money orderAmount = Money.of(30000L);

        // when
        Money discountAmount = discountValue.calculateDiscountAmount(orderAmount);

        // then
        assertThat(discountAmount).isEqualTo(Money.of(5000L));
    }

    @Test
    @DisplayName("정률 할인 금액을 계산할 수 있다")
    void calculatePercentageDiscountAmount() {
        // given
        DiscountValue discountValue = DiscountValue.percentage(10.0);
        Money orderAmount = Money.of(30000L);

        // when
        Money discountAmount = discountValue.calculateDiscountAmount(orderAmount);

        // then
        assertThat(discountAmount).isEqualTo(Money.of(3000L));
    }

    @Test
    @DisplayName("같은 값을 가진 DiscountValue는 동등하다")
    void testEquals() {
        // given
        DiscountValue discount1 = DiscountValue.fixed(5000L);
        DiscountValue discount2 = DiscountValue.fixed(5000L);
        DiscountValue discount3 = DiscountValue.fixed(3000L);

        // when & then
        assertThat(discount1).isEqualTo(discount2);
        assertThat(discount1).isNotEqualTo(discount3);
        assertThat(discount1.hashCode()).isEqualTo(discount2.hashCode());
    }

    @Test
    @DisplayName("정액 할인인지 확인할 수 있다")
    void isFixed() {
        // given
        DiscountValue fixedDiscount = DiscountValue.fixed(5000L);
        DiscountValue percentageDiscount = DiscountValue.percentage(10.0);

        // when & then
        assertThat(fixedDiscount.isFixed()).isTrue();
        assertThat(percentageDiscount.isFixed()).isFalse();
    }

    @Test
    @DisplayName("정률 할인인지 확인할 수 있다")
    void isPercentage() {
        // given
        DiscountValue fixedDiscount = DiscountValue.fixed(5000L);
        DiscountValue percentageDiscount = DiscountValue.percentage(10.0);

        // when & then
        assertThat(fixedDiscount.isPercentage()).isFalse();
        assertThat(percentageDiscount.isPercentage()).isTrue();
    }
}