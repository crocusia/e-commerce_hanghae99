package com.example.ecommerce.product.repository;

import com.example.ecommerce.common.exception.CustomException;
import com.example.ecommerce.common.exception.ErrorCode;
import com.example.ecommerce.product.domain.Product;
import com.example.ecommerce.product.domain.status.ProductStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaProductRepository extends JpaRepository<Product, Long>, ProductRepository {

    @Override
    Page<Product> findByProductStatus(ProductStatus status, Pageable pageable);

    @Override
    Optional<Product> findById(Long id);

    @Override
    default Product findByIdOrElseThrow(Long id) {
        return findById(id)
            .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    @Override
    Product save(Product product);

    @Override
    @Query("SELECT p FROM Product p WHERE p.id IN :productIds")
    List<Product> findAllByIds(@Param("productIds") List<Long> productIds);
}
