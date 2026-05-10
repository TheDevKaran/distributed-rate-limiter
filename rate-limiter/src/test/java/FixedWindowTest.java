
//     import com.example.fixed.FixedWindowLimiter;
//     import org.junit.jupiter.api.Test;

//     import java.util.function.LongSupplier;

//     import static org.junit.jupiter.api.Assertions.*;

//     public class FixedWindowTest {

//         static long fakeTime = 0;
//         static final LongSupplier clock = () -> fakeTime;

//         // ─────────────────────────────────────────────
//         // BASIC BEHAVIOR
//         // ─────────────────────────────────────────────

//         @Test
//         void shouldAllowRequestsUpToLimit() {
//             FixedWindowLimiter limiter = new FixedWindowLimiter(3, 10000, clock);
//             String user = "dev";

//             fakeTime = 0;

//             assertTrue(limiter.allowedReq(user));
//             assertTrue(limiter.allowedReq(user));
//             assertTrue(limiter.allowedReq(user));
//         }

//         @Test
//         void shouldBlockAfterLimitReached() {
//             FixedWindowLimiter limiter = new FixedWindowLimiter(3, 10000, clock);
//             String user = "dev";

//             fakeTime = 0;

//             limiter.allowedReq(user);
//             limiter.allowedReq(user);
//             limiter.allowedReq(user);

//             assertFalse(limiter.allowedReq(user));
//         }

//         // ─────────────────────────────────────────────
//         // WINDOW BEHAVIOR
//         // ─────────────────────────────────────────────

//         @Test
//         void shouldResetAfterWindowExpires() {
//             FixedWindowLimiter limiter = new FixedWindowLimiter(3, 10000, clock);
//             String user = "dev";

//             fakeTime = 0;

//             limiter.allowedReq(user);
//             limiter.allowedReq(user);
//             limiter.allowedReq(user);

//             fakeTime = 10001;

//             assertTrue(limiter.allowedReq(user));
//         }

// @Test
// void shouldNotExceedLimitUnderConcurrentRequests() throws InterruptedException {
//     FixedWindowLimiter limiter = new FixedWindowLimiter(10, 10000, clock);
//     String user = "dev";
//     fakeTime = 0;

//     java.util.concurrent.atomic.AtomicInteger allowed = new java.util.concurrent.atomic.AtomicInteger(0);
//     Thread[] threads = new Thread[100];

//     for (int i = 0; i < 100; i++) {
//         threads[i] = new Thread(() -> {
//             if (limiter.allowedReq(user)) allowed.incrementAndGet();
//         });
//     }

//     for (Thread t : threads) t.start();
//     for (Thread t : threads) t.join();

//     assertTrue(allowed.get() <= 10, "Allowed " + allowed.get() + " requests, expected max 10");
// }

//         // ─────────────────────────────────────────────
//         // BURST EDGE CASE (MOST IMPORTANT)
//         // ─────────────────────────────────────────────

//         @Test
//         void shouldAllowBurstAcrossBoundary() {
//             FixedWindowLimiter limiter = new FixedWindowLimiter(3, 10000, clock);
//             String user = "dev";

//             fakeTime = 0;
//             limiter.allowedReq(user);

//             fakeTime = 9000;
//             limiter.allowedReq(user);
//             limiter.allowedReq(user);

//             fakeTime = 10001;

//             assertTrue(limiter.allowedReq(user));
//             assertTrue(limiter.allowedReq(user));
//             assertTrue(limiter.allowedReq(user));
//         }

//         // ─────────────────────────────────────────────
//         // MULTI-USER ISOLATION
//         // ─────────────────────────────────────────────

//         @Test
//         void shouldHandleMultipleUsersIndependently() {
//             FixedWindowLimiter limiter = new FixedWindowLimiter(3, 10000, clock);

//             fakeTime = 0;

//             assertTrue(limiter.allowedReq("user1"));
//             assertTrue(limiter.allowedReq("user1"));
//             assertTrue(limiter.allowedReq("user1"));
//             assertFalse(limiter.allowedReq("user1"));

//             // user2 should not be affected
//             assertTrue(limiter.allowedReq("user2"));
//             assertTrue(limiter.allowedReq("user2"));
//             assertTrue(limiter.allowedReq("user2"));
//         }

//         // ─────────────────────────────────────────────
//         // RAPID REQUESTS (SAME TIMESTAMP)
//         // ─────────────────────────────────────────────

//         @Test
//         void shouldHandleMultipleRequestsAtSameTime() {
//             FixedWindowLimiter limiter = new FixedWindowLimiter(3, 10000, clock);
//             String user = "dev";

//             fakeTime = 0;

//             assertTrue(limiter.allowedReq(user));
//             assertTrue(limiter.allowedReq(user));
//             assertTrue(limiter.allowedReq(user));
//             assertFalse(limiter.allowedReq(user));
//         }

//         // ─────────────────────────────────────────────
//         // LONG IDLE TIME
//         // ─────────────────────────────────────────────

//         @Test
//         void shouldRecoverAfterLongIdleTime() {
//             FixedWindowLimiter limiter = new FixedWindowLimiter(3, 10000, clock);
//             String user = "dev";

//             fakeTime = 0;

//             limiter.allowedReq(user);
//             limiter.allowedReq(user);
//             limiter.allowedReq(user);

//             // long gap
//             fakeTime = 100000;

//             assertTrue(limiter.allowedReq(user));
//             assertTrue(limiter.allowedReq(user));
//         }

//         // ─────────────────────────────────────────────
//         // EDGE CASE: ZERO CAPACITY
//         // ─────────────────────────────────────────────

//         @Test
//         void shouldBlockAllIfCapacityZero() {
//             FixedWindowLimiter limiter = new FixedWindowLimiter(0, 10000, clock);
//             String user = "dev";

//             fakeTime = 0;

//             assertFalse(limiter.allowedReq(user));
//             assertFalse(limiter.allowedReq(user));
//         }

//         // ─────────────────────────────────────────────
//         // EDGE CASE: VERY SMALL WINDOW
//         // ─────────────────────────────────────────────

//         @Test
//         void shouldWorkWithSmallWindow() {
//             FixedWindowLimiter limiter = new FixedWindowLimiter(2, 1, clock);
//             String user = "dev";

//             fakeTime = 0;

//             assertTrue(limiter.allowedReq(user));
//             assertTrue(limiter.allowedReq(user));
//             assertFalse(limiter.allowedReq(user));

//             fakeTime = 2;

//             assertTrue(limiter.allowedReq(user));
//         }
//     }