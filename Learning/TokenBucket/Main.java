package TokenBucket;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(3, 1);
        String user = "dev";
        for(int i=0; i<10; i++){
            boolean check = limiter.allowRequests(user);
            System.out.println("test : " + i+ check);
            Thread.sleep(500);
        }
    }
}
