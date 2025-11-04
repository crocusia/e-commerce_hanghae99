package com.example.ecommerce.config;

import org.springframework.context.annotation.Configuration;
//import org.springframework.data.web.config.EnableSpringDataWebSupport;

@Configuration
//@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class WebConfig {
    // Pageable 파라미터 자동 처리 활성화 (JPA 의존성 제거로 주석 처리)
    // pageSerializationMode.VIA_DTO: Page 객체를 DTO로 직렬화
}
