package com.example.ecommerce.domain.product;

import com.example.ecommerce.product.domain.Product;
import com.example.ecommerce.product.domain.ProductStatus;
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
        int stock = 10;

        // when
        Product product = Product.create(name, price, stock);

        // then
        assertThat(product)
            .isNotNull()
            .extracting("name", "price", "stock", "productStatus")
            .containsExactly(name, price, stock, ProductStatus.ACTIVE);
    }
}
