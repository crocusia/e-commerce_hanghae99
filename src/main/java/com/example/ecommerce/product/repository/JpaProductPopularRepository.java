package com.example.ecommerce.product.repository;

import com.example.ecommerce.product.domain.ProductPopular;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaProductPopularRepository extends JpaRepository<ProductPopular, Long>, ProductPopularRepository {

    @Override
    ProductPopular save(ProductPopular productPopular);

    @Override
    @Query("SELECT pp FROM ProductPopular pp WHERE pp.rank > 0 AND pp.rank <= :topN ORDER BY pp.rank ASC")
    List<ProductPopular> findTopN(@Param("topN") int topN);
}
