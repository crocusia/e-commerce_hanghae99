package com.example.ecommerce.product.repository;

import com.example.ecommerce.product.domain.ProductPopular;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryProductPopularRepository implements ProductPopularRepository{
    @Override
    public ProductPopular save(ProductPopular productPopular){
        return ProductPopular.builder().build();
    }

    @Override
    public List<ProductPopular> findTopN(int topN){
        return List.of();
    }
}
