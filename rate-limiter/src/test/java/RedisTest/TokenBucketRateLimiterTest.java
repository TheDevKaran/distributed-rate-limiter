package RedisTest;

import com.example.redis.token.TokenBucketRateLimiter;
import org.junit.jupiter.api.*;
import redis.clients.jedis.Jedis;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class TokenBucketRateLimiterTest {

    TokenBucketRateLimiter limiter;
    Jedis jedis;

    @BeforeEach
    void setup() {
        limiter = new TokenBucketRateLimiter(5, 1.0, "localhost", 6379);
        jedis = new Jedis("localhost", 6379);
        jedis.flushDB();
    }

    @AfterEach
    void cleanup() {
        limiter.close();
        jedis.close();
    }

    // ─────────────────────────────────────────────
    // BASIC CORRECTNESS
    // ─────────────────────────────────────────────

    @Test
    void shouldAllowExactlyUpToCapacity() {
        for (int i = 0; i < 5; i++)
            assertTrue(limiter.allowRequests("dev"),
                    "Request " + (i + 1) + " should be allowed");

        assertFalse(limiter.allowRequests("dev"),
                "6th request must be blocked — bucket empty");
    }

    @Test
    void shouldBlockImmediatelyWhenCapacityIsZero() {
        TokenBucketRateLimiter zeroLimiter =
                new TokenBucketRateLimiter(0, 1.0, "localhost", 6379);
        assertFalse(zeroLimiter.allowRequests("dev"));
        zeroLimiter.close();
    }

    @Test
    void shouldAllowOneRequestIfCapacityIsOne() {
        TokenBucketRateLimiter oneLimiter =
                new TokenBucketRateLimiter(1, 1.0, "localhost", 6379);
        assertTrue(oneLimiter.allowRequests("dev"));
        assertFalse(oneLimiter.allowRequests("dev"));
        oneLimiter.close();
    }

    @Test
    void shouldRejectNegativeCapacity() {
        assertThrows(IllegalArgumentException.class, () ->
                new TokenBucketRateLimiter(-1, 1.0, "localhost", 6379));
    }

    @Test
    void shouldRejectNegativeRefillRate() {
        assertThrows(IllegalArgumentException.class, () ->
                new TokenBucketRateLimiter(5, -1.0, "localhost", 6379));
    }

    // ─────────────────────────────────────────────
    // REFILL LOGIC — THE CORE OF TOKEN BUCKET
    // ─────────────────────────────────────────────

    @Test
    void shouldRefillTokensOverTime() throws InterruptedException {
        // capacity=5, refillRate=1 token/sec
        for (int i = 0; i < 5; i++)
            limiter.allowRequests("dev");

        assertFalse(limiter.allowRequests("dev"), "Bucket empty");

        Thread.sleep(2000); // 2 tokens refilled

        assertTrue(limiter.allowRequests("dev"), "1st refilled token");
        assertTrue(limiter.allowRequests("dev"), "2nd refilled token");
        assertFalse(limiter.allowRequests("dev"), "3rd should block");
    }

    @Test
    void shouldNotExceedCapacityAfterLongIdle() throws InterruptedException {
        // drain bucket
        for (int i = 0; i < 5; i++)
            limiter.allowRequests("dev");

        Thread.sleep(30000); // way more than enough to refill

        // should have exactly capacity tokens, not more
        for (int i = 0; i < 5; i++)
            assertTrue(limiter.allowRequests("dev"),
                    "Req " + (i + 1) + " should be allowed");

        assertFalse(limiter.allowRequests("dev"),
                "6th must be blocked — cannot exceed capacity");
    }

    @Test
    void shouldAccumulateFractionalTokens() throws InterruptedException {
        // capacity=5, refillRate=0.5 tokens/sec
        TokenBucketRateLimiter fractLimiter =
                new TokenBucketRateLimiter(5, 0.5, "localhost", 6379);

        for (int i = 0; i < 5; i++)
            fractLimiter.allowRequests("dev");

        assertFalse(fractLimiter.allowRequests("dev"), "Empty");

        Thread.sleep(1000); // 0.5 tokens — not enough
        assertFalse(fractLimiter.allowRequests("dev"),
                "0.5 tokens not enough for 1 request");

        Thread.sleep(1000); // now 1.0 token accumulated
        assertTrue(fractLimiter.allowRequests("dev"),
                "1.0 token should allow 1 request");

        fractLimiter.close();
    }

    @Test
    void shouldNotGiveMassiveRefillAfterBlockedRequests()
            throws InterruptedException {

        // This tests the lastRefillTime update on block
        // If lastRefillTime is NOT updated on block,
        // user hammers for 10s while blocked,
        // then gets 10s worth of tokens all at once — wrong

        TokenBucketRateLimiter strictLimiter =
                new TokenBucketRateLimiter(3, 1.0, "localhost", 6379);

        // drain
        strictLimiter.allowRequests("dev");
        strictLimiter.allowRequests("dev");
        strictLimiter.allowRequests("dev");

        // hammer while blocked for 3 seconds
        long hammering = System.currentTimeMillis() + 3000;
        while (System.currentTimeMillis() < hammering)
            strictLimiter.allowRequests("dev"); // all blocked

        // now 3 tokens should have refilled (1/sec * 3sec)
        // NOT more — lastRefillTime must update on block too
        int allowed = 0;
        for (int i = 0; i < 10; i++)
            if (strictLimiter.allowRequests("dev")) allowed++;

        assertTrue(allowed <= 3,
                "Got " + allowed + " tokens after 3s — lastRefillTime not updated on block");

        strictLimiter.close();
    }

    @Test
    void shouldHandleZeroRefillRate() throws InterruptedException {
        // once empty, never refills
        TokenBucketRateLimiter noRefill =
                new TokenBucketRateLimiter(3, 0.0, "localhost", 6379);

        noRefill.allowRequests("dev");
        noRefill.allowRequests("dev");
        noRefill.allowRequests("dev");

        Thread.sleep(5000);

        assertFalse(noRefill.allowRequests("dev"),
                "Zero refill rate — should never recover");

        noRefill.close();
    }

    // ─────────────────────────────────────────────
    // BURST BEHAVIOR — TOKEN BUCKET'S ADVANTAGE
    // ─────────────────────────────────────────────

    @Test
    void shouldAllowFullBurstInstantly() {
        // all 5 tokens available at start — burst should be instant
        long start = System.currentTimeMillis();

        for (int i = 0; i < 5; i++)
            assertTrue(limiter.allowRequests("dev"),
                    "Burst req " + (i + 1) + " should be instant");

        long elapsed = System.currentTimeMillis() - start;
        assertTrue(elapsed < 1000,
                "Burst took " + elapsed + "ms — should be near instant");

        assertFalse(limiter.allowRequests("dev"), "6th blocked");
    }

    @Test
    void shouldAllowBurstThenRefillThenBurstAgain()
            throws InterruptedException {

        // drain
        for (int i = 0; i < 5; i++)
            limiter.allowRequests("dev");

        // wait for full refill (5 tokens at 1/sec = 5s)
        Thread.sleep(5500);

        // full burst again
        for (int i = 0; i < 5; i++)
            assertTrue(limiter.allowRequests("dev"),
                    "Second burst req " + (i + 1) + " should be allowed");

        assertFalse(limiter.allowRequests("dev"),
                "6th blocked after second burst");
    }

    // ─────────────────────────────────────────────
    // FLOAT DRIFT — THE SUBTLE KILLER
    // ─────────────────────────────────────────────

    @Test
    void shouldNotDriftAfterThousandsOfRefillCycles()
            throws InterruptedException {

        // refillRate=10 tokens/sec, capacity=1
        // drain and refill 1000 times
        // float drift accumulates — without rounding fix this breaks
        TokenBucketRateLimiter driftLimiter =
                new TokenBucketRateLimiter(1, 10.0, "localhost", 6379);

        int blocked = 0;
        int allowed = 0;

        for (int i = 0; i < 200; i++) {
            // drain
            if (driftLimiter.allowRequests("dev")) allowed++;
            else blocked++;

            Thread.sleep(100); // 10 tokens/sec * 0.1s = 1 token
        }

        System.out.println("Drift test — allowed: " + allowed
                + " blocked: " + blocked);

        // should allow close to 200 — drift would cause random blocks
        assertTrue(allowed >= 180,
                "Float drift caused " + blocked
                        + " unexpected blocks out of 200 attempts");

        driftLimiter.close();
    }

    // ─────────────────────────────────────────────
    // MULTI USER ISOLATION
    // ─────────────────────────────────────────────

    @Test
    void shouldIsolateDifferentUsers() {
        for (int i = 0; i < 5; i++)
            limiter.allowRequests("user1");

        assertFalse(limiter.allowRequests("user1"),
                "user1 should be blocked");

        for (int i = 0; i < 5; i++)
            assertTrue(limiter.allowRequests("user2"),
                    "user2 req " + (i + 1) + " should be allowed");

        assertFalse(limiter.allowRequests("user2"),
                "user2 6th should be blocked");
    }

    @Test
    void shouldHandle100UsersIndependently() {
        for (int u = 0; u < 100; u++) {
            String user = "user_" + u;
            for (int r = 0; r < 5; r++)
                assertTrue(limiter.allowRequests(user),
                        user + " req " + r + " should be allowed");
            assertFalse(limiter.allowRequests(user),
                    user + " 6th should be blocked");
        }
    }

    @Test
    void shouldNotMixTokensBetweenUsers() {
        // interleaved requests
        for (int i = 0; i < 5; i++) {
            limiter.allowRequests("user1");
            limiter.allowRequests("user2");
            limiter.allowRequests("user3");
        }

        assertFalse(limiter.allowRequests("user1"), "user1 blocked");
        assertFalse(limiter.allowRequests("user2"), "user2 blocked");
        assertFalse(limiter.allowRequests("user3"), "user3 blocked");
    }

    // ─────────────────────────────────────────────
    // DISTRIBUTED STATE
    // ─────────────────────────────────────────────

    @Test
    void shouldShareStateAcrossMultipleInstances() {
        TokenBucketRateLimiter server1 =
                new TokenBucketRateLimiter(5, 1.0, "localhost", 6379);
        TokenBucketRateLimiter server2 =
                new TokenBucketRateLimiter(5, 1.0, "localhost", 6379);

        assertTrue(server1.allowRequests("dev")); // 4 left
        assertTrue(server2.allowRequests("dev")); // 3 left
        assertTrue(server1.allowRequests("dev")); // 2 left
        assertTrue(server2.allowRequests("dev")); // 1 left
        assertTrue(server1.allowRequests("dev")); // 0 left

        assertFalse(server2.allowRequests("dev"),
                "6th request must be blocked across instances");

        server1.close();
        server2.close();
    }

    @Test
    void shouldEnforceLimitAcross10Servers() {
        List<TokenBucketRateLimiter> servers = new ArrayList<>();
        for (int i = 0; i < 10; i++)
            servers.add(new TokenBucketRateLimiter(5, 1.0, "localhost", 6379));

        int allowed = 0;
        for (int i = 0; i < 50; i++)
            if (servers.get(i % 10).allowRequests("dev")) allowed++;

        assertEquals(5, allowed,
                "Only 5 allowed across 10 servers, got " + allowed);

        servers.forEach(TokenBucketRateLimiter::close);
    }

    // ─────────────────────────────────────────────
    // CONCURRENCY — THE BRUTAL ONES
    // ─────────────────────────────────────────────

    @Test
    void shouldNeverExceedCapacityUnder500ConcurrentThreads()
            throws Exception {

        int capacity = 5;
        int totalThreads = 500;

        TokenBucketRateLimiter concurrentLimiter =
                new TokenBucketRateLimiter(capacity, 0.0, "localhost", 6379);

        AtomicInteger allowed = new AtomicInteger(0);
        runConcurrent(totalThreads, () -> {
            if (concurrentLimiter.allowRequests("dev"))
                allowed.incrementAndGet();
        });

        System.out.println("Concurrent allowed: " + allowed.get()
                + " / limit: " + capacity);

        assertEquals(capacity, allowed.get(),
                "Lua atomicity failed — allowed " + allowed.get()
                        + " instead of " + capacity);

        concurrentLimiter.close();
    }

    @Test
    void shouldHandleConcurrentRequestsForMultipleUsers()
            throws Exception {

        int usersCount = 10;
        int threadsPerUser = 50;
        int capacity = 5;

        TokenBucketRateLimiter concurrentLimiter =
                new TokenBucketRateLimiter(capacity, 0.0, "localhost", 6379);

        ConcurrentHashMap<String, AtomicInteger> allowedPerUser =
                new ConcurrentHashMap<>();
        for (int u = 0; u < usersCount; u++)
            allowedPerUser.put("user_" + u, new AtomicInteger(0));

        ExecutorService executor =
                Executors.newFixedThreadPool(usersCount * threadsPerUser);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch =
                new CountDownLatch(usersCount * threadsPerUser);

        for (int u = 0; u < usersCount; u++) {
            String user = "user_" + u;
            for (int t = 0; t < threadsPerUser; t++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        if (concurrentLimiter.allowRequests(user))
                            allowedPerUser.get(user).incrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }
        }

        startLatch.countDown();
        doneLatch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        for (int u = 0; u < usersCount; u++) {
            String user = "user_" + u;
            int count = allowedPerUser.get(user).get();
            assertEquals(capacity, count,
                    user + " allowed " + count + " instead of " + capacity);
        }

        concurrentLimiter.close();
    }

  @Test
void shouldNotAllowRefillRaceConditionUnderConcurrency()
        throws Exception {

    int capacity = 5;
    TokenBucketRateLimiter concurrentLimiter =
            new TokenBucketRateLimiter(capacity, 1.0, "localhost", 6379);

    // Phase 1 — drain
    for (int i = 0; i < capacity; i++)
        assertTrue(concurrentLimiter.allowRequests("dev"));

    System.out.println("Phase 1 done — drained");

    // Phase 2 — hammer immediately, 0 should pass
    AtomicInteger phase2 = new AtomicInteger(0);
    runConcurrent(500, () -> {
        if (concurrentLimiter.allowRequests("dev"))
            phase2.incrementAndGet();
    });
    System.out.println("Phase 2 allowed: " + phase2.get() + " (expected 0)");
    assertEquals(0, phase2.get(),
            "Phase 2: bucket empty, nothing should pass");

    // Phase 3 — wait 3s, ~3 tokens refilled
    Thread.sleep(3000);
    AtomicInteger phase3 = new AtomicInteger(0);
    runConcurrent(500, () -> {
        if (concurrentLimiter.allowRequests("dev"))
            phase3.incrementAndGet();
    });
    System.out.println("Phase 3 allowed: " + phase3.get() + " (expected ~3)");
    assertTrue(phase3.get() >= 2 && phase3.get() <= 4,
            "Phase 3: expected ~3 tokens, got " + phase3.get());

    // Phase 4 — wait 2s more, ~2 tokens refilled
    Thread.sleep(2000);
    AtomicInteger phase4 = new AtomicInteger(0);
    runConcurrent(500, () -> {
        if (concurrentLimiter.allowRequests("dev"))
            phase4.incrementAndGet();
    });
    System.out.println("Phase 4 allowed: " + phase4.get() + " (expected ~2)");
    assertTrue(phase4.get() >= 1 && phase4.get() <= 3,
            "Phase 4: expected ~2 tokens, got " + phase4.get());

    concurrentLimiter.close();
}

    // ─────────────────────────────────────────────
    // TTL AND CLEANUP
    // ─────────────────────────────────────────────

    @Test
    void shouldExpireRedisKeyAfterIdle() throws InterruptedException {
        // capacity=1, refillRate=1/sec → TTL = 1/1 + 60 = 61s
        // use tiny capacity and high rate to get small TTL for testing
        TokenBucketRateLimiter shortTTL =
                new TokenBucketRateLimiter(1, 100.0, "localhost", 6379);

        // TTL = ceil(1/100) + 60 = 61s — still too long for a test
        // just verify key exists after first request
        shortTTL.allowRequests("dev");

        assertTrue(jedis.exists("rate_limit:token:dev"),
                "Key should exist after first request");

        long ttl = jedis.ttl("rate_limit:token:dev");
        assertTrue(ttl > 0,
                "TTL should be set, got " + ttl);

        System.out.println("TTL set to: " + ttl + "s");

        shortTTL.close();
    }

    @Test
    void shouldClearUserCorrectly() {
        for (int i = 0; i < 5; i++)
            limiter.allowRequests("dev");

        assertFalse(limiter.allowRequests("dev"), "Blocked before clear");

        limiter.clearUser("dev");

        for (int i = 0; i < 5; i++)
            assertTrue(limiter.allowRequests("dev"),
                    "After clear, req " + (i + 1) + " should be allowed");

        assertFalse(limiter.allowRequests("dev"),
                "Should block again after exhausting cleared bucket");
    }

    @Test
    void shouldNotExistInRedisBeforeFirstRequest() {
        assertFalse(jedis.exists("rate_limit:token:dev"),
                "Key should not exist before any request");

        limiter.allowRequests("dev");

        assertTrue(jedis.exists("rate_limit:token:dev"),
                "Key should exist after first request");
    }

    // ─────────────────────────────────────────────
    // EDGE CASES THAT WILL HAUNT YOU
    // ─────────────────────────────────────────────

 @Test
void shouldHandleVeryHighCapacity() {
    TokenBucketRateLimiter bigLimiter =
            new TokenBucketRateLimiter(1000, 0.0, "localhost", 6379);

    for (int i = 0; i < 1000; i++)
        assertTrue(bigLimiter.allowRequests("dev"),
                "Request " + i + " should be allowed");

    assertFalse(bigLimiter.allowRequests("dev"),
            "1001st should be blocked");

    bigLimiter.close();
}

 @Test
void shouldRefillFasterWithHigherRefillRate() throws InterruptedException {
    TokenBucketRateLimiter fastRefill =
            new TokenBucketRateLimiter(5, 2.0, "localhost", 6379);

    for (int i = 0; i < 5; i++)
        fastRefill.allowRequests("dev");

    assertFalse(fastRefill.allowRequests("dev"), "Empty");

    Thread.sleep(1000); // 2 tokens/sec * 1s = 2 tokens

    assertTrue(fastRefill.allowRequests("dev"), "1st refilled");
    assertTrue(fastRefill.allowRequests("dev"), "2nd refilled");
    assertFalse(fastRefill.allowRequests("dev"), "3rd blocked");

    fastRefill.close();
}

    @Test
    void shouldHandleSingleTokenCapacityUnderConcurrency()
            throws Exception {

        // capacity=1 is the hardest case for atomicity
        // 100 threads fire at once — exactly 1 must pass
        TokenBucketRateLimiter singleToken =
                new TokenBucketRateLimiter(1, 0.0, "localhost", 6379);

        AtomicInteger allowed = new AtomicInteger(0);
        runConcurrent(100, () -> {
            if (singleToken.allowRequests("dev"))
                allowed.incrementAndGet();
        });

        assertEquals(1, allowed.get(),
                "Exactly 1 should pass with capacity=1, got " + allowed.get());

        singleToken.close();
    }

    // ─────────────────────────────────────────────
    // HELPER
    // ─────────────────────────────────────────────

    void runConcurrent(int threadCount, Runnable task)
            throws Exception {

        ExecutorService executor =
                Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch  = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    task.run();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(15, TimeUnit.SECONDS);
        executor.shutdown();
    }
}