package com.example.ecommerce.product.repository;

import com.example.ecommerce.common.exception.CustomException;
import com.example.ecommerce.common.exception.ErrorCode;
import com.example.ecommerce.product.domain.ProductStock;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaProductStockRepository extends JpaRepository<ProductStock, Long>, ProductStockRepository {

    @Override
    ProductStock save(ProductStock productStock);

    @Override
    Optional<ProductStock> findById(Long id);

    @Override
    default ProductStock findByIdOrElseThrow(Long id) {
        return findById(id)
            .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
    }
}
