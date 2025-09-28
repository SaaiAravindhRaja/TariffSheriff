package com.tariffsheriff.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.net.URI;

/**
 * Redis configuration for token blacklisting and session management.
 * Configures Redis connection and RedisTemplate for authentication system.
 */
@Configuration
@EnableRedisRepositories
public class RedisConfig {

    @Value("${spring.data.redis.url}")
    private String redisUrl;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    /**
     * Configure Redis connection factory.
     * Supports both local Redis and cloud Redis services.
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        
        try {
            URI redisUri = URI.create(redisUrl);
            config.setHostName(redisUri.getHost());
            config.setPort(redisUri.getPort());
            
            // Set password if provided
            if (redisPassword != null && !redisPassword.trim().isEmpty()) {
                config.setPassword(redisPassword);
            }
            
            // Extract password from URL if present
            String userInfo = redisUri.getUserInfo();
            if (userInfo != null && userInfo.contains(":")) {
                String[] credentials = userInfo.split(":");
                if (credentials.length > 1) {
                    config.setPassword(credentials[1]);
                }
            }
        } catch (Exception e) {
            // Fallback to localhost if URL parsing fails
            config.setHostName("localhost");
            config.setPort(6379);
        }

        return new JedisConnectionFactory(config);
    }

    /**
     * Configure RedisTemplate for object serialization.
     * Uses String serializer for keys and JSON serializer for values.
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Use String serializer for keys
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // Use JSON serializer for values
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        
        template.afterPropertiesSet();
        return template;
    }
}