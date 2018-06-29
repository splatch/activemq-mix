package org.apifocal.amix.plugins.metrics.threading;

import org.apifocal.amix.plugins.metrics.MeterContext;

import java.util.concurrent.ThreadFactory;

public class MeteredThreadFactory implements ThreadFactory {

    private final MeterContext meter;
    private final ThreadFactory delegate;

    public MeteredThreadFactory(MeterContext meter, ThreadFactory delegate) {
        this.meter = meter;
        this.delegate = delegate;
    }

    @Override
    public Thread newThread(Runnable r) {
        MeteredRunnable runnable = new MeteredRunnable(r);
        Thread thread = delegate.newThread(runnable);
        runnable.setTimer(meter.timer(thread.getName()));
        return thread;
    }


}
