package com.example.ecommerce.product.domain;

import com.example.ecommerce.product.domain.status.ProductStatus;
import com.example.ecommerce.product.domain.vo.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@DisplayName("Product 도메인 테스트")
class ProductTest {

    @Nested
    @DisplayName("생성 테스트")
    class CreateTest {
        @Test
        @DisplayName("create 팩토리 메서드로 상품을 생성한다")
        void createProductUsingFactoryMethod() {
            // given
            String name = "테스트 상품";
            long price = 10000L;
            String comment = "테스트 상품입니다";

            // when
            Product product = Product.create(name, price, comment);

            // then
            assertAll(
                () -> assertThat(product.getName()).isEqualTo(name),
                () -> assertThat(product.getPrice()).isEqualTo(Money.of(price)),
                () -> assertThat(product.getComment()).isEqualTo(comment)
            );
        }
    }

    @Nested
    @DisplayName("상품 상태 확인 테스트")
    class AvailabilityTest {

        @Test
        @DisplayName("ACTIVE 상태인 상품은 사용 가능하다")
        void activeProductIsAvailable() {
            // given
            Product product = Product.builder()
                .name("상품")
                .price(Money.of(10000L))
                .comment("설명")
                .status(ProductStatus.ACTIVE)
                .build();

            // when
            boolean available = product.isAvailable();

            // then
            assertThat(available).isTrue();
        }

        @Test
        @DisplayName("INACTIVE 상태인 상품은 사용 불가능하다")
        void inactiveProductIsNotAvailable() {
            // given
            Product product = Product.builder()
                .name("상품")
                .price(Money.of(10000L))
                .comment("설명")
                .status(ProductStatus.INACTIVE)
                .build();

            // when
            boolean available = product.isAvailable();

            // then
            assertThat(available).isFalse();
        }

        @Test
        @DisplayName("상태가 DELETED인 상품은 사용 불가능하다")
        void nullStatusProductIsNotAvailable() {
            // given
            Product product = Product.builder()
                .name("상품")
                .price(Money.of(10000L))
                .comment("설명")
                .status(ProductStatus.DELETED)
                .build();

            // when
            boolean available = product.isAvailable();

            // then
            assertThat(available).isFalse();
        }
    }
}
