package com.example.easylock;

public class Timer {
    private long saved_time;
    private boolean time_called = false;

    public void setTimer(long time) {
        saved_time = System.currentTimeMillis() + time;
        time_called = false;
    }

    public void cancel() {
        time_called = true;
    }

    public boolean finished() {
        if(time_called) {
            return false;
        }else {
            time_called = true;
            return System.currentTimeMillis() >= saved_time;
        }
    }
}
