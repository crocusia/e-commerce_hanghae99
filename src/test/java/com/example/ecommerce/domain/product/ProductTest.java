package com.example.ecommerce.domain.product;

import com.example.ecommerce.common.exception.CustomException;
import com.example.ecommerce.product.domain.Money;
import com.example.ecommerce.product.domain.Product;
import com.example.ecommerce.product.domain.ProductStatus;
import com.example.ecommerce.product.domain.Stock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Product 도메인 테스트")
class ProductTest {

    private Product product;

    @BeforeEach
    void setUp() {
        this.product = Product.create("상품1", 1000L, "상세 설명", 10);
    }

    @Test
    @DisplayName("상품을 생성할 수 있다")
    void createProduct() {
        // given
        String name = "상품1";
        long price = 1000L;
        String comment = "상세 설명";
        int stock = 10;

        // when
        Product productNew = Product.create(name, price, comment, stock);

        // then
        assertThat(productNew).isNotNull();
        assertThat(productNew.getName()).isEqualTo(name);
        assertThat(productNew.getPrice()).isEqualTo(Money.of(price));
        assertThat(productNew.getComment()).isEqualTo(comment);
        assertThat(productNew.getStock()).isEqualTo(Stock.of(stock));
        assertThat(productNew.getProductStatus()).isEqualTo(ProductStatus.ACTIVE);
    }

    @Test
    @DisplayName("재고가 충분하면 차감에 성공한다")
    void decreaseStock_Success() {
        // when
        product.decreaseStock(10);
        // then
        assertThat(product.getStock()).isEqualTo(Stock.of(0));
    }

    @Test
    @DisplayName("재고가 부족하면 예외가 발생한다")
    void decreaseStock_InsufficientStock() {
        // when & then
        assertThrows(CustomException.class, () ->
            product.decreaseStock(11)
        );
    }

    @Test
    @DisplayName("수량 0으로 차감하면 예외가 발생한다")
    void decreaseStock_QuantityIsZero() {
        assertThrows(CustomException.class, () ->
            product.decreaseStock(0)
        );
    }

    @Test
    @DisplayName("음수 수량으로 차감하면 예외가 발생한다")
    void decreaseStock_QuantityIsNegative() {
        assertThrows(CustomException.class, () ->
            product.decreaseStock(-5)
        );
    }

    @Test
    @DisplayName("재고를 복구할 수 있다")
    void increaseStock_Success() {
        // when
        product.increaseStock(7);
        // then
        assertThat(product.getStock()).isEqualTo(Stock.of(17));
    }

    @Test
    @DisplayName("수량 0으로 복구하면 예외가 발생한다")
    void increaseStock_QuantityIsZero() {
        // when & then
        assertThrows(CustomException.class, () ->
            product.increaseStock(0)
        );
    }

    @Test
    @DisplayName("음수 수량으로 복구하면 예외가 발생한다")
    void increaseStock_QuantityIsNegative() {
        // when & then
        assertThrows(CustomException.class, () ->
            product.increaseStock(-3)
        );
    }

    @Test
    @DisplayName("상품 상태를 변경할 수 있다")
    void changeStatus() {
        // when
        product.changeStatus(ProductStatus.INACTIVE);

        // then
        assertThat(product.getProductStatus()).isEqualTo(ProductStatus.INACTIVE);
    }

    @Test
    @DisplayName("요청 수량이 재고보다 적으면 true를 반환한다")
    void hasEnoughStock_LessThanStock() {
        // when & then
        assertTrue(product.hasEnoughStock(5));
    }

    @Test
    @DisplayName("요청 수량이 재고와 같으면 true를 반환한다")
    void hasEnoughStock_EqualsToStock() {
        // when & then
        assertTrue(product.hasEnoughStock(10));
    }

    @Test
    @DisplayName("요청 수량이 재고보다 많으면 false를 반환한다")
    void hasEnoughStock_MoreThanStock() {
        // when & then
        assertFalse(product.hasEnoughStock(11));
    }

    @Test
    @DisplayName("상품 상태가 ACTIVE이면 판매 가능 여부를 true로 반환한다")
    void isAvailable_ActiveProduct() {
        // when & then
        assertTrue(product.isAvailable());
    }

    @Test
    @DisplayName("상품 상태가 INACTIVE이면 판매 가능 여부를 false로 반환한다")
    void isAvailable_InactiveProduct() {
        // given
        Product inactiveProduct = Product.create("아이패드", 1_000_000L, "아이패드 상세 설명", 5);
        inactiveProduct.changeStatus(ProductStatus.INACTIVE);

        // when & then
        assertFalse(inactiveProduct.isAvailable());
    }

    @Test
    @DisplayName("상품 상태가 DELETED이면 판매 가능 여부를 false로 반환한다")
    void isAvailable_DeletedProduct() {
        // given
        Product inactiveProduct = Product.create("아이패드", 1_000_000L, "아이패드 상세 설명", 5);
        inactiveProduct.changeStatus(ProductStatus.DELETED);

        // when & then
        assertFalse(inactiveProduct.isAvailable());
    }
}
