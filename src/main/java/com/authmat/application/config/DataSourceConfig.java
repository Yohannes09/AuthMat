package com.authmat.application.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Bean
    public DataSource dataSource(
            @Value("${DB_URL}")String jdbcUrl,
            @Value("${DB_USERNAME}")String dbUsername,
            @Value("${DB_PASSWORD}")String dbPassword,
            @Value("${DB_DRIVER}")String dbDriver,
            @Value("#{environment['DB_MAX_POOL_SIZE'] ?: 10}")int dbMaxPoolSize,
            @Value("#{environment['DB_MIN_IDLE_CONNECTIONS'] ?: 5}")int dbMinIdleConnections,
            @Value("#{environment['DB_IDLE_TIMEOUT_MS'] ?: 30000}")long dbIdleTimeoutMs,
            @Value("#{environment['DB_MAX_LIFETIME_MS'] ?: 1800000}")long dbMaxLifetimeMs){

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(dbUsername);
        config.setPassword(dbPassword);
        config.setDriverClassName(dbDriver);
        config.setMaximumPoolSize(dbMaxPoolSize);
        config.setMinimumIdle(dbMinIdleConnections);
        config.setIdleTimeout(dbIdleTimeoutMs);
        config.setMaxLifetime(dbMaxLifetimeMs);

        return new HikariDataSource(config);
    }

}