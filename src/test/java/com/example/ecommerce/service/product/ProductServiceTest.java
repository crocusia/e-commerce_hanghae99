package com.example.ecommerce.service.product;

import com.example.ecommerce.common.exception.CustomException;
import com.example.ecommerce.common.exception.ErrorCode;
import com.example.ecommerce.product.domain.Product;
import com.example.ecommerce.product.repository.ProductRepository;
import com.example.ecommerce.product.domain.ProductStatus;
import com.example.ecommerce.product.service.ProductService;
import com.example.ecommerce.product.service.ProductService.ProductDetailOutPut;
import com.example.ecommerce.product.service.ProductService.ProductOutPut;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService 테스트")
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    @DisplayName("고객은 판매중인 상품 목록을 페이징으로 조회할 수 있다")
    void getActiveProducts() {
        // given
        Product activeProduct1 = Product.create("갤럭시", 2_000_000L, 10);
        ReflectionTestUtils.setField(activeProduct1, "id", 1L);

        Product activeProduct2 = Product.create("갤럭시탭", 1_500_000L, 20);
        ReflectionTestUtils.setField(activeProduct2, "id", 2L);

        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> productPage = new PageImpl<>(List.of(activeProduct1, activeProduct2), pageable, 2);

        when(productRepository.findByStatus(ProductStatus.ACTIVE, pageable))
            .thenReturn(productPage);

        // when
        Page<ProductOutPut> result = productService.getActiveProducts(pageable);

        // then
        verify(productRepository).findByStatus(ProductStatus.ACTIVE, pageable);
    }

    @Test
    @DisplayName("특정 상품의 상세 정보를 조회할 수 있다")
    void getProductDetail() {
        // given
        Long productId = 1L;
        Product product = Product.create("맥북 프로", 2_000_000L, 10);
        ReflectionTestUtils.setField(product, "id", productId);

        when(productRepository.findByIdOrElseThrow(productId))
            .thenReturn(product);

        // when
        ProductDetailOutPut result = productService.getProductDetail(productId);

        // then
        verify(productRepository).findByIdOrElseThrow(productId);
    }

    @Test
    @DisplayName("존재하지 않는 상품 조회 시 예외가 발생한다")
    void getProductDetail_NotFound() {
        // given
        Long productId = 999L;

        when(productRepository.findByIdOrElseThrow(productId))
            .thenThrow(new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        // when & then
        assertThrows(CustomException.class, () -> {
            productService.getProductDetail(productId);
        });

        verify(productRepository).findByIdOrElseThrow(productId);
    }
}
