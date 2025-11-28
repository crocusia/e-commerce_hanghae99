package com.example.ecommerce.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis 캐시 설정
 * Spring Cache Abstraction을 사용하여 메서드 수준 캐싱 지원
 */
@Configuration
@EnableCaching
public class RedisCacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // ObjectMapper 설정
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 기본 캐시 설정
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())
            )
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new GenericJackson2JsonRedisSerializer(objectMapper)
                )
            )
            .entryTtl(Duration.ofMinutes(10)) // 기본 TTL 10분
            .disableCachingNullValues();

        // 캐시별 개별 설정
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // 1. 인기 상품 (30분) - 가장 긴 TTL, 배치로 갱신
        cacheConfigurations.put("product:popular",
            defaultConfig.entryTtl(Duration.ofMinutes(30)));

        // 2. 상품 상세 (10분) - 재고 정보 포함
        cacheConfigurations.put("product:detail",
            defaultConfig.entryTtl(Duration.ofMinutes(10)));

        // 3. 상품 목록 (5분) - 자주 변경되므로 짧은 TTL
        cacheConfigurations.put("product:list",
            defaultConfig.entryTtl(Duration.ofMinutes(5)));

        // 4. 쿠폰 목록 (20분)
        cacheConfigurations.put("coupon:list",
            defaultConfig.entryTtl(Duration.ofMinutes(20)));

        // 5. 개별 쿠폰 (20분)
        cacheConfigurations.put("coupon",
            defaultConfig.entryTtl(Duration.ofMinutes(20)));

        // 6. 사용자 쿠폰 목록 (10분)
        cacheConfigurations.put("user:coupons",
            defaultConfig.entryTtl(Duration.ofMinutes(10)));

        // 7. 사용자 정보 (15분)
        cacheConfigurations.put("user:info",
            defaultConfig.entryTtl(Duration.ofMinutes(15)));

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .transactionAware() // 트랜잭션과 캐시 동기화
            .build();
    }
}
