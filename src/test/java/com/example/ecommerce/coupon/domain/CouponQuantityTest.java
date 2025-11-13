package com.example.ecommerce.coupon.domain;

import com.example.ecommerce.common.exception.CustomException;
import com.example.ecommerce.coupon.domain.vo.CouponQuantity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("CouponQuantity VO 테스트")
class CouponQuantityTest {

    @Test
    @DisplayName("CouponQuantity를 생성할 수 있다")
    void create() {
        // given
        int totalQuantity = 100;

        // when
        CouponQuantity quantity = CouponQuantity.of(totalQuantity);

        // then
        assertThat(quantity).isNotNull();
        assertThat(quantity.getTotalQuantity()).isEqualTo(totalQuantity);
        assertThat(quantity.getIssuedQuantity()).isEqualTo(0);
    }

    @Test
    @DisplayName("총 수량이 0 이하면 생성할 수 없다")
    void create_InvalidTotalQuantity() {
        // when & then
        assertThrows(CustomException.class, () -> CouponQuantity.of(0));
        assertThrows(CustomException.class, () -> CouponQuantity.of(-1));
    }

    @Test
    @DisplayName("쿠폰을 발급할 수 있다")
    void issue() {
        // given
        CouponQuantity quantity = CouponQuantity.of(100);

        // when
        CouponQuantity issuedQuantity = quantity.issue();

        // then
        assertThat(issuedQuantity.getIssuedQuantity()).isEqualTo(1);
        assertThat(issuedQuantity.getTotalQuantity()).isEqualTo(100);
    }

    @Test
    @DisplayName("여러 번 발급할 수 있다")
    void issueMultiple() {
        // given
        CouponQuantity quantity = CouponQuantity.of(100);

        // when
        CouponQuantity issued1 = quantity.issue();
        CouponQuantity issued2 = issued1.issue();
        CouponQuantity issued3 = issued2.issue();

        // then
        assertThat(issued3.getIssuedQuantity()).isEqualTo(3);
    }

    @Test
    @DisplayName("수량이 소진되면 발급할 수 없다")
    void issue_Exhausted() {
        // given
        CouponQuantity quantity = CouponQuantity.of(2);
        CouponQuantity issued1 = quantity.issue();
        CouponQuantity issued2 = issued1.issue();

        // when & then
        assertThrows(CustomException.class, issued2::issue);
    }

    @Test
    @DisplayName("발급 가능 여부를 확인할 수 있다")
    void canIssue() {
        // given
        CouponQuantity quantity = CouponQuantity.of(2);

        // when & then
        assertThat(quantity.canIssue()).isTrue();

        CouponQuantity issued1 = quantity.issue();
        assertThat(issued1.canIssue()).isTrue();

        CouponQuantity issued2 = issued1.issue();
        assertThat(issued2.canIssue()).isFalse();
    }

    @Test
    @DisplayName("남은 수량을 확인할 수 있다")
    void getRemainingQuantity() {
        // given
        CouponQuantity quantity = CouponQuantity.of(100);

        // when & then
        assertThat(quantity.getRemainingQuantity()).isEqualTo(100);

        CouponQuantity issued = quantity.issue();
        assertThat(issued.getRemainingQuantity()).isEqualTo(99);
    }

    @Test
    @DisplayName("같은 값을 가진 CouponQuantity는 동등하다")
    void testEquals() {
        // given
        CouponQuantity quantity1 = CouponQuantity.of(100);
        CouponQuantity quantity2 = CouponQuantity.of(100);
        CouponQuantity quantity3 = CouponQuantity.of(50);

        // when & then
        assertThat(quantity1).isEqualTo(quantity2);
        assertThat(quantity1).isNotEqualTo(quantity3);
        assertThat(quantity1.hashCode()).isEqualTo(quantity2.hashCode());
    }

    @Test
    @DisplayName("발급된 수량이 다르면 동등하지 않다")
    void testEquals_DifferentIssuedQuantity() {
        // given
        CouponQuantity quantity1 = CouponQuantity.of(100);
        CouponQuantity quantity2 = quantity1.issue();

        // when & then
        assertThat(quantity1).isNotEqualTo(quantity2);
    }
}