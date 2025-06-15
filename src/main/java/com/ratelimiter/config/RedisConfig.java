package com.ratelimiter.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import redis.clients.jedis.JedisPoolConfig;

@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.timeout:2000}")
    private int timeout;

    @Value("${spring.data.redis.jedis.pool.max-active:50}")
    private int maxActive;

    @Value("${spring.data.redis.jedis.pool.max-idle:10}")
    private int maxIdle;

    @Value("${spring.data.redis.jedis.pool.min-idle:5}")
    private int minIdle;

    @Bean
    public JedisPoolConfig jedisPoolConfig() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(maxActive);
        poolConfig.setMaxIdle(maxIdle);
        poolConfig.setMinIdle(minIdle);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setBlockWhenExhausted(false);
        return poolConfig;
    }

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisHost);
        config.setPort(redisPort);

        JedisConnectionFactory factory = new JedisConnectionFactory(config);
        factory.setPoolConfig(jedisPoolConfig());
        factory.setTimeout(timeout);
        return factory;
    }

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }

    // Lua script for Token Bucket algorithm
    @Bean
    public DefaultRedisScript<Long> tokenBucketScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText("""
            local key = KEYS[1]
            local capacity = tonumber(ARGV[1])
            local tokens = tonumber(ARGV[2])
            local interval = tonumber(ARGV[3])
            local requested = tonumber(ARGV[4])
            local now = tonumber(ARGV[5])
            
            local bucket = redis.call('HMGET', key, 'tokens', 'last_refill')
            local current_tokens = tonumber(bucket[1])
            local last_refill = tonumber(bucket[2])
            
            if current_tokens == nil then
                current_tokens = capacity
                last_refill = now
            end
            
            -- Calculate tokens to add based on time elapsed
            local elapsed = math.max(0, now - last_refill)
            local tokens_to_add = math.floor(elapsed / interval * tokens)
            current_tokens = math.min(capacity, current_tokens + tokens_to_add)
            
            if current_tokens >= requested then
                current_tokens = current_tokens - requested
                redis.call('HMSET', key, 'tokens', current_tokens, 'last_refill', now)
                redis.call('EXPIRE', key, interval * 2)
                return {1, current_tokens}
            else
                redis.call('HMSET', key, 'tokens', current_tokens, 'last_refill', now)
                redis.call('EXPIRE', key, interval * 2)
                return {0, current_tokens}
            end
        """);
        script.setResultType(Long.class);
        return script;
    }

    // Lua script for Fixed Window algorithm
    @Bean
    public DefaultRedisScript<Long> fixedWindowScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText("""
            local key = KEYS[1]
            local limit = tonumber(ARGV[1])
            local window = tonumber(ARGV[2])
            local requested = tonumber(ARGV[3])
            
            local current = redis.call('GET', key)
            if current == false then
                current = 0
            else
                current = tonumber(current)
            end
            
            if current + requested <= limit then
                local new_count = redis.call('INCRBY', key, requested)
                if new_count == requested then
                    redis.call('EXPIRE', key, window)
                end
                return {1, limit - new_count}
            else
                return {0, limit - current}
            end
        """);
        script.setResultType(Long.class);
        return script;
    }
}