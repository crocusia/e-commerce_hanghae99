package com.example.ecommerce.domain.product;

import com.example.ecommerce.common.exception.CustomException;
import com.example.ecommerce.product.domain.Money;
import com.example.ecommerce.product.domain.Product;
import com.example.ecommerce.product.domain.ProductStatus;
import com.example.ecommerce.product.domain.Stock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Product 도메인 테스트")
class ProductTest {

    @Test
    @DisplayName("상품을 생성할 수 있다")
    void createProduct() {
        // given
        String name = "맥북 프로";
        long price = 2_000_000L;
        String comment = "Apple M1 칩 탑재 맥북 프로";
        int stock = 10;

        // when
        Product product = Product.create(name, price, comment, stock);

        // then
        assertThat(product).isNotNull();
        assertThat(product.getName()).isEqualTo(name);
        assertThat(product.getPrice()).isEqualTo(Money.of(price));
        assertThat(product.getComment()).isEqualTo(comment);
        assertThat(product.getStock()).isEqualTo(Stock.of(stock));
        assertThat(product.getProductStatus()).isEqualTo(ProductStatus.ACTIVE);
    }

    @Test
    @DisplayName("재고가 충분하면 차감에 성공한다")
    void decreaseStock_Success() {
        // given
        Product product = Product.create("맥북 프로", 2_000_000L, "상세 설명", 10);

        // when
        product.decreaseStock(5);

        // then
        assertThat(product.getStock()).isEqualTo(Stock.of(5));
    }

    @Test
    @DisplayName("재고가 부족하면 예외가 발생한다")
    void decreaseStock_InsufficientStock() {
        // given
        Product product = Product.create("맥북 프로", 2_000_000L, "상세 설명", 3);

        // when & then
        assertThrows(CustomException.class, () ->
            product.decreaseStock(5)
        );
    }

    @Test
    @DisplayName("0 이하의 수량으로 차감하면 예외가 발생한다")
    void decreaseStock_InvalidQuantity() {
        // given
        Product product = Product.create("맥북 프로", 2_000_000L, "상세 설명", 10);

        // when & then
        assertThrows(CustomException.class, () ->
            product.decreaseStock(0)
        );
        assertThrows(CustomException.class, () ->
            product.decreaseStock(-5)
        );
    }

    @Test
    @DisplayName("재고를 복구할 수 있다")
    void increaseStock_Success() {
        // given
        Product product = Product.create("맥북 프로", 2_000_000L, "상세 설명", 5);

        // when
        product.increaseStock(3);

        // then
        assertThat(product.getStock()).isEqualTo(Stock.of(8));
    }

    @Test
    @DisplayName("0 이하의 수량으로 복구하면 예외가 발생한다")
    void increaseStock_InvalidQuantity() {
        // given
        Product product = Product.create("맥북 프로", 2_000_000L, "상세 설명", 10);

        // when & then
        assertThrows(CustomException.class, () ->
            product.increaseStock(0)
        );
        assertThrows(CustomException.class, () ->
            product.increaseStock(-3)
        );
    }

    @Test
    @DisplayName("상품 상태를 변경할 수 있다")
    void changeStatus() {
        // given
        Product product = Product.create("맥북 프로", 2_000_000L, "상세 설명", 10);

        // when
        product.changeStatus(ProductStatus.INACTIVE);

        // then
        assertThat(product.getProductStatus()).isEqualTo(ProductStatus.INACTIVE);
    }

    @Test
    @DisplayName("재고가 충분한지 확인할 수 있다")
    void hasEnoughStock() {
        // given
        Product product = Product.create("맥북 프로", 2_000_000L, "상세 설명", 10);

        // when & then
        assertTrue(product.hasEnoughStock(5));
        assertTrue(product.hasEnoughStock(10));
        assertFalse(product.hasEnoughStock(11));
    }

    @Test
    @DisplayName("판매 가능한 상품인지 확인할 수 있다")
    void isAvailable() {
        // given
        Product activeProduct = Product.create("맥북 프로", 2_000_000L, "상세 설명", 10);
        Product inactiveProduct = Product.create("아이패드", 1_000_000L, "아이패드 상세 설명", 5);
        inactiveProduct.changeStatus(ProductStatus.INACTIVE);

        // when & then
        assertTrue(activeProduct.isAvailable());
        assertFalse(inactiveProduct.isAvailable());
    }
}
