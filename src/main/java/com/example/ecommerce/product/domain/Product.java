package com.example.ecommerce.product.domain;

import com.example.ecommerce.common.exception.CustomException;
import com.example.ecommerce.common.exception.ErrorCode;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

    private static final int LOW_STOCK_THRESHOLD = 10;

    private Long id; //식별자 - 추후 스노플레이크 상품번호 추가 가능성
    private String name; //상품명
    private Long price; //가격
    private String comment; //상세 설명
    private int stock; //재고
    private ProductStatus productStatus; //상품 판매 상태
    private int stockQty; //예약 재고 - 추후 별도 테이블 관리 가능성 있음
    private Long viewCount; //조회 수
    private int salesCount; //판매 수
    private Double popularityScore; //종합 인기 점수
    private LocalDateTime deletedAt; //삭제 일시 (soft delete)
    private LocalDateTime createdAt; //생성 일시
    private LocalDateTime updatedAt; //수정 일시

    public static Product create(String name, long price, int stock) {
        validateName(name);
        validatePrice(price);
        validateStock(stock);

        Product product = new Product();
        product.name = name;
        product.price = price;
        product.stock = stock;
        product.productStatus = ProductStatus.ACTIVE;
        product.createdAt = LocalDateTime.now();
        product.updatedAt = LocalDateTime.now();
        return product;
    }

    public boolean isAvailable() {
        return this.productStatus == ProductStatus.ACTIVE;
    }

    public StockStatus getStockStatus(){
        if(stock <= 0){
            return StockStatus.OUT_OF_STOCK;
        }
        else if(stock <= LOW_STOCK_THRESHOLD){
            return StockStatus.LOW_STOCK;
        }
        return StockStatus.AVAILABLE;
    }

    // 검증 메서드
    private static void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private static void validatePrice(long price) {
        if (price < 0) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private static void validateStock(int stock) {
        if (stock < 0) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}
