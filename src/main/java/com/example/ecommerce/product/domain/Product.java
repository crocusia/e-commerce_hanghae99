package com.example.ecommerce.product.domain;

import com.example.ecommerce.common.exception.CustomException;
import com.example.ecommerce.common.exception.ErrorCode;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

    private Long id; //식별자 - 추후 스노플레이크 상품번호 추가 가능성
    private String name; //상품명
    private Money price; //가격
    private String comment; //상세 설명
    private Stock stock; //재고
    private ProductStatus productStatus; //상품 판매 상태
    private int stockQty; //예약 재고 - 추후 별도 테이블 관리 가능성 있음
    private Long viewCount; //조회 수
    private int salesCount; //판매 수
    private Double popularityScore; //종합 인기 점수
    private LocalDateTime deletedAt; //삭제 일시 (soft delete)
    private LocalDateTime createdAt; //생성 일시
    private LocalDateTime updatedAt; //수정 일시

    @Builder
    private Product(Long id, String name, Money price, String comment, Stock stock) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.comment = comment;
        this.stock = stock;
        this.productStatus = ProductStatus.ACTIVE;
        this.stockQty = 0;
        this.viewCount = 0L;
        this.salesCount = 0;
        this.popularityScore = 0d;
        this.deletedAt = null;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public static Product create(String name, long price, String comment, int stock) {
        validateName(name);

        return Product.builder()
            .name(name)
            .price(Money.of(price))
            .comment(comment)
            .stock(Stock.of(stock))
            .build();
    }

    public void decreaseStock(int quantity) {
        this.stock = this.stock.decrease(quantity);
        this.updatedAt = LocalDateTime.now();
    }

    public void increaseStock(int quantity) {
        this.stock = this.stock.increase(quantity);
        this.updatedAt = LocalDateTime.now();
    }

    public void changeStatus(ProductStatus status) {
        if (status == null) {
            throw new CustomException(ErrorCode.INVALID_PRODUCT_STATUS);
        }
        this.productStatus = status;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean hasEnoughStock(int quantity) {
        return this.stock.hasEnough(quantity);
    }

    public boolean isAvailable() {
        return this.productStatus == ProductStatus.ACTIVE;
    }

    public StockStatus getStockStatus(){
        return this.stock.getStatus();
    }

    // 검증 메서드
    private static void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}
