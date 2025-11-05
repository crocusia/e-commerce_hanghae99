package com.example.ecommerce.product.repository;

import com.example.ecommerce.common.exception.CustomException;
import com.example.ecommerce.common.exception.ErrorCode;
import com.example.ecommerce.product.domain.Product;
import com.example.ecommerce.product.domain.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class InMemoryProductRepository implements ProductRepository {

    private final Map<Long, Product> store = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1L);

    public Page<Product> findByStatus(ProductStatus status, Pageable pageable) {
        List<Product> filtered = store.values().stream()
            .filter(p -> status.equals(p.getProductStatus()))
            .collect(Collectors.toList());

        // 페이징
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filtered.size());

        if (start > filtered.size()) {
            return new PageImpl<>(Collections.emptyList(), pageable, filtered.size());
        }

        List<Product> pageContent = filtered.subList(start, end);

        return new PageImpl<>(pageContent, pageable, filtered.size());
    }

    @Override
    public Optional<Product> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Product findByIdOrElseThrow(Long id) {
        return findById(id).orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    @Override
    public Product save(Product product) {
        Product savedProduct = product;

        if (product.getId() == null) {
            savedProduct = Product.builder()
                .id(idGenerator.getAndIncrement())
                .name(product.getName())
                .price(product.getPrice())
                .comment(product.getComment())
                .stock(product.getStock())
                .build();
        }

        store.put(savedProduct.getId(), savedProduct);
        return savedProduct;
    }
}
