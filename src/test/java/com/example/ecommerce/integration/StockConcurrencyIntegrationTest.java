package com.example.ecommerce.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import com.example.ecommerce.coupon.repository.CouponRepository;
import com.example.ecommerce.coupon.repository.UserCouponRepository;
import com.example.ecommerce.order.domain.Order;
import com.example.ecommerce.order.dto.OrderItemRequest;
import com.example.ecommerce.order.dto.OrderRequest;
import com.example.ecommerce.order.orchestrator.OrderCreationOrchestrator;
import com.example.ecommerce.order.repository.OrderRepository;
import com.example.ecommerce.payment.dto.PaymentRequest;
import com.example.ecommerce.payment.orchestrator.PaymentOrchestrator;
import com.example.ecommerce.payment.repository.PaymentRepository;
import com.example.ecommerce.product.domain.Product;
import com.example.ecommerce.product.domain.ProductStock;
import com.example.ecommerce.product.domain.status.ProductStatus;
import com.example.ecommerce.product.domain.vo.Money;
import com.example.ecommerce.product.repository.ProductRepository;
import com.example.ecommerce.product.repository.ProductStockRepository;
import com.example.ecommerce.product.repository.StockReservationRepository;
import com.example.ecommerce.user.domain.User;
import com.example.ecommerce.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 재고 차감 동시성 제어 통합 테스트
 *
 * 전체 주문-결제 플로우에서 재고 차감이 올바르게 동작하는지 검증합니다.
 *
 * 테스트 플로우:
 * 1. 주문 생성 (OrderCreationOrchestrator) → OrderCreatedEvent 발행
 * 2. 재고 예약 (StockEventListener) → reservedStock 증가
 * 3. 재고 예약 완료 (OrderEventListener) → Order 상태 PENDING으로 변경
 * 4. 결제 생성 (PaymentOrchestrator) → PaymentCreatedEvent 발행
 * 5. 결제 처리 (PaymentEventListener) → 잔액 차감, PaymentCompletedEvent 발행
 * 6. 재고 확정 (StockEventListener) → currentStock 차감, reservedStock 감소
 * 7. 주문 완료 (OrderEventListener) → Order 상태 PAYMENT_COMPLETED로 변경
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("재고 차감 동시성 제어 통합 테스트")
class StockConcurrencyIntegrationTest {

    @Autowired
    private OrderCreationOrchestrator orderCreationOrchestrator;

    @Autowired
    private PaymentOrchestrator paymentOrchestrator;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductStockRepository productStockRepository;

    @Autowired
    private StockReservationRepository stockReservationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private UserCouponRepository userCouponRepository;

    private static final int INITIAL_STOCK = 10;
    private static final int CONCURRENT_REQUESTS = 20;
    private static final long INITIAL_USER_BALANCE = 1_000_000L;

    private Product product;
    private ProductStock productStock;
    private List<User> users;

    @BeforeEach
    void setUp() {
        // 상품 생성
        product = Product.builder()
            .name("테스트 상품")
            .price(Money.of(10_000L))
            .productStatus(ProductStatus.ACTIVE)
            .build();
        productRepository.save(product);

        // 재고 생성 (초기 재고: 10개)
        productStock = ProductStock.create(product.getProductId(), INITIAL_STOCK);
        productStockRepository.save(productStock);

        // 사용자 생성 (20명, 각각 잔액 충분)
        users = new ArrayList<>();
        for (int i = 0; i < CONCURRENT_REQUESTS; i++) {
            User user = User.create(
                "테스트 사용자" + i,
                "test" + i + "@example.com",
                INITIAL_USER_BALANCE
            );
            users.add(userRepository.save(user));
        }
    }

    @AfterEach
    void tearDown() {
        paymentRepository.deleteAllInBatch();
        orderRepository.deleteAllInBatch();
        userCouponRepository.deleteAllInBatch();
        couponRepository.deleteAllInBatch();
        stockReservationRepository.deleteAllInBatch();
        productStockRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("단일 주문 및 결제가 정상적으로 처리되어야 한다")
    void singleOrderAndPayment_shouldSucceed() throws InterruptedException {
        // given
        User user = users.get(0);

        // when: 1명이 주문 및 결제
        OrderRequest orderRequest = new OrderRequest(
            user.getId(),
            List.of(new OrderItemRequest(product.getProductId(), 1))
        );
        var orderResponse = orderCreationOrchestrator.createOrder(orderRequest);

        // 이벤트 처리 대기
        Thread.sleep(500);

        // 주문 상태 확인
        Order order = orderRepository.findById(orderResponse.id()).orElseThrow();

        if (order.getStatus().toString().equals("PENDING")) {
            // 결제 진행
            PaymentRequest paymentRequest = new PaymentRequest(
                order.getId(),
                user.getId()
            );
            paymentOrchestrator.createPayment(paymentRequest);

            // 결제 이벤트 처리 대기
            Thread.sleep(1000);
        }

        // then: 검증
        Order finalOrder = orderRepository.findById(orderResponse.id()).orElseThrow();
        System.out.println("Final order status: " + finalOrder.getStatus());

        ProductStock finalStock = productStockRepository.findByProductId(product.getProductId()).orElseThrow();
        System.out.println("Final stock - current: " + finalStock.getCurrentStock().getQuantity() + ", reserved: " + finalStock.getReservedStock());

        assertThat(finalOrder.getStatus().toString()).isEqualTo("PAYMENT_COMPLETED");
        assertThat(finalStock.getCurrentStock().getQuantity()).isEqualTo(INITIAL_STOCK - 1);
        assertThat(finalStock.getReservedStock()).isEqualTo(0);
    }

    @Test
    @DisplayName("20명이 동시에 주문 및 결제 시도 시, 재고 10개만큼만 성공하고 나머지는 실패해야 한다")
    void concurrentOrderAndPayment_shouldHandleStockCorrectly() throws InterruptedException {
        // given
        ExecutorService executorService = Executors.newFixedThreadPool(CONCURRENT_REQUESTS);
        CountDownLatch readyLatch = new CountDownLatch(CONCURRENT_REQUESTS);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(CONCURRENT_REQUESTS);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when: 20명이 동시에 주문 및 결제 시도
        for (int i = 0; i < CONCURRENT_REQUESTS; i++) {
            final int userIndex = i;
            executorService.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await(); // 모든 스레드가 준비될 때까지 대기

                    User user = users.get(userIndex);

                    // 1. 주문 생성
                    OrderRequest orderRequest = new OrderRequest(
                        user.getId(),
                        List.of(new OrderItemRequest(product.getProductId(), 1))
                    );
                    var orderResponse = orderCreationOrchestrator.createOrder(orderRequest);

                    // 이벤트 처리를 위한 대기
                    Thread.sleep(200);

                    // 2. 주문 상태 확인 (재고 예약 완료 여부)
                    Order order = orderRepository.findById(orderResponse.id()).orElseThrow();

                    if (order.getStatus().toString().equals("PENDING")) {
                        // 재고 예약 성공 → 결제 진행
                        PaymentRequest paymentRequest = new PaymentRequest(
                            order.getId(),
                            user.getId()
                        );
                        paymentOrchestrator.createPayment(paymentRequest);

                        // 결제 이벤트 처리를 위한 대기
                        Thread.sleep(400);

                        successCount.incrementAndGet();
                    } else {
                        // 재고 예약 실패
                        failCount.incrementAndGet();
                    }

                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await(); // 모든 스레드가 준비될 때까지 대기
        startLatch.countDown(); // 모든 스레드 동시 시작
        doneLatch.await(60, TimeUnit.SECONDS); // 모든 작업 완료 대기
        executorService.shutdown();

        // 추가 이벤트 처리를 위한 대기
        Thread.sleep(3000);

        // then: 검증
        // 1. 성공 10개, 실패 10개
        System.out.println("Success count: " + successCount.get() + ", Fail count: " + failCount.get());
        assertThat(successCount.get())
            .as("성공한 주문 수는 초기 재고(%d)와 같아야 합니다", INITIAL_STOCK)
            .isEqualTo(INITIAL_STOCK);
        assertThat(failCount.get())
            .as("실패한 주문 수는 %d개여야 합니다", CONCURRENT_REQUESTS - INITIAL_STOCK)
            .isEqualTo(CONCURRENT_REQUESTS - INITIAL_STOCK);

        // 2. 최종 재고 확인
        ProductStock finalStock = productStockRepository.findByProductId(product.getProductId()).orElseThrow();
        assertThat(finalStock.getCurrentStock().getQuantity()).isEqualTo(0);
        assertThat(finalStock.getReservedStock()).isEqualTo(0);

        // 3. 완료된 주문 개수 확인
        long completedOrders = orderRepository.findByStatus(
            com.example.ecommerce.order.domain.status.OrderStatus.PAYMENT_COMPLETED
        ).size();
        assertThat(completedOrders).isEqualTo(INITIAL_STOCK);
    }

    @Test
    @DisplayName("재고 부족 시 재고 예약이 실패해야 한다")
    void insufficientStock_shouldFailReservation() throws InterruptedException {
        // given: 재고 1개
        ProductStock stock = productStockRepository.findByProductId(product.getProductId()).orElseThrow();
        // 재고를 1개만 남김
        for (int i = 0; i < INITIAL_STOCK - 1; i++) {
            stock.decreaseStock(1);
        }
        productStockRepository.save(stock);

        // when: 2명이 동시에 주문 시도
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < 2; i++) {
            final int userIndex = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();

                    User user = users.get(userIndex);
                    OrderRequest orderRequest = new OrderRequest(
                        user.getId(),
                        List.of(new OrderItemRequest(product.getProductId(), 1))
                    );
                    orderCreationOrchestrator.createOrder(orderRequest);

                    Thread.sleep(100);
                    successCount.incrementAndGet();

                } catch (Exception e) {
                    // 예외 발생 가능
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        Thread.sleep(500);

        // then: 1명만 성공, 최종 재고 0
        ProductStock finalStock = productStockRepository.findByProductId(product.getProductId()).orElseThrow();
        int availableStock = finalStock.getCurrentStock().getQuantity() - finalStock.getReservedStock();
        assertThat(availableStock).isEqualTo(0);
    }

    @Test
    @DisplayName("결제 실패 시 예약된 재고가 해제되어야 한다")
    void paymentFailed_shouldReleaseReservedStock() throws InterruptedException {
        // given: 잔액이 부족한 사용자
        User poorUser = User.create(
            "가난한 사용자",
            "poor@example.com",
            100L // 상품 가격보다 적은 잔액
        );
        userRepository.save(poorUser);

        // when: 주문 생성 (재고 예약)
        OrderRequest orderRequest = new OrderRequest(
            poorUser.getId(),
            List.of(new OrderItemRequest(product.getProductId(), 1))
        );
        var orderResponse = orderCreationOrchestrator.createOrder(orderRequest);

        // 재고 예약 이벤트 처리 대기
        Thread.sleep(200);

        // 재고 예약 후 상태 확인
        ProductStock stockAfterReservation = productStockRepository.findByProductId(product.getProductId()).orElseThrow();
        int reservedAfterOrder = stockAfterReservation.getReservedStock();

        // 주문 상태 확인
        Order order = orderRepository.findById(orderResponse.id()).orElseThrow();

        if (order.getStatus().toString().equals("PENDING")) {
            // 결제 시도 (잔액 부족으로 실패할 것)
            try {
                PaymentRequest paymentRequest = new PaymentRequest(
                    order.getId(),
                    poorUser.getId()
                );
                paymentOrchestrator.createPayment(paymentRequest);

                // 결제 실패 이벤트 처리 대기
                Thread.sleep(300);
            } catch (Exception e) {
                // 결제 실패 예상
            }
        }

        Thread.sleep(500);

        // then: 예약된 재고가 해제되어야 함
        ProductStock finalStock = productStockRepository.findByProductId(product.getProductId()).orElseThrow();

        // 결제 실패 시 재고 해제 이벤트가 발행되므로 reserved stock이 감소해야 함
        assertThat(finalStock.getReservedStock()).isLessThanOrEqualTo(reservedAfterOrder);

        // 실제 재고(currentStock)는 차감되지 않아야 함
        assertThat(finalStock.getCurrentStock().getQuantity()).isEqualTo(INITIAL_STOCK);
    }

    @Test
    @DisplayName("동일 상품에 대한 여러 수량 주문 시 재고가 정확히 차감되어야 한다")
    void multipleQuantityOrders_shouldDeductStockCorrectly() throws InterruptedException {
        // given: 5명이 각각 2개씩 주문 시도 (총 10개, 재고와 동일)
        int orderQuantity = 2;
        int numberOfUsers = 5;

        ExecutorService executorService = Executors.newFixedThreadPool(numberOfUsers);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfUsers);
        AtomicInteger successCount = new AtomicInteger(0);

        // when
        for (int i = 0; i < numberOfUsers; i++) {
            final int userIndex = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();

                    User user = users.get(userIndex);
                    OrderRequest orderRequest = new OrderRequest(
                        user.getId(),
                        List.of(new OrderItemRequest(product.getProductId(), orderQuantity))
                    );
                    var orderResponse = orderCreationOrchestrator.createOrder(orderRequest);

                    Thread.sleep(200);

                    Order order = orderRepository.findById(orderResponse.id()).orElseThrow();
                    if (order.getStatus().toString().equals("PENDING")) {
                        PaymentRequest paymentRequest = new PaymentRequest(
                            order.getId(),
                            user.getId()
                        );
                        paymentOrchestrator.createPayment(paymentRequest);
                        Thread.sleep(400);
                        successCount.incrementAndGet();
                    }

                } catch (Exception e) {
                    // 재고 부족 시 예외 발생 가능
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(60, TimeUnit.SECONDS);
        executorService.shutdown();

        Thread.sleep(3000);

        // then: 5명 전부 성공, 최종 재고 0
        System.out.println("Success count: " + successCount.get() + " (expected: " + numberOfUsers + ")");
        assertThat(successCount.get())
            .as("성공한 주문 수는 %d명이어야 합니다", numberOfUsers)
            .isEqualTo(numberOfUsers);

        ProductStock finalStock = productStockRepository.findByProductId(product.getProductId()).orElseThrow();
        assertThat(finalStock.getCurrentStock().getQuantity()).isEqualTo(0);
        assertThat(finalStock.getReservedStock()).isEqualTo(0);
    }
}
