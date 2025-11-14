package com.example.ecommerce.product.domain;

import com.example.ecommerce.common.domain.SoftDeleteEntity;
import com.example.ecommerce.product.domain.status.ProductStatus;
import com.example.ecommerce.product.domain.vo.Money;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class Product extends SoftDeleteEntity {

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "price", nullable = false))
    private Money price;

    @Column(name = "comment", length = 1000)
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_status", nullable = false)
    private ProductStatus productStatus;

    public static Product create(String name, long price, String comment) {
        return Product.builder()
            .name(name)
            .price(Money.of(price))
            .comment(comment)
            .productStatus(ProductStatus.ACTIVE)
            .build();
    }

    public boolean isAvailable() {
        return this.productStatus == ProductStatus.ACTIVE;
    }

    public Long getProductId() {
        return getId();
    }
}