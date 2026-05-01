import java.util.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.LongSupplier;

public class FixedWindowLimiter{
    Window window;
    long winTime;
    int maxReq=0;
    String userid;
    private Map<String, Window> mpp;
    private final LongSupplier clock;     
    
    FixedWindowLimiter(int maxReq, int winTime) {
        this(maxReq, winTime, System::currentTimeMillis);      // 2. chain to new constructor
    }// 1. add field

    FixedWindowLimiter(int maxReq, int winTime, LongSupplier clock){
        this.maxReq = maxReq;
        this.winTime = winTime;
        this.mpp = new HashMap<>();
        this.clock = clock;                                  // 2. initialize field 
    }

    public boolean allowedReq(String userid){
        long now = clock.getAsLong();

        //user not present
        if(!mpp.containsKey(userid)){
            mpp.put(userid, new Window(now, 1));
            return true;
        }

        // user present
        Window w = mpp.get(userid);
        
        //window expired
        if(now - w.windowStartTime >= winTime){
            w.windowStartTime = now;
            w.count = 1;
            return true;
        }

        //same windoe 
        if(w.count < maxReq){
            w.count++;
            return true;
        }

        //limit exceeded
        return false;
    }
}

//     1. If user not present:
//        create window(count=1, time=now)
//        allow

// 2. Else:
//        if window expired:
//             reset count=1, time=now
//             allow

//        else:
//             if count < max:
//                 count++
//                 allow
//             else:
//                 block
// }}