package org.apifocal.amix.tools.metrics.broker;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;

import java.util.concurrent.TimeUnit;

public class Reporters {

    public static ConsoleReporter console(MetricRegistry registry) {
        ConsoleReporter reporter = ConsoleReporter.forRegistry(registry).build();
        reporter.start(10, TimeUnit.SECONDS);
        return reporter;
    }

}
