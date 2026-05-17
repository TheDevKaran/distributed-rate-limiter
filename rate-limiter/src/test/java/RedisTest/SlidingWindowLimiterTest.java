// package RedisTest;

// import com.example.limiter.redis.sliding.SlidingWindowLimiter;
// import redis.clients.jedis.Jedis;

// import static org.junit.jupiter.api.Assertions.assertEquals;
// import static org.junit.jupiter.api.Assertions.assertFalse;
// import static org.junit.jupiter.api.Assertions.assertTrue;

// import java.util.*;
// import java.util.concurrent.*;
// import java.util.concurrent.atomic.AtomicInteger;

// import org.junit.jupiter.api.AfterEach;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;

// public class SlidingWindowLimiterTest {

//     SlidingWindowLimiter limiter;
//     Jedis jedis;

//     @BeforeEach
//     void setup() {
//         limiter = new SlidingWindowLimiter(5, 5, "localhost", 6379);
//         jedis = new Jedis("localhost", 6379);
//         jedis.flushDB();
//     }

//     @AfterEach
//     void cleanup() {
//         limiter.close();
//         jedis.close();
//     }

//     // ─────────────────────────────────────────────
//     // BASIC CORRECTNESS
//     // ─────────────────────────────────────────────

//     @Test
//     void shouldAllowExactlyUpToLimit() {
//         for (int i = 0; i < 5; i++)
//             assertTrue(limiter.allowedReq("dev"), "Request " + (i + 1) + " should be allowed");

//         assertFalse(limiter.allowedReq("dev"), "6th request must be blocked");
//     }

//     @Test
//     void shouldBlockImmediatelyWhenLimitIsZero() {
//         SlidingWindowLimiter zeroLimiter =
//                 new SlidingWindowLimiter(0, 5, "localhost", 6379);
//         assertFalse(zeroLimiter.allowedReq("dev"));
//         zeroLimiter.close();
//     }

//     @Test
//     void shouldAllowOneRequestIfLimitIsOne() {
//         SlidingWindowLimiter oneLimiter =
//                 new SlidingWindowLimiter(1, 5, "localhost", 6379);
//         assertTrue(oneLimiter.allowedReq("dev"));
//         assertFalse(oneLimiter.allowedReq("dev"));
//         oneLimiter.close();
//     }

//     @Test
//     void shouldHandleRapidFireRequests() {
//         // 100 requests fired as fast as possible
//         // only 5 should get through — tests same-second duplicate handling
//         int allowed = 0;
//         for (int i = 0; i < 100; i++)
//             if (limiter.allowedReq("dev")) allowed++;

//         assertEquals(5, allowed,
//                 "Same-second burst allowed " + allowed + " instead of 5 — duplicate member bug");
//     }

//     // ─────────────────────────────────────────────
//     // SLIDING WINDOW CORE BEHAVIOR
//     // this is what separates it from fixed window
//     // ─────────────────────────────────────────────

//     @Test
//     void shouldSlideWindowAndAllowAfterOldestExpires()
//             throws InterruptedException {

//         // 5 requests at t=0
//         for (int i = 0; i < 5; i++)
//             assertTrue(limiter.allowedReq("dev"));

//         assertFalse(limiter.allowedReq("dev"), "Should block at limit");

//         // wait 3s — oldest requests still in window (window=5s)
//         Thread.sleep(3000);
//         assertFalse(limiter.allowedReq("dev"), "Should still block, window not slid enough");

//         // wait 3 more seconds — now t=6s, oldest (t=0) has expired
//         Thread.sleep(3000);
//         assertTrue(limiter.allowedReq("dev"), "Should allow after oldest expires");
//     }

//     @Test
//     void shouldNeverAllowMoreThanLimitInAnySlidingWindow()
//             throws InterruptedException {

//         // This is the key test — fixed window fails this, sliding must pass
//         // Send 3 requests, wait 4s, send 3 more
//         // At t=4s: window covers t=-1 to t=4 → first 3 still inside → only 2 more allowed

//         SlidingWindowLimiter limiter2 =
//                 new SlidingWindowLimiter(3, 5, "localhost", 6379);

//         assertTrue(limiter2.allowedReq("user")); // t=0, count=1
//         assertTrue(limiter2.allowedReq("user")); // t=0, count=2
//         assertTrue(limiter2.allowedReq("user")); // t=0, count=3

//         Thread.sleep(4000); // t=4s

//         // window still has 3 from t=0 (they expire at t=5)
//         // only 0 slots left
//         assertFalse(limiter2.allowedReq("user"), "Window still has 3 requests, must block");
//         assertFalse(limiter2.allowedReq("user"), "Must block");

//         Thread.sleep(2000); // t=6s — t=0 requests expired

//         assertTrue(limiter2.allowedReq("user"), "Now expired, should allow");

//         limiter2.close();
//     }

//     @Test
//     void shouldNotResetLikeFixedWindow() throws InterruptedException {
//         // Fixed window resets entire count at boundary
//         // Sliding window never does — it remembers recent requests

//         SlidingWindowLimiter limiter3 =
//                 new SlidingWindowLimiter(3, 10, "localhost", 6379);

//         // requests at t=0
//         limiter3.allowedReq("user");
//         limiter3.allowedReq("user");
//         limiter3.allowedReq("user");

//         Thread.sleep(6000); // t=6s

//         // sliding looks back 10s — all 3 still inside
//         // fixed window would have reset here if window was 5s
//         assertFalse(limiter3.allowedReq("user"),
//                 "Sliding must remember t=0 requests — fixed window would allow here");

//         limiter3.close();
//     }

//     // ─────────────────────────────────────────────
//     // WINDOW RESET — FULL CYCLE
//     // ─────────────────────────────────────────────

//     @Test
//     void shouldGiveFullQuotaAfterFullWindowExpires()
//             throws InterruptedException {

//         for (int i = 0; i < 5; i++)
//             limiter.allowedReq("dev");

//         assertFalse(limiter.allowedReq("dev"));

//         Thread.sleep(6000); // full window expired

//         for (int i = 0; i < 5; i++)
//             assertTrue(limiter.allowedReq("dev"), "Req " + (i + 1) + " should be allowed after reset");

//         assertFalse(limiter.allowedReq("dev"), "Should block again after new window exhausted");
//     }

//     @Test
//     void shouldHandleMultipleFullCycles() throws InterruptedException {
//         for (int cycle = 0; cycle < 3; cycle++) {
//             for (int i = 0; i < 5; i++)
//                 assertTrue(limiter.allowedReq("dev"),
//                         "Cycle " + cycle + " req " + i + " should be allowed");
//             assertFalse(limiter.allowedReq("dev"),
//                     "Cycle " + cycle + " 6th should be blocked");
//             Thread.sleep(6000);
//         }
//     }

//     // ─────────────────────────────────────────────
//     // MULTI USER ISOLATION
//     // ─────────────────────────────────────────────

//     @Test
//     void shouldIsolateDifferentUsers() {
//         for (int i = 0; i < 5; i++)
//             limiter.allowedReq("user1");

//         assertFalse(limiter.allowedReq("user1"));

//         for (int i = 0; i < 5; i++)
//             assertTrue(limiter.allowedReq("user2"),
//                     "user2 req " + (i + 1) + " should be allowed");

//         assertFalse(limiter.allowedReq("user2"));
//     }

//     @Test
//     void shouldNotMixCountersBetweenUsers() {
//         for (int i = 0; i < 5; i++) {
//             limiter.allowedReq("user1");
//             limiter.allowedReq("user2");
//             limiter.allowedReq("user3");
//         }

//         assertFalse(limiter.allowedReq("user1"), "user1 should be blocked");
//         assertFalse(limiter.allowedReq("user2"), "user2 should be blocked");
//         assertFalse(limiter.allowedReq("user3"), "user3 should be blocked");
//     }

//     @Test
//     void shouldHandle100UsersIndependently() {
//         for (int u = 0; u < 100; u++) {
//             String user = "user_" + u;
//             for (int r = 0; r < 5; r++)
//                 assertTrue(limiter.allowedReq(user),
//                         user + " req " + r + " should be allowed");
//             assertFalse(limiter.allowedReq(user),
//                     user + " 6th req should be blocked");
//         }
//     }

//     // ─────────────────────────────────────────────
//     // DISTRIBUTED STATE
//     // ─────────────────────────────────────────────

//     @Test
//     void shouldShareStateAcrossMultipleInstances() {
//         SlidingWindowLimiter server1 =
//                 new SlidingWindowLimiter(5, 5, "localhost", 6379);
//         SlidingWindowLimiter server2 =
//                 new SlidingWindowLimiter(5, 5, "localhost", 6379);

//         assertTrue(server1.allowedReq("dev")); // 1
//         assertTrue(server2.allowedReq("dev")); // 2
//         assertTrue(server1.allowedReq("dev")); // 3
//         assertTrue(server2.allowedReq("dev")); // 4
//         assertTrue(server1.allowedReq("dev")); // 5

//         assertFalse(server2.allowedReq("dev"),
//                 "6th request must be blocked across instances");

//         server1.close();
//         server2.close();
//     }

//     @Test
//     void shouldEnforceLimitAcross10Servers() {
//         List<SlidingWindowLimiter> servers = new ArrayList<>();
//         for (int i = 0; i < 10; i++)
//             servers.add(new SlidingWindowLimiter(5, 10, "localhost", 6379));

//         int allowed = 0;
//         for (int i = 0; i < 50; i++) {
//             if (servers.get(i % 10).allowedReq("dev")) allowed++;
//         }

//         assertEquals(5, allowed,
//                 "Only 5 allowed across 10 servers, got " + allowed);

//         servers.forEach(SlidingWindowLimiter::close);
//     }

//     // ─────────────────────────────────────────────
//     // CONCURRENCY — THE BRUTAL ONES
//     // ─────────────────────────────────────────────

//     @Test
//     void shouldNeverExceedLimitUnder500ConcurrentThreads()
//             throws Exception {

//         int limit = 5;
//         int totalThreads = 500;

//         SlidingWindowLimiter concurrentLimiter =
//                 new SlidingWindowLimiter(limit, 10, "localhost", 6379);

//         ExecutorService executor =
//                 Executors.newFixedThreadPool(totalThreads);
//         CountDownLatch startLatch = new CountDownLatch(1);
//         CountDownLatch doneLatch = new CountDownLatch(totalThreads);
//         AtomicInteger allowed = new AtomicInteger(0);

//         for (int i = 0; i < totalThreads; i++) {
//             executor.submit(() -> {
//                 try {
//                     startLatch.await();
//                     if (concurrentLimiter.allowedReq("dev"))
//                         allowed.incrementAndGet();
//                 } catch (InterruptedException e) {
//                     Thread.currentThread().interrupt();
//                 } finally {
//                     doneLatch.countDown();
//                 }
//             });
//         }

//         startLatch.countDown(); // all 500 fire at once
//         doneLatch.await(15, TimeUnit.SECONDS);
//         executor.shutdown();

//         System.out.println("Allowed: " + allowed.get()
//                 + " / " + totalThreads + " threads / limit: " + limit);

//         assertEquals(limit, allowed.get(),
//                 "Lua atomicity failed — allowed " + allowed.get()
//                         + " instead of " + limit);

//         concurrentLimiter.close();
//     }

//     @Test
//     void shouldHandleConcurrentRequestsForMultipleUsers()
//             throws Exception {

//         int usersCount = 10;
//         int threadsPerUser = 50;
//         int limit = 5;

//         SlidingWindowLimiter concurrentLimiter =
//                 new SlidingWindowLimiter(limit, 10, "localhost", 6379);

//         ExecutorService executor =
//                 Executors.newFixedThreadPool(usersCount * threadsPerUser);

//         CountDownLatch startLatch = new CountDownLatch(1);
//         CountDownLatch doneLatch =
//                 new CountDownLatch(usersCount * threadsPerUser);

//         ConcurrentHashMap<String, AtomicInteger> allowedPerUser =
//                 new ConcurrentHashMap<>();
//         for (int u = 0; u < usersCount; u++)
//             allowedPerUser.put("user_" + u, new AtomicInteger(0));

//         for (int u = 0; u < usersCount; u++) {
//             String user = "user_" + u;
//             for (int t = 0; t < threadsPerUser; t++) {
//                 executor.submit(() -> {
//                     try {
//                         startLatch.await();
//                         if (concurrentLimiter.allowedReq(user))
//                             allowedPerUser.get(user).incrementAndGet();
//                     } catch (InterruptedException e) {
//                         Thread.currentThread().interrupt();
//                     } finally {
//                         doneLatch.countDown();
//                     }
//                 });
//             }
//         }

//         startLatch.countDown();
//         doneLatch.await(15, TimeUnit.SECONDS);
//         executor.shutdown();

//         for (int u = 0; u < usersCount; u++) {
//             String user = "user_" + u;
//             int count = allowedPerUser.get(user).get();
//             assertEquals(limit, count,
//                     user + " allowed " + count + " instead of " + limit);
//         }

//         concurrentLimiter.close();
//     }

//     @Test
//     void shouldMaintainAccuracyUnderConcurrentSlidingWindow()
//             throws Exception {

//         // Hardest test:
//         // Phase 1: 5 threads allowed at t=0
//         // wait 4s
//         // Phase 2: 500 threads fire — only 0 should get through (window still active)
//         // wait 2s more (t=6s, window expired)
//         // Phase 3: 500 threads fire — only 5 should get through

//         int limit = 5;
//         SlidingWindowLimiter concurrentLimiter =
//                 new SlidingWindowLimiter(limit, 5, "localhost", 6379);

//         // Phase 1
//         for (int i = 0; i < limit; i++)
//             assertTrue(concurrentLimiter.allowedReq("dev"));

//         System.out.println("Phase 1 done — bucket exhausted");

//         // Phase 2 — window still active, nothing should pass
//         Thread.sleep(4000);

//         AtomicInteger phase2Allowed = new AtomicInteger(0);
//         runConcurrent(500, () -> {
//             if (concurrentLimiter.allowedReq("dev"))
//                 phase2Allowed.incrementAndGet();
//         });

//         System.out.println("Phase 2 allowed: " + phase2Allowed.get()
//                 + " (expected 0)");
//         assertEquals(0, phase2Allowed.get(),
//                 "Phase 2: window still active, nothing should pass");

//         // Phase 3 — window expired, exactly 5 should pass
//         Thread.sleep(2000);

//         AtomicInteger phase3Allowed = new AtomicInteger(0);
//         runConcurrent(500, () -> {
//             if (concurrentLimiter.allowedReq("dev"))
//                 phase3Allowed.incrementAndGet();
//         });

//         System.out.println("Phase 3 allowed: " + phase3Allowed.get()
//                 + " (expected 5)");
//         assertEquals(limit, phase3Allowed.get(),
//                 "Phase 3: exactly " + limit + " should pass after window expires");

//         concurrentLimiter.close();
//     }

//     // ─────────────────────────────────────────────
//     // REDIS KEY CLEANUP
//     // ─────────────────────────────────────────────

//     @Test
//     void shouldCleanupRedisKeysAfterExpiry() throws InterruptedException {
//         for (int i = 0; i < 5; i++)
//             limiter.allowedReq("dev");

//         Thread.sleep(7000); // window=5s, expire=6s

//         // keys should be gone from Redis
//         assertFalse(jedis.exists("rate_limit:sliding:dev"),
//                 "Main key should have expired");
//         assertFalse(jedis.exists("rate_limit:sliding:dev:seq"),
//                 "Seq key should have expired");
//     }

//     @Test
//     void shouldClearUserCorrectly() {
//         for (int i = 0; i < 5; i++)
//             limiter.allowedReq("dev");

//         assertFalse(limiter.allowedReq("dev"));

//         limiter.clearUser("dev");

//         // after clear, full quota restored
//         for (int i = 0; i < 5; i++)
//             assertTrue(limiter.allowedReq("dev"),
//                     "After clear, req " + (i + 1) + " should be allowed");
//     }

//     // ─────────────────────────────────────────────
//     // EDGE CASES
//     // ─────────────────────────────────────────────

//     @Test
//     void shouldHandleVeryHighLimit() {
//         SlidingWindowLimiter bigLimiter =
//                 new SlidingWindowLimiter(10000, 60, "localhost", 6379);

//         for (int i = 0; i < 10000; i++)
//             assertTrue(bigLimiter.allowedReq("dev"),
//                     "Request " + i + " should be allowed");

//         assertFalse(bigLimiter.allowedReq("dev"),
//                 "10001st should be blocked");

//         bigLimiter.close();
//     }

//     @Test
//     void shouldRecoverAfterLongIdleTime() throws InterruptedException {
//         for (int i = 0; i < 5; i++)
//             limiter.allowedReq("dev");

//         Thread.sleep(30000); // 30s idle — way past window

//         for (int i = 0; i < 5; i++)
//             assertTrue(limiter.allowedReq("dev"),
//                     "After long idle, req " + (i + 1) + " should be allowed");
//     }

//     // ─────────────────────────────────────────────
//     // HELPER
//     // ─────────────────────────────────────────────

//     void runConcurrent(int threadCount, Runnable task)
//             throws Exception {

//         ExecutorService executor =
//                 Executors.newFixedThreadPool(threadCount);
//         CountDownLatch startLatch = new CountDownLatch(1);
//         CountDownLatch doneLatch = new CountDownLatch(threadCount);

//         for (int i = 0; i < threadCount; i++) {
//             executor.submit(() -> {
//                 try {
//                     startLatch.await();
//                     task.run();
//                 } catch (InterruptedException e) {
//                     Thread.currentThread().interrupt();
//                 } finally {
//                     doneLatch.countDown();
//                 }
//             });
//         }

//         startLatch.countDown();
//         doneLatch.await(15, TimeUnit.SECONDS);
//         executor.shutdown();
//     }
// }