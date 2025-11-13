package com.example.ecommerce.product.repository;

import com.example.ecommerce.product.domain.ProductStock;
import java.util.Optional;

public interface ProductStockRepository {
    ProductStock save(ProductStock productStock);

    Optional<ProductStock> findById(Long id);

    ProductStock findByIdOrElseThrow(Long id);
}
