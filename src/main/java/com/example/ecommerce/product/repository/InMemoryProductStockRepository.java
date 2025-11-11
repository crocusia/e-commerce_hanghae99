package com.example.ecommerce.product.repository;

import com.example.ecommerce.product.domain.ProductStock;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryProductStockRepository implements ProductStockRepository{
    @Override
    public ProductStock save(ProductStock productStock){
        return ProductStock.builder().build();
    }

    @Override
    public Optional<ProductStock> findById(Long id){
        return Optional.ofNullable(ProductStock.builder().build());
    }

    @Override
    public ProductStock findByIdOrElseThrow(Long id){
        return ProductStock.builder().build();
    }
}
