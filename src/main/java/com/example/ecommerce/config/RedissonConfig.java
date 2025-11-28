package com.example.ecommerce.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Redisson 설정 (프로덕션 환경 전용)
 * 테스트 환경에서는 TestContainersConfig에서 RedissonClient를 생성합니다.
 */
@Configuration
@Profile("!test")  // test 프로파일이 아닐 때만 활성화
public class RedissonConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
            .setAddress("redis://" + redisHost + ":" + redisPort)
            .setConnectionMinimumIdleSize(10)
            .setConnectionPoolSize(20)
            .setRetryAttempts(3)
            .setRetryInterval(1500)
            .setTimeout(3000)
            .setConnectTimeout(5000);

        return Redisson.create(config);
    }
}
