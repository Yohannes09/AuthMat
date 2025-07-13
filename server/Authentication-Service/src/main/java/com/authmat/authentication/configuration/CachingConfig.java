package com.authmat.authentication.configuration;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

/**
 * Configuration class for setting up Redis-based caching in the authentication service.
 * <p>
 * This configures a RedisCacheManager with custom TTLs and a custom serializer that supports
 * Java 8 date/time types (e.g., LocalDateTime) using the Jackson JSR310 module.
 * <p>
 * The serializer is essential to avoid errors related to unsupported date/time serialization,
 * and must be consistent across cache read/write operations to prevent deserialization issues.
 * <p>
 * Ensure Redis data is flushed or compatible when making changes to cached model classes.
 */
@Configuration
@EnableCaching
public class CachingConfig {

    /**
     * Configures the RedisCacheManager with default TTL and per-cache TTL overrides.
     * Uses a custom serializer to properly handle LocalDateTime and other complex types.
     *
     * @param redisConnectionFactory the factory for creating Redis connections
     * @return a configured RedisCacheManager
     */
    @Bean
    public RedisCacheManager redisCacheManager(
            RedisConnectionFactory redisConnectionFactory,
            RedisSerializer<Object> redisSerializer
    ){
        RedisCacheWriter redisCacheWriter = RedisCacheWriter
                .nonLockingRedisCacheWriter(redisConnectionFactory);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .prefixCacheNameWith("authentication:")
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(redisSerializer)
                );

        Map<String, RedisCacheConfiguration> perCacheConfig = Map.of(
                "role", defaultConfig.entryTtl(Duration.ofMinutes(60)),
                "user", defaultConfig.entryTtl(Duration.ofMinutes(60)),
                "token", defaultConfig.entryTtl(Duration.ofMinutes(60))
        );

        return RedisCacheManager.builder(redisCacheWriter)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(perCacheConfig)
                .build();
    }

    /**
     * Creates a RedisConnectionFactory for connecting to a standalone Redis instance.
     *
     * @return RedisConnectionFactory
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(
                "10.0.0.222", 6379
        );
        return new LettuceConnectionFactory(config);
    }

    /**
     * Builds and returns a custom Redis serializer using Jackson that supports advanced
     * Java types (like {@link java.time.LocalDateTime}) and enables type information
     * for polymorphic deserialization.
     * <p>
     * <b>Key Features:</b>
     * <ul>
     *     <li>Registers {@code JavaTimeModule} to properly handle Java 8 date/time types.</li>
     *     <li>Disables timestamp-based date serialization for cleaner JSON output.</li>
     *     <li>Enables default typing using {@code activateDefaultTyping}, which includes type metadata
     *         (e.g., {@code @class}) in the JSON payload. This is critical when deserializing to
     *         non-final classes or generic {@code Object} fields, especially in cache scenarios.</li>
     * </ul>
     *
     * <b>Why it's important:</b>
     * <p>
     * Redis cache may store data in raw JSON form, and when retrieving it, Spring attempts to
     * deserialize it back to the original type. Without type metadata, deserialization of generic
     * objects (e.g., {@code Object} or {@code Map}) can fail or produce {@code LinkedHashMap} instead
     * of the intended domain class.
     *
     * @return a {@link GenericJackson2JsonRedisSerializer} with a custom-configured {@link com.fasterxml.jackson.databind.ObjectMapper}
     */
    @Bean
    public RedisSerializer<Object> redisSerializer(ObjectMapper objectMapper){
        return new GenericJackson2JsonRedisSerializer(objectMapper);
    }

    @Bean
    public ObjectMapper objectMapper(){
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        return objectMapper;
    }


    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer(objectMapper()));
        template.afterPropertiesSet();

        return template;
    }

}