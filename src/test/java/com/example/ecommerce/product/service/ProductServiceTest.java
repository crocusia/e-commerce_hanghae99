package com.example.ecommerce.product.service;

import com.example.ecommerce.common.exception.CustomException;
import com.example.ecommerce.product.domain.Product;
import com.example.ecommerce.product.domain.ProductPopular;
import com.example.ecommerce.product.domain.ProductStock;
import com.example.ecommerce.product.domain.status.ProductStatus;
import com.example.ecommerce.product.domain.status.StockStatus;
import com.example.ecommerce.product.domain.vo.Money;
import com.example.ecommerce.product.domain.vo.Stock;
import com.example.ecommerce.product.dto.ProductDetailResponse;
import com.example.ecommerce.product.dto.ProductRequest;
import com.example.ecommerce.product.dto.ProductResponse;
import com.example.ecommerce.product.repository.ProductPopularRepository;
import com.example.ecommerce.product.repository.ProductRepository;
import com.example.ecommerce.product.repository.ProductStockRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService 테스트")
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductStockRepository stockRepository;

    @Mock
    private ProductPopularRepository popularRepository;

    @InjectMocks
    private ProductService productService;

    // 헬퍼 메서드
    private Product createProduct(Long id, String name, long price, ProductStatus status) {
        return Product.builder()
            .id(id)
            .name(name)
            .price(Money.of(price))
            .comment("상품 설명")
            .productStatus(status)
            .build();
    }

    private ProductStock createProductStock(Long productId, int stock) {
        return ProductStock.builder()
            .id(productId)
            .currentStock(Stock.of(stock))
            .build();
    }

    private ProductPopular createProductPopular(Long productId, long salesCount, int rank) {
        return ProductPopular.builder()
            .productId(productId)
            .salesCount(salesCount)
            .rank(rank)
            .build();
    }

    @Nested
    @DisplayName("상품 생성 테스트")
    class CreateProductTest {

        @Test
        @DisplayName("상품을 정상적으로 생성한다")
        void createProduct_Success() {
            // given
            ProductRequest request = new ProductRequest("테스트 상품", 10000L, 100, "상품 설명");
            Product savedProduct = createProduct(1L, "테스트 상품", 10000L, ProductStatus.ACTIVE);
            ProductStock savedStock = createProductStock(1L, 100);

            given(productRepository.save(any(Product.class))).willReturn(savedProduct);
            given(stockRepository.save(any(ProductStock.class))).willReturn(savedStock);

            // when
            ProductDetailResponse response = productService.createProduct(request);

            // then
            assertAll(
                () -> assertThat(response.id()).isEqualTo(1L),
                () -> assertThat(response.name()).isEqualTo("테스트 상품"),
                () -> assertThat(response.price()).isEqualTo(10000L),
                () -> assertThat(response.stock()).isEqualTo(100),
                () -> then(productRepository).should(times(1)).save(any(Product.class)),
                () -> then(stockRepository).should(times(1)).save(any(ProductStock.class))
            );
        }

        @Test
        @DisplayName("상품과 재고를 함께 저장한다")
        void createProductWithStock() {
            // given
            ProductRequest request = new ProductRequest("테스트 상품", 10000L,  50,"상품 설명");
            Product savedProduct = createProduct(1L, "테스트 상품", 10000L, ProductStatus.ACTIVE);
            ProductStock savedStock = createProductStock(1L, 50);

            given(productRepository.save(any(Product.class))).willReturn(savedProduct);
            given(stockRepository.save(any(ProductStock.class))).willReturn(savedStock);

            // when
            productService.createProduct(request);

            // then
            then(productRepository).should().save(any(Product.class));
            then(stockRepository).should().save(any(ProductStock.class));
        }
    }

    @Nested
    @DisplayName("활성 상품 목록 조회 테스트")
    class GetActiveProductsTest {

        @Test
        @DisplayName("활성 상품 목록을 페이징하여 조회한다")
        void getActiveProducts() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            List<Product> products = Arrays.asList(
                createProduct(1L, "상품1", 10000L, ProductStatus.ACTIVE),
                createProduct(2L, "상품2", 20000L, ProductStatus.ACTIVE)
            );
            Page<Product> productPage = new PageImpl<>(products, pageable, products.size());

            given(productRepository.findByProductStatus(ProductStatus.ACTIVE, pageable)).willReturn(productPage);

            // when
            Page<ProductResponse> result = productService.getActiveProducts(pageable);

            // then
            assertAll(
                () -> assertThat(result.getContent()).hasSize(2),
                () -> assertThat(result.getContent().get(0).name()).isEqualTo("상품1"),
                () -> assertThat(result.getContent().get(1).name()).isEqualTo("상품2"),
                () -> then(productRepository).should().findByProductStatus(ProductStatus.ACTIVE, pageable)
            );
        }
    }

    @Nested
    @DisplayName("상품 상세 조회 테스트")
    class GetProductDetailTest {

        @Test
        @DisplayName("상품 상세 정보를 조회한다")
        void getProductDetail() {
            // given
            Long productId = 1L;
            Product product = createProduct(productId, "테스트 상품", 10000L, ProductStatus.ACTIVE);
            ProductStock stock = createProductStock(productId, 100);

            given(productRepository.findByIdOrElseThrow(productId)).willReturn(product);
            given(stockRepository.findByIdOrElseThrow(productId)).willReturn(stock);

            // when
            ProductDetailResponse response = productService.getProductDetail(productId);

            // then
            assertAll(
                () -> assertThat(response.id()).isEqualTo(productId),
                () -> assertThat(response.name()).isEqualTo("테스트 상품"),
                () -> assertThat(response.price()).isEqualTo(10000L),
                () -> assertThat(response.stock()).isEqualTo(100),
                () -> assertThat(response.status()).isEqualTo(StockStatus.AVAILABLE),
                () -> then(productRepository).should().findByIdOrElseThrow(productId),
                () -> then(stockRepository).should().findByIdOrElseThrow(productId)
            );
        }

        @Test
        @DisplayName("재고가 부족하면 LOW_STOCK 상태를 반환한다")
        void getProductDetailWithLowStock() {
            // given
            Long productId = 1L;
            Product product = createProduct(productId, "테스트 상품", 10000L, ProductStatus.ACTIVE);
            ProductStock stock = createProductStock(productId, 5);

            given(productRepository.findByIdOrElseThrow(productId)).willReturn(product);
            given(stockRepository.findByIdOrElseThrow(productId)).willReturn(stock);

            // when
            ProductDetailResponse response = productService.getProductDetail(productId);

            // then
            assertThat(response.status()).isEqualTo(StockStatus.LOW_STOCK);
        }

        @Test
        @DisplayName("재고가 없으면 OUT_OF_STOCK 상태를 반환한다")
        void getProductDetailWithOutOfStock() {
            // given
            Long productId = 1L;
            Product product = createProduct(productId, "테스트 상품", 10000L, ProductStatus.ACTIVE);
            ProductStock stock = createProductStock(productId, 0);

            given(productRepository.findByIdOrElseThrow(productId)).willReturn(product);
            given(stockRepository.findByIdOrElseThrow(productId)).willReturn(stock);

            // when
            ProductDetailResponse response = productService.getProductDetail(productId);

            // then
            assertThat(response.status()).isEqualTo(StockStatus.OUT_OF_STOCK);
        }

        @Test
        @DisplayName("존재하지 않는 상품을 조회하면 예외가 발생한다")
        void getProductDetailWithNotFound() {
            // given
            Long productId = 999L;
            given(productRepository.findByIdOrElseThrow(productId)).willThrow(CustomException.class);

            // when & then
            assertThatThrownBy(() -> productService.getProductDetail(productId))
                .isInstanceOf(CustomException.class);
        }
    }

    @Nested
    @DisplayName("인기 상품 조회 테스트")
    class GetPopularProductsTest {

        @Test
        @DisplayName("인기 상품 목록을 조회한다")
        void getPopularProducts() {
            // given
            int limit = 5;
            List<ProductPopular> popularProducts = Arrays.asList(
                createProductPopular(1L, 1000L, 1),
                createProductPopular(2L, 800L, 2),
                createProductPopular(3L, 600L, 3)
            );
            List<Product> products = Arrays.asList(
                createProduct(1L, "인기상품1", 10000L, ProductStatus.ACTIVE),
                createProduct(2L, "인기상품2", 20000L, ProductStatus.ACTIVE),
                createProduct(3L, "인기상품3", 30000L, ProductStatus.ACTIVE)
            );

            given(popularRepository.findTopN(limit)).willReturn(popularProducts);
            given(productRepository.findAllByIds(Arrays.asList(1L, 2L, 3L))).willReturn(products);

            // when
            List<ProductResponse> result = productService.getPopularProducts(limit);

            // then
            assertAll(
                () -> assertThat(result).hasSize(3),
                () -> assertThat(result.get(0).name()).isEqualTo("인기상품1"),
                () -> assertThat(result.get(1).name()).isEqualTo("인기상품2"),
                () -> assertThat(result.get(2).name()).isEqualTo("인기상품3"),
                () -> then(popularRepository).should().findTopN(limit),
                () -> then(productRepository).should().findAllByIds(any())
            );
        }

        @Test
        @DisplayName("인기 상품 목록이 비어있으면 빈 리스트를 반환한다")
        void getPopularProductsWithEmpty() {
            // given
            int limit = 5;
            given(popularRepository.findTopN(limit)).willReturn(Collections.emptyList());

            // when
            List<ProductResponse> result = productService.getPopularProducts(limit);

            // then
            assertAll(
                () -> assertThat(result).isEmpty(),
                () -> then(popularRepository).should().findTopN(limit),
                () -> then(productRepository).should(times(0)).findAllByIds(any())
            );
        }

        @Test
        @DisplayName("INACTIVE 상품은 인기 상품 목록에서 제외한다")
        void getPopularProductsFilterInactive() {
            // given
            int limit = 5;
            List<ProductPopular> popularProducts = Arrays.asList(
                createProductPopular(1L, 1000L, 1),
                createProductPopular(2L, 800L, 2),
                createProductPopular(3L, 600L, 3)
            );
            List<Product> products = Arrays.asList(
                createProduct(1L, "인기상품1", 10000L, ProductStatus.ACTIVE),
                createProduct(2L, "인기상품2", 20000L, ProductStatus.INACTIVE),
                createProduct(3L, "인기상품3", 30000L, ProductStatus.ACTIVE)
            );

            given(popularRepository.findTopN(limit)).willReturn(popularProducts);
            given(productRepository.findAllByIds(Arrays.asList(1L, 2L, 3L))).willReturn(products);

            // when
            List<ProductResponse> result = productService.getPopularProducts(limit);

            // then
            assertAll(
                () -> assertThat(result).hasSize(2),
                () -> assertThat(result.get(0).name()).isEqualTo("인기상품1"),
                () -> assertThat(result.get(1).name()).isEqualTo("인기상품3")
            );
        }

        @Test
        @DisplayName("순위 순서대로 인기 상품을 반환한다")
        void getPopularProductsInOrder() {
            // given
            int limit = 3;
            List<ProductPopular> popularProducts = Arrays.asList(
                createProductPopular(3L, 1000L, 1),
                createProductPopular(1L, 800L, 2),
                createProductPopular(2L, 600L, 3)
            );
            List<Product> products = Arrays.asList(
                createProduct(1L, "상품A", 10000L, ProductStatus.ACTIVE),
                createProduct(2L, "상품B", 20000L, ProductStatus.ACTIVE),
                createProduct(3L, "상품C", 30000L, ProductStatus.ACTIVE)
            );

            given(popularRepository.findTopN(limit)).willReturn(popularProducts);
            given(productRepository.findAllByIds(any())).willReturn(products);

            // when
            List<ProductResponse> result = productService.getPopularProducts(limit);

            // then
            assertAll(
                () -> assertThat(result).hasSize(3),
                () -> assertThat(result.get(0).id()).isEqualTo(3L),
                () -> assertThat(result.get(1).id()).isEqualTo(1L),
                () -> assertThat(result.get(2).id()).isEqualTo(2L)
            );
        }
    }
}
