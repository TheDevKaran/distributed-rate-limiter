import SlidingWindow.SlidingWindow;
import TokenBucket.TokenBucketRateLimiter;
import java.util.function.LongSupplier;

public class ComparisonTest {

    static long fakeTime = 0;
    static final LongSupplier clock = () -> fakeTime;

    public static void main(String[] args) {
        test1_FixedWindowBoundaryBurst();
        test2_SlidingWindowCorrectness();
        test3_TokenBucketGradualRefill();
    }

    // ─────────────────────────────────────────────────────────
    // TEST 1: Fixed Window — Boundary Burst
    // ─────────────────────────────────────────────────────────
    static void test1_FixedWindowBoundaryBurst() {
        header("TEST 1: Fixed Window — Boundary Burst Problem");

        FixedWindowLimiter fixed = new FixedWindowLimiter(3, 10000, clock);
        String user = "dev";

        fakeTime = 0;
        log(1, fixed.allowedReq(user));

        fakeTime = 9000;
        log(2, fixed.allowedReq(user));
        log(3, fixed.allowedReq(user));

        // FIXED → after boundary
        fakeTime = 10001;

        log(4, fixed.allowedReq(user));
        log(5, fixed.allowedReq(user));
        log(6, fixed.allowedReq(user));

        System.out.println();
    }

    // ─────────────────────────────────────────────────────────
    // TEST 2: Sliding Window — Correct Behavior
    // ─────────────────────────────────────────────────────────
    static void test2_SlidingWindowCorrectness() {
        header("TEST 2: Sliding Window");

        SlidingWindow sliding = new SlidingWindow(3, 10000L, clock);
        String user = "dev";

        fakeTime = 0;
        log(1, sliding.allowedRequests(user));

        fakeTime = 9000;
        log(2, sliding.allowedRequests(user));
        log(3, sliding.allowedRequests(user));

        // still same window → should block
        fakeTime = 9999;
        log(4, sliding.allowedRequests(user));
        log(5, sliding.allowedRequests(user));
        log(6, sliding.allowedRequests(user));

        // after boundary → DIFFERENCE
        fakeTime = 10001;
        log(7, sliding.allowedRequests(user));
        log(8, sliding.allowedRequests(user));
        log(9, sliding.allowedRequests(user));

        System.out.println();
    }

    // ─────────────────────────────────────────────────────────
    // TEST 3: Token Bucket — Gradual Refill
    // ─────────────────────────────────────────────────────────
    static void test3_TokenBucketGradualRefill() {
        header("TEST 3: Token Bucket");

        TokenBucketRateLimiter token = new TokenBucketRateLimiter(3, 0.3, clock);
        String user = "dev";

        fakeTime = 0;
        logToken(1, token.allowRequests(user));
        logToken(2, token.allowRequests(user));
        logToken(3, token.allowRequests(user));

        logToken(4, token.allowRequests(user));

        fakeTime = 4000;
        logToken(5, token.allowRequests(user));

        fakeTime = 10000;
        logToken(6, token.allowRequests(user));

        System.out.println();
    }

    static void log(int reqNum, boolean result) {
        System.out.printf("Req %-2d (t=%-6dms) | %s%n",
                reqNum, fakeTime,
                result ? "ALLOWED" : "BLOCKED");
    }

    static void logToken(int reqNum, boolean result) {
        System.out.printf("Req %-2d (t=%-6dms) | %s%n",
                reqNum, fakeTime,
                result ? "ALLOWED" : "BLOCKED");
    }

    static void header(String title) {
        System.out.println("==========================================");
        System.out.println(title);
        System.out.println("==========================================");
    }
}