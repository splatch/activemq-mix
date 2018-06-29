package org.apifocal.amix.plugins.metrics.context;

import com.codahale.metrics.*;
import org.apifocal.amix.plugins.metrics.MeterContext;

import java.util.function.Supplier;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * A default implementation (thus simple) of meter context.
 *
 * It delegates creation of metric
 */
public class SimpleMeterContext implements MeterContext {

    private final MetricRegistry metricRegistry;
    private final String prefix;

    public SimpleMeterContext(MetricRegistry metricRegistry, String prefix) {
        this.metricRegistry = metricRegistry;
        this.prefix = prefix;
    }

    public SimpleMeterContext(MetricRegistry metricRegistry, Class<?> clazz) {
        this(metricRegistry, clazz.getName());
    }

    @Override
    public Timer timer(String name) {
        return metricRegistry.timer(createName(name));
    }

    @Override
    public Counter counter(String name) {
        return metricRegistry.counter(createName(name));
    }

    @Override
    public Meter meter(String name) {
        return metricRegistry.meter(name);
    }

    @Override
    public Gauge<Long> gauge(String name, Supplier<Long> supplier) {
        return metricRegistry.gauge(createName(name), () -> supplier::get);
    }

    @Override
    public MeterContext child(String name) {
        return new SimpleMeterContext(metricRegistry, createName(name));
    }

    @Override
    public MeterContext sibling(String name) {
        return new SimpleMeterContext(metricRegistry, name);
    }

    private String createName(String ... segments) {
        return name(prefix, segments);
    }

    private static String[] collapse(String prefix, String[] components) {
        String[] name = new String[components.length + 1];
        name[0] = prefix;
        System.arraycopy(components, 0, name, 1, components.length);
        return components;
    }

}
