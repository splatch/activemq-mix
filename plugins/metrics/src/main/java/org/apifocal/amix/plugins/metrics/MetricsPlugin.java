package org.apifocal.amix.plugins.metrics;

import com.codahale.metrics.MetricRegistry;
import org.apache.activemq.broker.Broker;
import org.apache.activemq.broker.BrokerPlugin;

public class MetricsPlugin implements BrokerPlugin {
    private final MetricRegistry metricRegistry;

    public MetricsPlugin(MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
    }

    @Override
    public Broker installPlugin(Broker broker) throws Exception {
        return new MetricsBroker(broker, metricRegistry);
    }
}
