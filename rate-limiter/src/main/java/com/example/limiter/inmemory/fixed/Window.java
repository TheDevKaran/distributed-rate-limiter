package com.example.limiter.inmemory.fixed;

public class Window{
    long windowStartTime;
    int count;
    Window(long windowStartTime, int count){
        this.windowStartTime = windowStartTime;
        this.count = count;
    }
}