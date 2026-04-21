package com.authmat.application.config;

import com.authmat.application.authorization.dto.RoleDto;
import com.authmat.application.user.dto.UserDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * <p><strong>Lessons learned:</strong></p>
 *
 * <p>
 * {@code activateDefaultTyping()} is a security risk and can expose the application
 * to deserialization attacks. When enabled, it embeds the fully-qualified Java
 * class name into the serialized JSON payload.
 * </p>
 *
 * <p>
 * This approach should be avoided. Instead, serialize only known and trusted
 * types explicitly.
 * </p>
 *
 * <p><strong>Example of what NOT to do:</strong></p>
 *
 * <pre>
 * {@code
 * @Bean
 * public ObjectMapper objectMapper() {
 *     ObjectMapper objectMapper = new ObjectMapper();
 *     objectMapper.registerModule(new JavaTimeModule());
 *     objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
 *
 *     objectMapper.activateDefaultTyping(
 *         LaissezFaireSubTypeValidator.instance,
 *         ObjectMapper.DefaultTyping.NON_FINAL,
 *         JsonTypeInfo.As.PROPERTY
 *     );
 *
 *     return objectMapper;
 * }
 * }
 * </pre>
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisConnectionFactory redisConnectionFactory(
            @Value("${spring.redis.host}") String hostName,
            @Value("${spring.redis.port:6379}") int port) {
        return new LettuceConnectionFactory(
                new RedisStandaloneConfiguration(hostName, port)
        );
    }

    @Bean(name = "strRedisTemplate")
    public RedisTemplate<String, String> strRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();

        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();

        return template;
    }

    @Bean(name = "userRedisTemplate")
    public RedisTemplate<String, UserDto> userRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, UserDto> template = new RedisTemplate<>();

        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();

        return template;
    }

    @Bean(name = "roleRedisTemplate")
    public RedisTemplate<String, RoleDto> roleRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, RoleDto> template = new RedisTemplate<>();

        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();

        return template;
    }

    @Bean
    public ObjectMapper objectMapper(){
        ObjectMapper objectMapper = new ObjectMapper();

        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return objectMapper;
    }

}
