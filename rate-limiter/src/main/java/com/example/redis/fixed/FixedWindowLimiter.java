package com.example.redis.fixed;

import redis.clients.jedis.Jedis;
import java.util.Collections;

public class FixedWindowLimiter {

    private final Jedis jedis;
    private final int maxReq;
    private final int windowTime;

    public FixedWindowLimiter(int maxReq, int windowTime) {
        this.jedis = new Jedis("localhost", 6379);
        this.maxReq = maxReq;
        this.windowTime = windowTime;
    }

    private static final String LUA_SCRIPT = """
        local current = redis.call('GET', KEYS[1])

        if not current then
            redis.call('SET', KEYS[1], 1)
            redis.call('EXPIRE', KEYS[1], ARGV[2])
            return 1
        end

        if tonumber(current) < tonumber(ARGV[1]) then
            return redis.call('INCR', KEYS[1])
        else
            return -1
        end
        """;

    public boolean allowedReq(String userId) {

        String key = "rate_limit:" + userId;

        Object result = jedis.eval(
                LUA_SCRIPT,
                Collections.singletonList(key),
                java.util.Arrays.asList(
                        String.valueOf(maxReq),
                        String.valueOf(windowTime)
                )
        );

        long response = (Long) result;  

        return response != -1;
    }
}