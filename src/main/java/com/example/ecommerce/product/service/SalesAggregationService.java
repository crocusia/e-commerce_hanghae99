package com.example.ecommerce.product.service;

import com.example.ecommerce.order.domain.Order;
import com.example.ecommerce.order.domain.OrderItem;
import com.example.ecommerce.order.domain.status.OrderStatus;
import com.example.ecommerce.order.repository.OrderRepository;
import com.example.ecommerce.product.domain.ProductPopular;
import com.example.ecommerce.product.repository.ProductPopularRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SalesAggregationService {

    private final OrderRepository orderRepository;
    private final ProductPopularRepository productPopularRepository;
    private final ProductSalesRedisService salesRedisService;
    private final CacheManager cacheManager;

    /**
     * 매일 새벽 3시 일괄 집계
     * 1. Redis 전체 누적 판매량 → DB ProductPopular 테이블 동기화
     * 2. 3일 랭킹 스냅샷 생성 (하루 1회 갱신)
     */
    @Scheduled(cron = "0 0 3 * * *") // 매일 새벽 3시 실행
    @Transactional
    public void aggregateSales() {
        log.info("판매 수 집계 시작 (Redis 기반)...");

        try {
            // 1. 전체 누적 판매량 집계 → DB 동기화
            Map<Long, Long> salesMap = salesRedisService.getAllSales();

            if (salesMap.isEmpty()) {
                log.warn("Redis 판매 데이터 없음, DB에서 재집계 시작");
                salesMap = calculateSalesCountFromDB();
                salesRedisService.syncFromDB(salesMap);
            }

            updateProductPopular(salesMap);
            evictPopularProductsCache();

            log.info("판매 수 집계 완료 - 총 상품 수: {}", salesMap.size());

            // 2. 3일 랭킹 스냅샷 생성 (하루 1회)
            boolean snapshotSuccess = salesRedisService.createRankingSnapshot();
            if (snapshotSuccess) {
                log.info("3일 랭킹 스냅샷 생성 완료 (일별 갱신)");
            } else {
                log.warn("3일 랭킹 스냅샷 생성 실패");
            }

        } catch (Exception e) {
            log.error("판매 수 집계 실패", e);
        }
    }

    @Transactional(readOnly = true)
    public Map<Long, Long> calculateSalesCountFromDB() {
        log.info("DB에서 판매 수 재집계 시작...");

        // 결제 완료된 주문만 조회
        List<Order> completedOrders = orderRepository.findByStatus(OrderStatus.PAYMENT_COMPLETED);

        Map<Long, Long> salesMap = new HashMap<>();

        for (Order order : completedOrders) {
            for (OrderItem orderItem : order.getOrderItems()) {
                Long productId = orderItem.getProductId();
                Integer quantity = orderItem.getQuantity();

                salesMap.merge(productId, quantity.longValue(), Long::sum);
            }
        }

        log.info("DB 재집계 완료 - 주문 수: {}, 상품 수: {}",
            completedOrders.size(), salesMap.size());

        return salesMap;
    }

    @Transactional
    public void updateProductPopular(Map<Long, Long> salesMap) {
        if (salesMap.isEmpty()) {
            log.info("No sales data to aggregate");
            return;
        }

        List<Map.Entry<Long, Long>> sortedSales = salesMap.entrySet().stream()
            .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
            .collect(Collectors.toList());

        productPopularRepository.deleteAllInBatch();

        AtomicInteger rank = new AtomicInteger(1);
        for (Map.Entry<Long, Long> entry : sortedSales) {
            ProductPopular popular = ProductPopular.create(
                entry.getKey(),
                entry.getValue(),
                rank.getAndIncrement()
            );
            productPopularRepository.save(popular);
        }

        log.info("Updated {} product popular records", sortedSales.size());
    }

    private void evictPopularProductsCache() {
        try {
            if (cacheManager.getCache("product:popular") != null) {
                cacheManager.getCache("product:popular").clear();
                log.info("인기 상품 캐시 무효화 완료");
            }
        } catch (Exception e) {
            log.error("캐시 무효화 실패", e);
        }
    }
}
