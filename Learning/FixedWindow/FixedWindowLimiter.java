import java.util.*;
import java.util.HashMap;
import java.util.Map;

public class FixedWindowLimiter{
    Window window;
    long winTime;
    int maxReq=0;
    String userid;
    private Map<String, Window> mpp;

    FixedWindowLimiter(int maxReq, int winTime){
        this.maxReq = maxReq;
        this.winTime = winTime;
        this.mpp = new HashMap<>();
    }

    public boolean allowedReq(String userid){
        long now = System.currentTimeMillis();

        //user not present
        if(!mpp.containsKey(userid)){
            mpp.put(userid, new Window(now, 1));
            return true;
        }

        // user present
        Window w = mpp.get(userid);
        
        //window expired
        if(now - w.windowStartTime > winTime){
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