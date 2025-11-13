package com.example.ecommerce.product.domain;

import java.time.Duration;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
public class ProductPopular {

    private final Long id;
    private final Long productId;
    private long salesCount;
    private final int rank;
    private LocalDateTime lastAggregatedAt;

    @Builder
    private ProductPopular(Long id, Long productId, long salesCount, int rank) {
        this.id = id;
        this.productId = productId;
        this.salesCount = salesCount;
        this.rank = rank;
        this.lastAggregatedAt = LocalDateTime.now();
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
