package org.apifocal.amix.plugins.metrics.threading;

import com.codahale.metrics.Timer;
import org.apifocal.amix.plugins.metrics.MeterContext;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

public class MeteredRejectedExecutionHandler implements RejectedExecutionHandler {

    private final MeterContext meter;
    private final RejectedExecutionHandler delegate;

    public MeteredRejectedExecutionHandler(MeterContext meter, RejectedExecutionHandler delegate) {
        this.meter = meter;
        this.delegate = delegate;
    }

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        Timer executions = meter.timer("rejectedExecutions");
        executions.time(() -> delegate.rejectedExecution(r, executor));
    }

}
