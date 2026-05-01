public class Main {
    public static void main(String[] args) throws InterruptedException {
        FixedWindowLimiter limiter = new FixedWindowLimiter(3, 5000);
        String user = "dev";
        // for(int i=0; i<=10; i++){
        //     boolean allow = limiter.allowedReq(user);
        //     System.out.println("Request " + i + "->" + (allow?"ALLOWED" : "bLOCKED"));
        //     Thread.sleep(1000);
        // }
           System.out.println("Init → " + limiter.allowedReq(user));

        // go near end of window
        Thread.sleep(4900);

        System.out.println("Req1 → " + limiter.allowedReq(user));
        Thread.sleep(50);

        System.out.println("Req2 → " + limiter.allowedReq(user));
        Thread.sleep(40);

        System.out.println("Req3 → " + limiter.allowedReq(user));

        // NOW new window should start
        Thread.sleep(20);

        System.out.println("Req4 → " + limiter.allowedReq(user));
        Thread.sleep(10);

        System.out.println("Req5 → " + limiter.allowedReq(user));
        Thread.sleep(10);

        System.out.println("Req6 → " + limiter.allowedReq(user));
    }
}
