package com.example.ecommerce.product.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSalesRedisService {

    private static final String SALES_KEY = "product:sales";
    private static final String RANK_KEY = "product:rank";

    private final RedisTemplate<String, String> redisTemplate;

    public void incrementSales(Long productId, int quantity) {
        try {
            Double newScore = redisTemplate.opsForZSet()
                .incrementScore(SALES_KEY, productId.toString(), quantity);

            log.debug("상품 판매 수 증가 - productId: {}, quantity: {}, totalSales: {}",
                productId, quantity, newScore);

        } catch (Exception e) {
            log.error("Redis 판매 수 증가 실패 - productId: {}, quantity: {}",
                productId, quantity, e);
        }
    }

    public Map<Long, Long> getTopNProducts(int limit) {
        try {
            Set<ZSetOperations.TypedTuple<String>> topProducts = redisTemplate.opsForZSet()
                .reverseRangeWithScores(SALES_KEY, 0, limit - 1);

            if (topProducts == null || topProducts.isEmpty()) {
                log.debug("Redis에서 인기 상품 조회 결과 없음");
                return new LinkedHashMap<>();
            }

            Map<Long, Long> result = new LinkedHashMap<>();
            for (ZSetOperations.TypedTuple<String> tuple : topProducts) {
                Long productId = Long.parseLong(tuple.getValue());
                Long salesCount = tuple.getScore().longValue();
                result.put(productId, salesCount);
            }

            log.debug("Top {} 인기 상품 조회 완료 - count: {}", limit, result.size());
            return result;

        } catch (Exception e) {
            log.error("Redis에서 인기 상품 조회 실패 - limit: {}", limit, e);
            return new LinkedHashMap<>();
        }
    }

    public long getSalesCount(Long productId) {
        try {
            Double score = redisTemplate.opsForZSet().score(SALES_KEY, productId.toString());
            return score != null ? score.longValue() : 0L;

        } catch (Exception e) {
            log.error("Redis 판매 수 조회 실패 - productId: {}", productId, e);
            return 0L;
        }
    }

    public int getRank(Long productId) {
        try {
            Long rank = redisTemplate.opsForZSet()
                .reverseRank(SALES_KEY, productId.toString());

            // rank는 0-based이므로 1을 더함
            return rank != null ? rank.intValue() + 1 : -1;

        } catch (Exception e) {
            log.error("Redis 순위 조회 실패 - productId: {}", productId, e);
            return -1;
        }
    }

    public Map<Long, Long> getAllSales() {
        try {
            Set<ZSetOperations.TypedTuple<String>> allProducts = redisTemplate.opsForZSet()
                .reverseRangeWithScores(SALES_KEY, 0, -1); // -1 = 전체 조회

            if (allProducts == null || allProducts.isEmpty()) {
                return new LinkedHashMap<>();
            }

            Map<Long, Long> result = new LinkedHashMap<>();
            for (ZSetOperations.TypedTuple<String> tuple : allProducts) {
                Long productId = Long.parseLong(tuple.getValue());
                Long salesCount = tuple.getScore().longValue();
                result.put(productId, salesCount);
            }

            log.debug("전체 판매 수 조회 완료 - 상품 수: {}", result.size());
            return result;

        } catch (Exception e) {
            log.error("Redis 전체 판매 수 조회 실패", e);
            return new LinkedHashMap<>();
        }
    }

    public void resetSales(Long productId) {
        try {
            redisTemplate.opsForZSet().remove(SALES_KEY, productId.toString());
            log.debug("상품 판매 수 초기화 - productId: {}", productId);

        } catch (Exception e) {
            log.error("Redis 판매 수 초기화 실패 - productId: {}", productId, e);
        }
    }

    public long resetAllSales() {
        try {
            Long removed = redisTemplate.delete(SALES_KEY) ?
                redisTemplate.opsForZSet().size(SALES_KEY) : 0L;

            log.info("전체 판매 수 데이터 초기화 완료 - 삭제된 항목: {}", removed);
            return removed != null ? removed : 0L;

        } catch (Exception e) {
            log.error("Redis 전체 판매 수 초기화 실패", e);
            return 0L;
        }
    }

    /**
     * Redis에서 DB로 판매 수 동기화
     *
     * @param salesMap DB에서 집계한 판매 수
     */
    public void syncFromDB(Map<Long, Long> salesMap) {
        if (salesMap.isEmpty()) {
            log.info("동기화할 판매 데이터 없음");
            return;
        }

        try {
            // 기존 데이터 삭제
            redisTemplate.delete(SALES_KEY);

            // 새로운 데이터 추가
            for (Map.Entry<Long, Long> entry : salesMap.entrySet()) {
                redisTemplate.opsForZSet()
                    .add(SALES_KEY, entry.getKey().toString(), entry.getValue());
            }

            log.info("DB → Redis 판매 수 동기화 완료 - 상품 수: {}", salesMap.size());

        } catch (Exception e) {
            log.error("DB → Redis 동기화 실패", e);
        }
    }
}
