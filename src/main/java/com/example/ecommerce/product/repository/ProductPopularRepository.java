package com.example.ecommerce.product.repository;

import com.example.ecommerce.product.domain.ProductPopular;
import java.util.List;


public interface ProductPopularRepository {
    ProductPopular save(ProductPopular productPopular);

    List<ProductPopular> findTopN(int topN);
}
