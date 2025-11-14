package com.example.ecommerce.product.repository;

import com.example.ecommerce.product.domain.Product;
import com.example.ecommerce.product.domain.status.ProductStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductRepository {
    Page<Product> findByProductStatus(ProductStatus status, Pageable pageable);

    Optional<Product> findById(Long id);

    Product findByIdOrElseThrow(Long id);

    Product save(Product product);

    List<Product> findAllByIds(List<Long> productIds);

    void deleteAllInBatch();
}
