package com.example.ecommerce.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("이커머스 플랫폼 API")
                .version("1.0.0")
                .description("""
                    ## 이커머스 플랫폼 API 명세서

                    온라인 상품 판매 플랫폼의 API 문서입니다.

                    ### 주요 기능
                    - **상품 카탈로그**: 상품 조회, 재고 확인
                    - **장바구니**: 상품 담기, 수량 변경, 삭제
                    - **주문 및 결제**: 주문 생성, 잔액 기반 결제
                    - **선착순 쿠폰**: 쿠폰 발급 (1인 1매), 사용

                    ### 특징
                    - 재고 동시성 제어 (비관적 락/낙관적 락)
                    - 선착순 쿠폰 동시성 제어 (Redis 분산 락)
                    - 트랜잭션 무결성 보장
                    - 외부 데이터 플랫폼 연동 (비동기)

                    ### 기술 스택
                    - Spring Boot 3.x
                    - MySQL/PostgreSQL
                    - Redis
                    - Kafka/RabbitMQ
                    """)
                .contact(new Contact()
                    .name("E-commerce API Team")
                    .email("support@example.com")
                ))
            .servers(List.of(
                new Server()
                    .url("http://localhost:8080")
                    .description("로컬 개발 서버"),
                new Server()
                    .url("https://api.example.com")
                    .description("프로덕션 서버")
            ));
    }
}
