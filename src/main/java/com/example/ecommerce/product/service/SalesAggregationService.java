package com.example.ecommerce.product.service;

import com.example.ecommerce.order.domain.Order;
import com.example.ecommerce.order.domain.OrderItem;
import com.example.ecommerce.order.domain.status.OrderStatus;
import com.example.ecommerce.order.repository.OrderRepository;
import com.example.ecommerce.product.domain.ProductPopular;
import com.example.ecommerce.product.repository.ProductPopularRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Scheduled(cron = "0 0 3 * * *") // 매일 새벽 3시 실행
    @Transactional
    public void aggregateSales() {
        log.info("Starting sales aggregation...");

        try {
            Map<Long, Long> salesMap = calculateSalesCount();
            updateProductPopular(salesMap);
            log.info("Sales aggregation completed. Total products: {}", salesMap.size());
        } catch (Exception e) {
            log.error("Failed to aggregate sales", e);
        }
    }

    @Transactional(readOnly = true)
    public Map<Long, Long> calculateSalesCount() {
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

        return salesMap;
    }

    @Transactional
    public void updateProductPopular(Map<Long, Long> salesMap) {
        if (salesMap.isEmpty()) {
            log.info("No sales data to aggregate");
            return;
        }

        // 판매량 기준으로 정렬하고 랭킹 부여
        List<Map.Entry<Long, Long>> sortedSales = salesMap.entrySet().stream()
            .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
            .collect(Collectors.toList());

        // 기존 데이터 모두 삭제
        productPopularRepository.deleteAllInBatch();

        // 새로운 데이터 저장
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
}
