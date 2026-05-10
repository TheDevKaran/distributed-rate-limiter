package com.example.inmemory.fixed;

import java.util.HashMap;
import java.util.Map;
import java.util.function.LongSupplier;

public class FixedWindowLimiter {

    long winTime;
    int maxReq = 0;

    private Map<String, Window> mpp;
    private final LongSupplier clock;

    public FixedWindowLimiter(int maxReq, int winTime) {
        this(maxReq, winTime, System::currentTimeMillis);
    }

    public FixedWindowLimiter(
            int maxReq,
            int winTime,
            LongSupplier clock
    ) {

        this.maxReq = maxReq;
        this.winTime = winTime;
        this.mpp = new HashMap<>();
        this.clock = clock;
    }

    public boolean allowedReq(String userid) {

        if (maxReq <= 0)
            return false;

        long now = clock.getAsLong();

        // user not present
        if (!mpp.containsKey(userid)) {

            mpp.put(
                    userid,
                    new Window(now, 1)
            );

            return true;
        }

        // user present
        Window w = mpp.get(userid);

        // window expired
        if (now - w.windowStartTime >= winTime) {

            w.windowStartTime = now;
            w.count = 1;

            return true;
        }

        // same window
        if (w.count < maxReq) {

            w.count++;

            return true;
        }

        // limit exceeded
        return false;
    }
}