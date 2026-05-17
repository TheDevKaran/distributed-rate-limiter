package InMemroyTest;

import com.example.limiter.inmemory.token.TokenBucketRateLimiter;
import org.junit.jupiter.api.Test;

import java.util.function.LongSupplier;

import static org.junit.jupiter.api.Assertions.*;

public class TokenBucketTest {

    static long fakeTime = 0;
    static final LongSupplier clock = () -> fakeTime;

    // ─────────────────────────────────────────────
    // BASIC BEHAVIOR
    // ─────────────────────────────────────────────

    @Test
    void shouldAllowRequestsUpToCapacity() {
        TokenBucketRateLimiter limiter =
                new TokenBucketRateLimiter(3, 1, clock);

        String user = "dev";

        fakeTime = 0;

        assertTrue(limiter.allowRequests(user));
        assertTrue(limiter.allowRequests(user));
        assertTrue(limiter.allowRequests(user));
    }

    @Test
    void shouldBlockWhenTokensExhausted() {
        TokenBucketRateLimiter limiter =
                new TokenBucketRateLimiter(3, 1, clock);

        String user = "dev";

        fakeTime = 0;

        limiter.allowRequests(user);
        limiter.allowRequests(user);
        limiter.allowRequests(user);

        assertFalse(limiter.allowRequests(user));
    }

    // ─────────────────────────────────────────────
    // REFILL LOGIC
    // ─────────────────────────────────────────────

    @Test
    void shouldRefillTokensOverTime() {
        TokenBucketRateLimiter limiter =
                new TokenBucketRateLimiter(3, 1, clock);

        String user = "dev";

        fakeTime = 0;

        limiter.allowRequests(user);
        limiter.allowRequests(user);
        limiter.allowRequests(user);

        // 2 seconds → 2 tokens
        fakeTime = 2000;

        assertTrue(limiter.allowRequests(user));
        assertTrue(limiter.allowRequests(user));
    }

    @Test
    void shouldNotExceedCapacityAfterRefill() {
        TokenBucketRateLimiter limiter =
                new TokenBucketRateLimiter(3, 10, clock);

        String user = "dev";

        fakeTime = 0;

        limiter.allowRequests(user); // consume 1

        fakeTime = 10000; // huge refill

        // should cap at capacity = 3
        assertTrue(limiter.allowRequests(user));
        assertTrue(limiter.allowRequests(user));
        assertTrue(limiter.allowRequests(user));
        assertFalse(limiter.allowRequests(user));
    }

    // ─────────────────────────────────────────────
    // FRACTIONAL TOKENS (IMPORTANT)
    // ─────────────────────────────────────────────

@Test
void shouldAccumulateFractionalTokens() {
    TokenBucketRateLimiter limiter =
            new TokenBucketRateLimiter(3, 0.5, clock);

    String user = "dev";

    fakeTime = 0;

    // drain completely
    limiter.allowRequests(user);
    limiter.allowRequests(user);
    limiter.allowRequests(user);

    // now empty

    fakeTime = 1000; // +0.5 token
    assertFalse(limiter.allowRequests(user)); // correct

    fakeTime = 2000; // +1.0 token
    assertTrue(limiter.allowRequests(user));  // correct
}

    // ─────────────────────────────────────────────
    // BURST BEHAVIOR (ADVANTAGE)
    // ─────────────────────────────────────────────

    @Test
    void shouldAllowBurstIfTokensAvailable() {
        TokenBucketRateLimiter limiter =
                new TokenBucketRateLimiter(5, 1, clock);

        String user = "dev";

        fakeTime = 0;

        for(int i = 0; i < 5; i++){
            assertTrue(limiter.allowRequests(user));
        }

        assertFalse(limiter.allowRequests(user));
    }

    // ─────────────────────────────────────────────
    // MULTI USER
    // ─────────────────────────────────────────────

    @Test
    void shouldHandleMultipleUsersIndependently() {
        TokenBucketRateLimiter limiter =
                new TokenBucketRateLimiter(3, 1, clock);

        fakeTime = 0;

        limiter.allowRequests("user1");
        limiter.allowRequests("user1");
        limiter.allowRequests("user1");

        assertFalse(limiter.allowRequests("user1"));

        // user2 unaffected
        assertTrue(limiter.allowRequests("user2"));
        assertTrue(limiter.allowRequests("user2"));
        assertTrue(limiter.allowRequests("user2"));
    }

    // ─────────────────────────────────────────────
    // SAME TIMESTAMP
    // ─────────────────────────────────────────────

    @Test
    void shouldHandleMultipleRequestsAtSameTime() {
        TokenBucketRateLimiter limiter =
                new TokenBucketRateLimiter(3, 1, clock);

        fakeTime = 0;

        assertTrue(limiter.allowRequests("dev"));
        assertTrue(limiter.allowRequests("dev"));
        assertTrue(limiter.allowRequests("dev"));
        assertFalse(limiter.allowRequests("dev"));
    }

    // ─────────────────────────────────────────────
    // LONG IDLE
    // ─────────────────────────────────────────────

    @Test
    void shouldRecoverAfterLongIdleTime() {
        TokenBucketRateLimiter limiter =
                new TokenBucketRateLimiter(3, 1, clock);

        String user = "dev";

        fakeTime = 0;

        limiter.allowRequests(user);
        limiter.allowRequests(user);
        limiter.allowRequests(user);

        fakeTime = 100000; // long gap

        assertTrue(limiter.allowRequests(user));
        assertTrue(limiter.allowRequests(user));
        assertTrue(limiter.allowRequests(user));
    }

    // ─────────────────────────────────────────────
    // EDGE CASES
    // ─────────────────────────────────────────────

    @Test
    void shouldBlockAllIfCapacityZero() {
        TokenBucketRateLimiter limiter =
                new TokenBucketRateLimiter(0, 1, clock);

        assertFalse(limiter.allowRequests("dev"));
    }

    @Test
    void shouldHandleZeroRefillRate() {
        TokenBucketRateLimiter limiter =
                new TokenBucketRateLimiter(3, 0, clock);

        String user = "dev";

        fakeTime = 0;

        limiter.allowRequests(user);
        limiter.allowRequests(user);
        limiter.allowRequests(user);

        fakeTime = 10000;

        // no refill → still blocked
        assertFalse(limiter.allowRequests(user));
    }


    @Test
    void shouldNotAllowIfPartialRefillNotEnoughForOneToken() {
        TokenBucketRateLimiter limiter =
                new TokenBucketRateLimiter(3, 0.3, clock);

        String user = "dev";

        fakeTime = 0;
        limiter.allowRequests(user);
        limiter.allowRequests(user);
        limiter.allowRequests(user);

        fakeTime = 1000; // 0.3 tokens refilled — not enough for 1
        assertFalse(limiter.allowRequests(user));

        fakeTime = 2000; // 0.6 tokens — still not enough
        assertFalse(limiter.allowRequests(user));

        fakeTime = 4000; // 1.2 tokens — now enough
        assertTrue(limiter.allowRequests(user));
    }
}