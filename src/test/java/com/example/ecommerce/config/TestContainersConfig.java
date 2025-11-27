package com.example.ecommerce.config;

import com.redis.testcontainers.RedisContainer;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestContainersConfig {

    @Bean
    @ServiceConnection
    public RedisContainer redisContainer() {
        RedisContainer redis = new RedisContainer(
            DockerImageName.parse("redis:7-alpine")
        );
        redis.start();
        return redis;
    }

    @Bean
    public RedissonClient redissonClient(RedisContainer redisContainer) {
        Config config = new Config();
        config.useSingleServer()
            .setAddress("redis://" + redisContainer.getHost() + ":" + redisContainer.getFirstMappedPort())
            .setConnectionMinimumIdleSize(10)
            .setConnectionPoolSize(20)
            .setRetryAttempts(3)
            .setRetryInterval(1500)
            .setTimeout(3000)
            .setConnectTimeout(5000);

        return Redisson.create(config);
    }

    @Bean
    public MySQLContainer<?> mysqlContainer() {
        MySQLContainer<?> mysql = new MySQLContainer<>(
            DockerImageName.parse("mysql:8.0")
        )
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");
        mysql.start();
        return mysql;
    }

    @Bean
    @Primary
    public DataSource dataSource(MySQLContainer<?> mysqlContainer) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(mysqlContainer.getJdbcUrl());
        hikariConfig.setUsername(mysqlContainer.getUsername());
        hikariConfig.setPassword(mysqlContainer.getPassword());
        hikariConfig.setDriverClassName(mysqlContainer.getDriverClassName());

        // 커넥션 풀 설정: 20개 스레드 * 2 (주문 트랜잭션 + 이벤트 리스너) + 여유분
        hikariConfig.setMaximumPoolSize(50);
        hikariConfig.setMinimumIdle(10);
        hikariConfig.setConnectionTimeout(60000);
        hikariConfig.setIdleTimeout(600000);
        hikariConfig.setMaxLifetime(1800000);

        return new HikariDataSource(hikariConfig);
    }
}
