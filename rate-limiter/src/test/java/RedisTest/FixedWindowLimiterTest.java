package RedisTest;

import com.example.redis.fixed.FixedWindowLimiter;
import org.junit.jupiter.api.*;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class FixedWindowLimiterTest {

    FixedWindowLimiter limiter;
    Jedis jedis;

    @BeforeEach
    void setup() {
        limiter = new FixedWindowLimiter(5, 5, "localhost", 6379);
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
    void shouldAllowExactlyUpToLimit() {
        for (int i = 0; i < 5; i++)
            assertTrue(limiter.allowedReq("dev").isAllowed(), "Request " + (i + 1) + " should be allowed");

        assertFalse(limiter.allowedReq("dev").isAllowed(), "6th request must be blocked");
    }

    @Test
    void shouldBlockImmediatelyWhenLimitIsZero() {
        FixedWindowLimiter zeroLimiter = new FixedWindowLimiter(0, 5, "localhost", 6379);
        assertFalse(zeroLimiter.allowedReq("dev").isAllowed());
        zeroLimiter.close();
    }

    @Test
    void shouldAllowOneRequestIfLimitIsOne() {
        FixedWindowLimiter oneLimiter = new FixedWindowLimiter(1, 5, "localhost", 6379);
        assertTrue(oneLimiter.allowedReq("dev").isAllowed());
        assertFalse(oneLimiter.allowedReq("dev").isAllowed());
        oneLimiter.close();
    }

    // ─────────────────────────────────────────────
    // WINDOW RESET
    // ─────────────────────────────────────────────

    @Test
    void shouldResetAfterWindowExpires() throws InterruptedException {
        for (int i = 0; i < 5; i++)
            limiter.allowedReq("dev");

        assertFalse(limiter.allowedReq("dev").isAllowed(), "Should be blocked before reset");

        Thread.sleep(6000); // window is 5s, wait 6s

        assertTrue(limiter.allowedReq("dev").isAllowed(), "Should allow after window expires");
    }

    @Test
    void shouldNotResetBeforeWindowExpires() throws InterruptedException {
        for (int i = 0; i < 5; i++)
            limiter.allowedReq("dev");

        Thread.sleep(3000); // only half the window passed

        assertFalse(limiter.allowedReq("dev").isAllowed(), "Should still be blocked mid-window");
    }

    @Test
    void shouldGiveFullQuotaAfterReset() throws InterruptedException {
        for (int i = 0; i < 5; i++)
            limiter.allowedReq("dev");

        Thread.sleep(6000);

        // full quota restored — not just 1
        for (int i = 0; i < 5; i++)
            assertTrue(limiter.allowedReq("dev").isAllowed(), "Request " + (i + 1) + " should be allowed after reset");

        assertFalse(limiter.allowedReq("dev").isAllowed(), "Should block again after new window exhausted");
    }

    // ─────────────────────────────────────────────
    // MULTI USER ISOLATION
    // ─────────────────────────────────────────────

    @Test
    void shouldIsolateDifferentUsers() {
        for (int i = 0; i < 5; i++)
            limiter.allowedReq("user1");

        assertFalse(limiter.allowedReq("user1").isAllowed());

        // user2 completely unaffected
        for (int i = 0; i < 5; i++)
            assertTrue(limiter.allowedReq("user2").isAllowed(), "user2 request " + (i + 1) + " should be allowed");

        assertFalse(limiter.allowedReq("user2").isAllowed());
    }

    @Test
    void shouldNotMixCountersBetweenUsers() {
        // interleaved requests across 3 users
        for (int i = 0; i < 5; i++) {
            limiter.allowedReq("user1");
            limiter.allowedReq("user2");
            limiter.allowedReq("user3");
        }

        assertFalse(limiter.allowedReq("user1").isAllowed(), "user1 should be blocked");
        assertFalse(limiter.allowedReq("user2").isAllowed(), "user2 should be blocked");
        assertFalse(limiter.allowedReq("user3").isAllowed(), "user3 should be blocked");
    }

    @Test
    void shouldHandle100UsersIndependently() {
        // 100 users each making 5 requests — none should bleed into others
        for (int u = 0; u < 100; u++) {
            String user = "user_" + u;
            for (int r = 0; r < 5; r++)
                assertTrue(limiter.allowedReq(user).isAllowed(), user + " req " + r + " should be allowed");
            assertFalse(limiter.allowedReq(user).isAllowed(), user + " 6th req should be blocked");
        }
    }

    // ─────────────────────────────────────────────
    // DISTRIBUTED STATE (CORE VALUE OF REDIS)
    // ─────────────────────────────────────────────

    @Test
    void shouldShareStateAcrossMultipleInstances() {
        FixedWindowLimiter server1 = new FixedWindowLimiter(5, 5, "localhost", 6379);
        FixedWindowLimiter server2 = new FixedWindowLimiter(5, 5, "localhost", 6379);

        assertTrue(server1.allowedReq("dev").isAllowed()); // 1
        assertTrue(server2.allowedReq("dev").isAllowed()); // 2
        assertTrue(server1.allowedReq("dev").isAllowed()); // 3
        assertTrue(server2.allowedReq("dev").isAllowed()); // 4
        assertTrue(server1.allowedReq("dev").isAllowed()); // 5

        // 6th request — regardless of which server — must be blocked
        assertFalse(server2.allowedReq("dev").isAllowed(), "6th request must be blocked across servers");

        server1.close();
        server2.close();
    }

    @Test
    void shouldEnforceLimitAcross10Servers() {
        // simulates 10 app servers all sharing one Redis
        List<FixedWindowLimiter> servers = new ArrayList<>();
        for (int i = 0; i < 10; i++)
            servers.add(new FixedWindowLimiter(5, 5, "localhost", 6379));

        int allowed = 0;
        for (int i = 0; i < 20; i++) {
            FixedWindowLimiter server = servers.get(i % 10);
            if (server.allowedReq("dev").isAllowed()) allowed++;
        }

        assertEquals(5, allowed, "Only 5 requests should be allowed across all servers");

        servers.forEach(FixedWindowLimiter::close);
    }

    // ─────────────────────────────────────────────
    // CONCURRENCY — THIS IS THE BRUTAL ONE
    // ─────────────────────────────────────────────

    @Test
    void shouldNeverExceedLimitUnderHighConcurrency() throws Exception {
        int totalThreads = 500;
        int limit = 5;
        FixedWindowLimiter concurrentLimiter =
                new FixedWindowLimiter(limit, 10, "localhost", 6379);

        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch startLatch = new CountDownLatch(1); // all threads fire at once
        CountDownLatch doneLatch = new CountDownLatch(totalThreads);
        AtomicInteger allowed = new AtomicInteger(0);

        for (int i = 0; i < totalThreads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // hold all threads until gun fires
                    if (concurrentLimiter.allowedReq("dev").isAllowed())
                        allowed.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // fire
        doneLatch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        System.out.println("Allowed: " + allowed.get() + " / 500 threads / limit: " + limit);

        assertEquals(limit, allowed.get(),
                "Lua script atomicity failed — allowed " + allowed.get() + " instead of " + limit);

        concurrentLimiter.close();
    }

    @Test
    void shouldHandleConcurrentRequestsForMultipleUsers() throws Exception {
        int usersCount = 10;
        int threadsPerUser = 50;
        int limit = 5;

        FixedWindowLimiter concurrentLimiter =
                new FixedWindowLimiter(limit, 10, "localhost", 6379);

        ExecutorService executor =
                Executors.newFixedThreadPool(usersCount * threadsPerUser);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(usersCount * threadsPerUser);

        // track allowed count per user
        ConcurrentHashMap<String, AtomicInteger> allowedPerUser = new ConcurrentHashMap<>();
        for (int u = 0; u < usersCount; u++)
            allowedPerUser.put("user_" + u, new AtomicInteger(0));

        for (int u = 0; u < usersCount; u++) {
            String user = "user_" + u;
            for (int t = 0; t < threadsPerUser; t++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        if (concurrentLimiter.allowedReq(user).isAllowed())
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

        // every user must have exactly 'limit' allowed — no more, no less
        for (int u = 0; u < usersCount; u++) {
            String user = "user_" + u;
            int count = allowedPerUser.get(user).get();
            assertEquals(limit, count,
                    user + " had " + count + " allowed, expected exactly " + limit);
        }

        concurrentLimiter.close();
    }

    // ─────────────────────────────────────────────
    // EDGE CASES
    // ─────────────────────────────────────────────

    @Test
    void shouldHandleVeryHighLimit() {
        FixedWindowLimiter bigLimiter = new FixedWindowLimiter(10000, 60, "localhost", 6379);
        for (int i = 0; i < 10000; i++)
            assertTrue(bigLimiter.allowedReq("dev").isAllowed(), "Request " + i + " should be allowed");
        assertFalse(bigLimiter.allowedReq("dev").isAllowed(), "10001st should be blocked");
        bigLimiter.close();
    }

    @Test
    void shouldHandleRapidSuccessiveRequests() {
        // all at same millisecond effectively — tests same-timestamp behavior
        int allowed = 0;
        for (int i = 0; i < 100; i++)
            if (limiter.allowedReq("dev").isAllowed()) allowed++;

        assertEquals(5, allowed, "Only 5 should be allowed regardless of speed");
    }

    @Test
    void shouldRecoverCorrectlyAfterMultipleWindows() throws InterruptedException {
        // exhaust, wait, exhaust, wait, exhaust — 3 full cycles
        for (int cycle = 0; cycle < 3; cycle++) {
            for (int i = 0; i < 5; i++)
                assertTrue(limiter.allowedReq("dev").isAllowed(), "Cycle " + cycle + " req " + i + " should be allowed");
            assertFalse(limiter.allowedReq("dev").isAllowed(), "Cycle " + cycle + " 6th should be blocked");
            Thread.sleep(6000);
        }
    }
}