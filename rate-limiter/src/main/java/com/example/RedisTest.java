package com.example;

import redis.clients.jedis.Jedis;

public class RedisTest {

    public static void main(String[] args) {

        Jedis jedis = new Jedis("localhost", 6379);

//         jedis.set("name", "dev");

//         System.out.println(jedis.get("name"));
//         jedis.set("count", "0");

//         jedis.incr("count");

// System.out.println(jedis.get("count"));
//         jedis.set("temp", "hello");

// jedis.expire("temp", 10);

// System.out.println(jedis.get("temp"));

// jedis.set("count", "0");
// Object result = jedis.eval("return redis.call('INCR', 'count')");

// System.out.println(result);
// jedis.del("count");
Object result = jedis.eval(
    """
    local current = redis.call('GET', 'count')

    if not current then
        redis.call('SET', 'count', 1)
        return 1
    end

    if tonumber(current) < 5 then
        return redis.call('INCR', 'count')
    else
        return -1
    end
    """
);

System.out.println(result);
    }
}