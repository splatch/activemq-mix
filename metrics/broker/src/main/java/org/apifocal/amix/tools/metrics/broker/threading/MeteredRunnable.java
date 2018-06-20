package org.apifocal.amix.tools.metrics.broker.threading;

import com.codahale.metrics.Timer;

public class MeteredRunnable implements Runnable {

    private final Runnable delegate;

    private Timer timer;

    public MeteredRunnable(Runnable delegate) {
        this.delegate = delegate;
    }

    @Override
    public void run() {
        timer.time(delegate);
    }

    public void setTimer(Timer timer) {
        this.timer = timer;
    }
}
