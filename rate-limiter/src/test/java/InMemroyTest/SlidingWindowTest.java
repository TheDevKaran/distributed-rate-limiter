package InMemroyTest;

import org.junit.jupiter.api.Test;

import com.example.limiter.inmemory.sliding.SlidingWindow;

import java.util.function.LongSupplier;

import static org.junit.jupiter.api.Assertions.*;

public class SlidingWindowTest {

    static long fakeTime = 0;
    static final LongSupplier clock = () -> fakeTime;

    // ─────────────────────────────────────────────
    // BASIC BEHAVIOR
    // ─────────────────────────────────────────────

    @Test
    void shouldAllowRequestsUpToLimit() {
        SlidingWindow limiter = new SlidingWindow(3, 10000L, clock);
        String user = "dev";

        fakeTime = 0;

        assertTrue(limiter.allowedRequests(user));
        assertTrue(limiter.allowedRequests(user));
        assertTrue(limiter.allowedRequests(user));
    }

    @Test
    void shouldBlockAfterLimitReached() {
        SlidingWindow limiter = new SlidingWindow(3, 10000L, clock);
        String user = "dev";

        fakeTime = 0;

        limiter.allowedRequests(user);
        limiter.allowedRequests(user);
        limiter.allowedRequests(user);

        assertFalse(limiter.allowedRequests(user));
    }

    // ─────────────────────────────────────────────
    // SLIDING WINDOW CORE BEHAVIOR
    // ─────────────────────────────────────────────

    @Test
    void shouldSlideWindowCorrectly() {
        SlidingWindow limiter = new SlidingWindow(3, 10000L, clock);
        String user = "dev";

        fakeTime = 0;
        limiter.allowedRequests(user);

        fakeTime = 5000;
        limiter.allowedRequests(user);

        fakeTime = 9000;
        limiter.allowedRequests(user);

        // window full now

        fakeTime = 10001; // oldest expires

        assertTrue(limiter.allowedRequests(user)); // should allow
    }

    // ─────────────────────────────────────────────
    // BURST PREVENTION (MOST IMPORTANT DIFFERENCE)
    // ─────────────────────────────────────────────

    @Test
    void shouldPreventBurstAcrossBoundary() {
        SlidingWindow limiter = new SlidingWindow(3, 10000L, clock);
        String user = "dev";

        fakeTime = 0;
        limiter.allowedRequests(user);

        fakeTime = 9000;
        limiter.allowedRequests(user);
        limiter.allowedRequests(user);

        // Fixed window would allow 3 here ❌
        fakeTime = 10001;

        assertTrue(limiter.allowedRequests(user));   // only 1 allowed
        assertFalse(limiter.allowedRequests(user));  // blocked
        assertFalse(limiter.allowedRequests(user));  // blocked
    }

    // ─────────────────────────────────────────────
    // EXACT BOUNDARY CHECK
    // ─────────────────────────────────────────────

    @Test
    void shouldHandleExactBoundaryCorrectly() {
        SlidingWindow limiter = new SlidingWindow(3, 10000L, clock);
        String user = "dev";

        fakeTime = 0;
        limiter.allowedRequests(user);

        fakeTime = 5000;
        limiter.allowedRequests(user);

        fakeTime = 10000; // boundary

        limiter.allowedRequests(user);

        // still 3 requests inside window
        assertFalse(limiter.allowedRequests(user));
    }

    // ─────────────────────────────────────────────
    // MULTI USER ISOLATION
    // ─────────────────────────────────────────────

    @Test
    void shouldHandleMultipleUsersIndependently() {
        SlidingWindow limiter = new SlidingWindow(3, 10000L, clock);

        fakeTime = 0;

        limiter.allowedRequests("user1");
        limiter.allowedRequests("user1");
        limiter.allowedRequests("user1");

        assertFalse(limiter.allowedRequests("user1"));

        // user2 unaffected
        assertTrue(limiter.allowedRequests("user2"));
        assertTrue(limiter.allowedRequests("user2"));
        assertTrue(limiter.allowedRequests("user2"));
    }

    // ─────────────────────────────────────────────
    // SAME TIMESTAMP REQUESTS
    // ─────────────────────────────────────────────

    @Test
    void shouldHandleMultipleRequestsAtSameTime() {
        SlidingWindow limiter = new SlidingWindow(3, 10000L, clock);
        String user = "dev";

        fakeTime = 0;

        assertTrue(limiter.allowedRequests(user));
        assertTrue(limiter.allowedRequests(user));
        assertTrue(limiter.allowedRequests(user));
        assertFalse(limiter.allowedRequests(user));
    }

    // ─────────────────────────────────────────────
    // CLEANUP LOGIC (VERY IMPORTANT)
    // ─────────────────────────────────────────────

    @Test
    void shouldRemoveExpiredRequests() {
        SlidingWindow limiter = new SlidingWindow(3, 10000L, clock);
        String user = "dev";

        fakeTime = 0;
        limiter.allowedRequests(user);

        fakeTime = 20000; // old request fully expired

        assertTrue(limiter.allowedRequests(user)); // should not be blocked
        assertTrue(limiter.allowedRequests(user));
        assertTrue(limiter.allowedRequests(user));
    }

    // ─────────────────────────────────────────────
    // LONG IDLE TIME
    // ─────────────────────────────────────────────

    @Test
    void shouldRecoverAfterLongIdleTime() {
        SlidingWindow limiter = new SlidingWindow(3, 10000L, clock);
        String user = "dev";

        fakeTime = 0;

        limiter.allowedRequests(user);
        limiter.allowedRequests(user);
        limiter.allowedRequests(user);

        fakeTime = 100000;

        assertTrue(limiter.allowedRequests(user));
        assertTrue(limiter.allowedRequests(user));
    }

    // ─────────────────────────────────────────────
    // EDGE CASES
    // ─────────────────────────────────────────────

    @Test
    void shouldBlockAllIfCapacityZero() {
        SlidingWindow limiter = new SlidingWindow(0, 10000L, clock);
        String user = "dev";

        fakeTime = 0;

        assertFalse(limiter.allowedRequests(user));
    }

    @Test
    void shouldWorkWithVerySmallWindow() {
        SlidingWindow limiter = new SlidingWindow(2, 1L, clock);
        String user = "dev";

        fakeTime = 0;

        assertTrue(limiter.allowedRequests(user));
        assertTrue(limiter.allowedRequests(user));
        assertFalse(limiter.allowedRequests(user));

        fakeTime = 2;

        assertTrue(limiter.allowedRequests(user));
    }

    @Test
    void shouldBlockBurstEvenAfterWindowReset() {
        // This is the CORE difference from fixed window.
        // Fixed would allow 3 here. Sliding blocks because it remembers t=9000 requests.
        SlidingWindow limiter = new SlidingWindow(3, 10000L, clock);
        String user = "dev";

        fakeTime = 0;
        limiter.allowedRequests(user);

        fakeTime = 9000;
        limiter.allowedRequests(user);
        limiter.allowedRequests(user);

        fakeTime = 9999; // fixed window hasn't reset, sliding also blocks — but for different reason
        assertFalse(limiter.allowedRequests(user));
        assertFalse(limiter.allowedRequests(user));
        assertFalse(limiter.allowedRequests(user));
    }
}