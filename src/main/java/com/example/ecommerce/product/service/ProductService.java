package com.example.ecommerce.product.service;

import com.example.ecommerce.product.domain.StockStatus;
import com.example.ecommerce.product.domain.Product;
import com.example.ecommerce.product.domain.ProductStatus;
import com.example.ecommerce.product.repository.ProductRepository;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Schema(description = "상품 등록 요청")
    public record ProductInPut(
        @NotBlank(message = "상품명은 필수입니다")
        @Schema(description = "상품명")
        String name,

        @NotNull(message = "가격은 필수입니다")
        @Min(value = 0, message = "가격은 0 이상이어야 합니다")
        @Schema(description = "가격")
        Long price,

        @NotNull(message = "재고는 필수입니다")
        @Min(value = 0, message = "재고는 0 이상이어야 합니다")
        @Schema(description = "재고")
        Integer stock,

        @Schema(description = "상세 설명")
        String comment
    ) {
        public Product toEntity() {
            return Product.builder()
                .name(name)
                .price(com.example.ecommerce.product.domain.Money.of(price))
                .stock(com.example.ecommerce.product.domain.Stock.of(stock))
                .comment(comment)
                .build();
        }
    }

    @Schema(description = "상품 응답")
    public record ProductOutPut(
        @Schema(description = "상품 ID") Long id,
        @Schema(description = "상품명") String name,
        @Schema(description = "가격") Long price,
        @Schema(description = "재고 상태") StockStatus status,
        @Schema(description = "생성 일시") LocalDateTime createdAt
        ) {
        public static ProductOutPut from(Product product) {
            return new ProductOutPut(
                product.getId(),
                product.getName(),
                product.getPrice().getAmount(),
                product.getStockStatus(),
                product.getCreatedAt()
            );
        }
    }

    @Schema(description = "상품 상세 정보 응답")
    public record ProductDetailOutPut(
        @Schema(description = "상품 ID") Long id,
        @Schema(description = "상품명") String name,
        @Schema(description = "가격") Long price,
        @Schema(description = "상세 설명") String comment,
        @Schema(description = "재고 상태") StockStatus status,
        @Schema(description = "재고량") Integer stock,
        @Schema(description = "생성 일시") LocalDateTime createdAt
    ) {
        public static ProductDetailOutPut from(Product product) {
            return new ProductDetailOutPut(
                product.getId(),
                product.getName(),
                product.getPrice().getAmount(),
                product.getComment(),
                product.getStockStatus(),
                product.getStock().getQuantity(),
                product.getCreatedAt()
            );
        }
    }

    //편의를 위한 기능) 상품 등록
    public ProductOutPut registerProduct(ProductInPut input) {
        Product product = input.toEntity();
        Product savedProduct = productRepository.save(product);
        return ProductOutPut.from(savedProduct);
    }

    //기능 1) 판매 중인 상품 목록 조회 (고객용)
    public Page<ProductOutPut> getActiveProducts(Pageable pageable) {
        Page<Product> products = productRepository.findByStatus(ProductStatus.ACTIVE, pageable);
        return products.map(ProductOutPut::from);
    }

    //기능 2) 특정 상품 정보 상세 조회
    public ProductDetailOutPut getProductDetail(Long id){
        Product result = productRepository.findByIdOrElseThrow(id);
        return ProductDetailOutPut.from(result);
    }
}
