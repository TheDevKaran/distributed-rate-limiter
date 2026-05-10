package com.example.redis.fixed;

import com.example.inmemory.fixed.FixedWindowLimiter;

import redis.clients.jedis.Jedis;

public class FixedWindowTest {

    public static void main(String[] args)
            throws InterruptedException {

        testSingleUserSingleServer();

        testWindowReset();

        testMultiUserSingleServer();

        testSingleUserMultiServer();
    }

    // --------------------------------------------------

    static void testSingleUserSingleServer() {

        System.out.println("\n===== 1 USER | 1 SERVER =====");

        clearKey("dev");

        FixedWindowLimiter limiter =
                new FixedWindowLimiter(5, 10);

        for(int i = 1; i <= 6; i++) {

            boolean allowed =
                    limiter.allowedReq("dev");

            System.out.println(
                    "Request " + i +
                    " -> " + allowed
            );
        }
    }

    // --------------------------------------------------

    static void testWindowReset()
            throws InterruptedException {

        System.out.println("\n===== WINDOW RESET =====");

        clearKey("dev");

        FixedWindowLimiter limiter =
                new FixedWindowLimiter(5, 5);

        for(int i = 1; i <= 6; i++) {

            System.out.println(
                    "Request " + i +
                    " -> " +
                    limiter.allowedReq("dev")
            );
        }

        System.out.println("\nWaiting for window reset...\n");

        Thread.sleep(6000);

        System.out.println(
                "After reset -> " +
                limiter.allowedReq("dev")
        );
    }

    // --------------------------------------------------

    static void testMultiUserSingleServer() {

        System.out.println("\n===== N USERS | 1 SERVER =====");

        clearKey("dev");
        clearKey("alex");

        FixedWindowLimiter limiter =
                new FixedWindowLimiter(3, 10);

        System.out.println("\nDEV:");

        for(int i = 1; i <= 4; i++) {

            System.out.println(
                    "Request " + i +
                    " -> " +
                    limiter.allowedReq("dev")
            );
        }

        System.out.println("\nALEX:");

        for(int i = 1; i <= 4; i++) {

            System.out.println(
                    "Request " + i +
                    " -> " +
                    limiter.allowedReq("alex")
            );
        }
    }

    // --------------------------------------------------

    static void testSingleUserMultiServer() {

        System.out.println("\n===== 1 USER | N SERVERS =====");

        clearKey("dev");

        FixedWindowLimiter server1 =
                new FixedWindowLimiter(5, 10);

        FixedWindowLimiter server2 =
                new FixedWindowLimiter(5, 10);

        for(int i = 1; i <= 3; i++) {

            System.out.println(
                    "Server1 -> " +
                    server1.allowedReq("dev")
            );

            System.out.println(
                    "Server2 -> " +
                    server2.allowedReq("dev")
            );
        }
    }

    // --------------------------------------------------

    static void clearKey(String userId) {

        Jedis jedis =
                new Jedis("localhost", 6379);

        jedis.del("rate_limit:" + userId);

        jedis.close();
    }
}