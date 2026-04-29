package SlidingWindow;

class Main{
    public static void main(String[] args) throws InterruptedException{
        SlidingWindow slider = new SlidingWindow(3, 5000L);
        String user = "dev";
        // for(int i=0; i<10; i++){
        //     if(slider.allowedRequests(user)){
        //         System.out.println( i + "Allowed");
        //     }else
        //         System.out.println( i + "Blocked");
        //     Thread.sleep(1000);
        // }
       Thread.sleep(4900);

        System.out.println("Req 1 → " + slider.allowedRequests(user));
        Thread.sleep(50);

        System.out.println("Req 2 → " + slider.allowedRequests(user));
        Thread.sleep(40);

        System.out.println("Req 3 → " + slider.allowedRequests(user));
        Thread.sleep(20);

        System.out.println("Req 4 → " + slider.allowedRequests(user));
        Thread.sleep(10);

        System.out.println("Req 5 → " + slider.allowedRequests(user));
        Thread.sleep(10);

        System.out.println("Req 6 → " + slider.allowedRequests(user));

        
    }
}