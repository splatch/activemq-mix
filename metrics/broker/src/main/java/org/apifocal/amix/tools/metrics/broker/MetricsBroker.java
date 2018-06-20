package org.apifocal.amix.tools.metrics.broker;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.apache.activemq.broker.*;
import org.apache.activemq.broker.region.Destination;
import org.apache.activemq.broker.region.Subscription;
import org.apache.activemq.command.*;
import org.apifocal.amix.tools.metrics.broker.context.SimpleMeterContext;
import org.apifocal.amix.tools.metrics.broker.threading.MeteredThreadPoolExecutor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class MetricsBroker extends BrokerFilter {

    private final MeterContext meter;

    @FunctionalInterface
    interface Execution {
        void execute() throws Exception;
    }

    public MetricsBroker(Broker next, MetricRegistry metricRegistry) {
        super(next);
        this.meter = new SimpleMeterContext(metricRegistry, "broker." + getBrokerId().getValue());
    }

    @Override
    public void acknowledge(ConsumerBrokerExchange consumerExchange, MessageAck ack) throws Exception {
        Timer timer = meter.timer("acknowledge");

        timer(timer, () -> getNext().acknowledge(consumerExchange, ack));
    }

    @Override
    public Response messagePull(ConnectionContext context, MessagePull pull) throws Exception {
        Timer timer = meter.timer("messagePull");
        return timer.time(() -> getNext().messagePull(context, pull));
    }

    @Override
    public void addConnection(ConnectionContext context, ConnectionInfo info) throws Exception {
        Timer timer = meter.timer("addConnection");
        timer(timer, () -> getNext().addConnection(context, info));
    }

    @Override
    public Subscription addConsumer(ConnectionContext context, ConsumerInfo info) throws Exception {
        increase(
            "consumers",
            meter,
            meter.sibling("client." + info.getClientId()),
            meter.sibling("consumer." + info.getConsumerId()),
            meter.sibling(info.getDestination().getDestinationTypeAsString() + "." + info.getDestination().getPhysicalName())
        );

        Timer timer = meter.timer("addConsumer");
        return timer.time(() -> getNext().addConsumer(context, info));
    }

    @Override
    public void addProducer(ConnectionContext context, ProducerInfo info) throws Exception {
        increase(
            "producers",
            meter,
            meter.sibling(info.getDestination().getDestinationTypeAsString() + "." + info.getDestination().getPhysicalName()),
            meter.sibling("producer." + info.getProducerId())
        );

        Timer timer = meter.timer("addConsumer");
        timer(timer, () -> getNext().addProducer(context, info));
    }

    @Override
    public void removeConsumer(ConnectionContext context, ConsumerInfo info) throws Exception {
        decrease(
            "consumers",
            meter,
            meter.sibling("client." + info.getClientId()),
            meter.sibling("consumer." + info.getConsumerId()),
            meter.sibling(info.getDestination().getDestinationTypeAsString() + "." + info.getDestination().getPhysicalName())
        );

        Timer timer = meter.timer("removeConsumer");
        timer(timer, () -> getNext().removeConsumer(context, info));
    }

    @Override
    public void removeProducer(ConnectionContext context, ProducerInfo info) throws Exception {
        decrease(
            "producers",
            meter,
            meter.sibling(info.getDestination().getDestinationTypeAsString() + "." + info.getDestination().getPhysicalName()),
            meter.sibling("producer." + info.getProducerId())
        );

        Timer timer = meter.timer("removeProducer");
        timer(timer, () -> getNext().removeProducer(context, info));
    }

    @Override
    public Destination addDestination(ConnectionContext context, ActiveMQDestination destination, boolean createIfTemporary) throws Exception {
        increase(
            "destinations",
            meter,
            meter.sibling("client." + context.getClientId())
        );

        Timer timer = meter.timer("addDestination");
        return timer.time(() -> getNext().addDestination(context, destination, createIfTemporary));
    }

    @Override
    public void removeDestination(ConnectionContext context, ActiveMQDestination destination, long timeout) throws Exception {
        decrease(
            "destinations",
            meter,
            meter.sibling("client." + context.getClientId())
        );

        Timer timer = meter.timer("removeDestination");
        timer(timer, () -> getNext().removeDestination(context, destination, timeout));
    }


    @Override
    public ThreadPoolExecutor getExecutor() {
        return new MeteredThreadPoolExecutor(meter.child("pool"), getNext().getExecutor());
    }

    @Override
    public void networkBridgeStarted(BrokerInfo brokerInfo, boolean createdByDuplex, String remoteIp) {
        List<MeterContext> contexts = new ArrayList<>();
        contexts.add(meter);
        contexts.add(meter.sibling("remote." + remoteIp));

        Arrays.stream(brokerInfo.getPeerBrokerInfos())
            .map(BrokerInfo::getBrokerId)
            .map(BrokerId::getValue)
            .map(meter::sibling)
            .forEach(contexts::add);

        increase(
            "bridges",
            contexts
        );

        Timer timer = meter.timer("networkBridgeStarted");
        timer.time(() -> getNext().networkBridgeStarted(brokerInfo, createdByDuplex, remoteIp));
    }

    @Override
    public void networkBridgeStopped(BrokerInfo brokerInfo) {
        List<MeterContext> contexts = new ArrayList<>();
        contexts.add(meter);

        Arrays.stream(brokerInfo.getPeerBrokerInfos())
            .map(BrokerInfo::getBrokerId)
            .map(BrokerId::getValue)
            .map(meter::sibling)
            .forEach(contexts::add);

        increase(
            "bridges",
            contexts
        );

        Timer timer = meter.timer("networkBridgeStopped");
        timer.time(() -> getNext().networkBridgeStopped(brokerInfo));
    }

    private void timer(Timer timer, Execution runnable) throws Exception {
        timer.time(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                runnable.execute();
                return null;
            }
        });
    }

    private void increase(String name, MeterContext ... contexts) {
        increase(name, Arrays.stream(contexts));
    }

    private void increase(String name, List<MeterContext> contexts) {
        increase(name, contexts.stream());
    }

    private void increase(String name, Stream<MeterContext> contexts) {
        counter(name, contexts, Counter::inc);
    }

    private void decrease(String name, MeterContext ... contexts) {
        decrease(name, Arrays.stream(contexts));
    }

    private void decrease(String name, List<MeterContext> contexts) {
        decrease(name, contexts.stream());
    }

    private void decrease(String name, Stream<MeterContext> contexts) {
        counter(name, contexts, Counter::dec);
    }

    private void counter(String name, Stream<MeterContext> contexts, Consumer<Counter> callback) {
        contexts.map(context -> context.counter(name))
            .forEach(callback);
    }

}
