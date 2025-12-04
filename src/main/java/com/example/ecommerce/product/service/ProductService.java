package com.example.ecommerce.product.service;

import com.example.ecommerce.product.domain.Product;
import com.example.ecommerce.product.domain.ProductPopular;
import com.example.ecommerce.product.domain.ProductStock;
import com.example.ecommerce.product.domain.status.ProductStatus;
import com.example.ecommerce.product.domain.status.StockStatus;
import com.example.ecommerce.product.domain.vo.Stock;
import com.example.ecommerce.product.dto.ProductDetailResponse;
import com.example.ecommerce.product.dto.ProductRequest;
import com.example.ecommerce.product.dto.ProductResponse;
import com.example.ecommerce.product.repository.ProductPopularRepository;
import com.example.ecommerce.product.repository.ProductRepository;
import com.example.ecommerce.product.repository.ProductStockRepository;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private static final int LOW_STOCK_THRESHOLD = 10;

    private final ProductRepository productRepository;
    private final ProductStockRepository stockRepository;
    private final ProductPopularRepository popularRepository;

    @Transactional
    public ProductDetailResponse createProduct(ProductRequest input) {
        Product product = input.toEntity();
        Product savedProduct = productRepository.save(product);

        ProductStock productStock = ProductStock.builder()
            .id(savedProduct.getProductId())
            .currentStock(Stock.of(input.stock()))
            .build();
        ProductStock savedProductStock = stockRepository.save(productStock);
        StockStatus status = productStock.getStockStatus(LOW_STOCK_THRESHOLD);

        return ProductDetailResponse.from(savedProduct, status, savedProductStock);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getActiveProducts(Pageable pageable) {
        Page<Product> products = productRepository.findByProductStatus(ProductStatus.ACTIVE, pageable);
        return products.map(ProductResponse::from);
    }

    @Transactional(readOnly = true)
    public ProductDetailResponse getProductDetail(Long id){
        Product result = productRepository.findByIdOrElseThrow(id);
        ProductStock stock = stockRepository.findByIdOrElseThrow(id);
        StockStatus status = stock.getStockStatus(LOW_STOCK_THRESHOLD);
        return ProductDetailResponse.from(result, status, stock);
    }

    @Cacheable(value = "product:popular", key = "#limit")
    @Transactional(readOnly = true)
    public List<ProductResponse> getPopularProducts(int limit) {
        log.debug("인기 상품 조회 (캐시 미스) - limit: {}", limit);

        List<ProductPopular> popularProducts = popularRepository.findTopN(limit);

        if (popularProducts.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> productIds = popularProducts.stream()
            .map(ProductPopular::getProductId)
            .collect(Collectors.toList());

        List<Product> products = productRepository.findAllByIds(productIds);

        Map<Long, Product> productMap = products.stream()
            .collect(Collectors.toMap(Product::getProductId, Function.identity()));

        return popularProducts.stream()
            .map(ProductPopular::getProductId)
            .map(productMap::get)
            .filter(product -> product != null && product.isAvailable())
            .map(ProductResponse::from)
            .collect(Collectors.toList());
    }

}
