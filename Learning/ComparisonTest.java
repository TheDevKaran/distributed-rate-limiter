import SlidingWindow.SlidingWindow;

public class ComparisonTest {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("========================================");
        System.out.println("Max 3 requests per 10s window");
        System.out.println("========================================\n");

        testFixed();
        testSliding();
    }

    // -------------------------------------------------------
    static void testFixed() throws InterruptedException {
        FixedWindowLimiter fixed = new FixedWindowLimiter(3, 10000);
        String user = "dev";
        long start = System.currentTimeMillis();

        System.out.println("=== FIXED WINDOW ===");

        System.out.println("\n-- Phase 1: 3 requests at t~0s --");
        for (int i = 1; i <= 3; i++) {
            log(i, fixed.allowedReq(user), start);
        }

        System.out.println("\n[ waiting for window to reset... ~10s ]");
        Thread.sleep(10100); // window resets

        System.out.println("\n-- Phase 2: burst immediately after reset --");
        System.out.println("   Window just reset → Fixed allows freely\n");
        for (int i = 4; i <= 6; i++) {
            log(i, fixed.allowedReq(user), start);
        }

        System.out.println("\n→ Fixed allowed 6 requests. 3 from Phase1 + 3 from Phase2.");
        System.out.println("→ If Phase1 was at t=9.9s and Phase2 at t=10.1s, that's 6 in 0.2s. DANGEROUS.\n");
    }

    // -------------------------------------------------------
    static void testSliding() throws InterruptedException {
        SlidingWindow sliding = new SlidingWindow(3, 10000L);
        String user = "dev";
        long start = System.currentTimeMillis();

        System.out.println("=== SLIDING WINDOW ===");

        System.out.println("\n-- Phase 1: 3 requests at t~0s --");
        for (int i = 1; i <= 3; i++) {
            log(i, sliding.allowedRequests(user), start);
        }

        System.out.println("\n[ waiting 10.1s ]");
        Thread.sleep(10100);

        System.out.println("\n-- Phase 2: burst after 10s --");
        System.out.println("   Sliding looks back 10s → Phase1 requests just expired → allows\n");
        for (int i = 4; i <= 6; i++) {
            log(i, sliding.allowedRequests(user), start);
        }

        System.out.println("\n→ Sliding also allows here because 10s genuinely passed.");
        System.out.println("→ The difference: try Phase2 at t=5s (before Phase1 expires). It will BLOCK.");
    }

    // -------------------------------------------------------
    static void log(int reqNum, boolean result, long start) {
        System.out.printf("  Req %d (t=%dms) → %s%n",
                reqNum,
                System.currentTimeMillis() - start,
                result ? "ALLOWED" : "BLOCKED");
    }
}