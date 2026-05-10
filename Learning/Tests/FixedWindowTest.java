import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.function.LongSupplier;

public class FixedWindowTest {

    static long fakeTime = 0;
    static final LongSupplier clock = () -> fakeTime;

    // ✅ Test 1: Allow first N requests
    @Test
    void shouldAllowRequestsUpToLimit() {
        FixedWindowLimiter limiter = new FixedWindowLimiter(3, 10000, clock);
        String user = "dev";

        fakeTime = 0;

        assertTrue(limiter.allowedReq(user));
        assertTrue(limiter.allowedReq(user));
        assertTrue(limiter.allowedReq(user));
    }

    // ❌ Test 2: Block after limit reached
    @Test
    void shouldBlockAfterMaxRequestsInSameWindow() {
        FixedWindowLimiter limiter = new FixedWindowLimiter(3, 10000, clock);
        String user = "dev";

        fakeTime = 0;

        assertTrue(limiter.allowedReq(user));
        assertTrue(limiter.allowedReq(user));
        assertTrue(limiter.allowedReq(user));

        // 4th request should be blocked
        assertFalse(limiter.allowedReq(user));
    }

    // 🔄 Test 3: Reset after window expires
    @Test
    void shouldResetAfterWindowExpires() {
        FixedWindowLimiter limiter = new FixedWindowLimiter(3, 10000, clock);
        String user = "dev";

        fakeTime = 0;

        assertTrue(limiter.allowedReq(user));
        assertTrue(limiter.allowedReq(user));
        assertTrue(limiter.allowedReq(user));

        // move to next window
        fakeTime = 10001;

        // should allow again
        assertTrue(limiter.allowedReq(user));
    }

    // ⚠️ Test 4: Boundary burst problem
    @Test
    void shouldAllowBurstAcrossWindowBoundary() {
        FixedWindowLimiter limiter = new FixedWindowLimiter(3, 10000, clock);
        String user = "dev";

        fakeTime = 0;
        assertTrue(limiter.allowedReq(user)); // 1

        fakeTime = 9000;
        assertTrue(limiter.allowedReq(user)); // 2
        assertTrue(limiter.allowedReq(user)); // 3

        // new window
        fakeTime = 10001;

        // allows again (burst issue)
        assertTrue(limiter.allowedReq(user)); // 4
        assertTrue(limiter.allowedReq(user)); // 5
        assertTrue(limiter.allowedReq(user)); // 6
    }
}