package com.example.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

@Configuration
public class RedisConfig {

    @Value("${redis.host}")
    private String redisHost;

    @Value("${redis.port}")
    private int redisPort;

    @Bean
    public JedisPool jedisPool() {

        JedisPoolConfig config = new JedisPoolConfig();

        config.setMaxTotal(50);
        config.setMaxIdle(10);
        config.setMinIdle(2);
        config.setTestOnBorrow(true);

        return new JedisPool(config, redisHost, redisPort);
    }
}