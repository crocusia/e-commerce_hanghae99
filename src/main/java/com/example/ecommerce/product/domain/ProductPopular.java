package com.example.ecommerce.product.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "product_populars")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ProductPopular {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "sales_count", nullable = false)
    private long salesCount;

    @Column(name = "product_rank", nullable = false)
    private int rank;

    @Column(name = "last_aggregated_at")
    private LocalDateTime lastAggregatedAt;

    public static ProductPopular create(Long productId, long salesCount, int rank) {
        return ProductPopular.builder()
            .productId(productId)
            .salesCount(salesCount)
            .rank(rank)
            .lastAggregatedAt(LocalDateTime.now())
            .build();
    }

    public boolean isTopN(int topN) {
        return this.rank > 0 && this.rank <= topN;
    }

    public boolean isStale(Duration threshold) {
        if (this.lastAggregatedAt == null) {
            return true;
        }
        return this.lastAggregatedAt.isBefore(LocalDateTime.now().minus(threshold));
    }

    public void increaseSalesCount(long salesCount) {
        this.salesCount += salesCount;
    }
}
