package com.example.ecommerce.product.domain;

import com.example.ecommerce.product.domain.status.ProductStatus;
import com.example.ecommerce.product.domain.vo.Money;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
public class Product {

    private final Long productId;
    private String name;
    private Money price;
    private String comment;
    private ProductStatus productStatus;

    private LocalDateTime deletedAt;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder
    private Product(Long id, String name, Money price, String comment, ProductStatus status) {
        this.productId = id;
        this.name = name;
        this.price = price;
        this.comment = comment;
        this.productStatus = status;
        this.deletedAt = null;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public static Product create(String name, long price, String comment) {
        return Product.builder()
            .name(name)
            .price(Money.of(price))
            .comment(comment)
            .build();
    }

    public boolean isAvailable() {
        return this.productStatus == ProductStatus.ACTIVE;
    }
}