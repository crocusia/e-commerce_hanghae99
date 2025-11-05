package com.example.ecommerce.config;

import com.example.ecommerce.product.domain.Product;
import com.example.ecommerce.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 애플리케이션 시작 시 초기 데이터를 설정하는 컴포넌트
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final ProductRepository productRepository;

    @Override
    public void run(String... args) {
        log.info("초기 데이터 설정 시작...");

        // 샘플 상품 데이터 추가
        createProduct("맥북 프로 16인치", 3_500_000L, 50);
        createProduct("아이패드 프로 12.9인치", 1_500_000L, 100);
        createProduct("에어팟 프로 2세대", 350_000L, 200);
        createProduct("애플 워치 울트라", 1_150_000L, 75);
        createProduct("맥 미니 M2", 850_000L, 30);

        log.info("초기 데이터 설정 완료!");
    }

    private void createProduct(String name, long price, int stock) {
        Product product = Product.create(name, price, stock);
        productRepository.save(product);
        log.info("상품 생성 완료: {} (가격: {}원, 재고: {}개)", name, price, stock);
    }
}
