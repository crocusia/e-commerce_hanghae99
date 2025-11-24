package com.example.ecommerce.product.repository;

import com.example.ecommerce.common.exception.CustomException;
import com.example.ecommerce.common.exception.ErrorCode;
import com.example.ecommerce.product.domain.ProductStock;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaProductStockRepository extends JpaRepository<ProductStock, Long>, ProductStockRepository {

    @Override
    ProductStock save(ProductStock productStock);

    @Override
    Optional<ProductStock> findById(Long id);

    @Override
    Optional<ProductStock> findByProductId(Long productId);

    @Override
    default ProductStock findByIdOrElseThrow(Long id) {
        return findById(id)
            .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    @Override
    @Query("SELECT ps FROM ProductStock ps WHERE ps.id = :id")
    ProductStock findByIdWithLock(@Param("id") Long id);

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ps FROM ProductStock ps WHERE ps.productId = :productId")
    ProductStock findByProductIdWithLock(@Param("productId") Long productId);
}
