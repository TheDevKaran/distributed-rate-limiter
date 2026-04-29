package SlidingWindow;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class SlidingWindow{
        private Map<String, Queue<Long>> mpp;
        int maxReq;
        Long winTime;
        String userId;

        SlidingWindow(int maxReq, Long winTime){
            this.maxReq = maxReq;
            this.winTime = winTime;
            this.mpp = new HashMap<>();
        }

        public boolean allowedRequests(String userId){
            long now = System.currentTimeMillis();
            
            //user not found
            if(!mpp.containsKey(userId)){
                Queue<Long> q = new LinkedList<>();
                q.add(now);
                mpp.put(userId, q);
                return true;
            }

            //user found
            Queue<Long> queue = mpp.get(userId);
            
            //remove timestamps
            while(!queue.isEmpty() && queue.peek()<(now-winTime))
                queue.poll();

            if(queue.size()<maxReq){
                queue.add(now);
                return true;
            }


            return false;
        }
}