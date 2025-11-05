package com.example.ecommerce.product.repository;

import com.example.ecommerce.product.domain.Product;
import com.example.ecommerce.product.domain.ProductStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductRepository {
    Page<Product> findByStatus(ProductStatus status, Pageable pageable);

    Optional<Product> findById(Long id);

    Product findByIdOrElseThrow(Long id);
}
