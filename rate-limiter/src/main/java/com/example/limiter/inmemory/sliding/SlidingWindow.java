package com.example.limiter.inmemory.sliding;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.function.LongSupplier;

public class SlidingWindow{
        private Map<String, Queue<Long>> mpp;
        int maxReq;
        Long winTime;
        String userId;
        private final LongSupplier clock;                          // 1. add field

         public SlidingWindow(int maxReq, Long winTime){
            this(maxReq, winTime, System::currentTimeMillis);      // 2. chain to new constructor
        }

        public SlidingWindow(int maxReq, Long winTime, LongSupplier clock){
            this.clock = clock;                                  // 2. initialize field
            this.maxReq = maxReq;
            this.winTime = winTime;
            this.mpp = new HashMap<>();
        }

        public boolean allowedRequests(String userId){
            if (maxReq <= 0) return false;
            long now = clock.getAsLong();
            
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