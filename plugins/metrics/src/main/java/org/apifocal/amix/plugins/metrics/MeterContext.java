package org.apifocal.amix.plugins.metrics;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;

import java.util.function.Supplier;

public interface MeterContext {

    Timer timer(String name);

    Counter counter(String name);

    Meter meter(String name);

    Gauge gauge(String name, Supplier<Long> supplier);

    MeterContext child(String name);

    MeterContext sibling(String name);

}
