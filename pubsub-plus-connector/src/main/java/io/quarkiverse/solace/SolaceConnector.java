package io.quarkiverse.solace;

import static io.smallrye.reactive.messaging.annotations.ConnectorAttribute.Direction.INCOMING;
import static io.smallrye.reactive.messaging.annotations.ConnectorAttribute.Direction.INCOMING_AND_OUTGOING;
import static io.smallrye.reactive.messaging.annotations.ConnectorAttribute.Direction.OUTGOING;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Flow;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.BeforeDestroyed;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.Reception;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.spi.Connector;

import com.solace.messaging.MessagingService;

import io.smallrye.reactive.messaging.annotations.ConnectorAttribute;
import io.smallrye.reactive.messaging.connector.InboundConnector;
import io.smallrye.reactive.messaging.connector.OutboundConnector;
import io.smallrye.reactive.messaging.health.HealthReport;
import io.smallrye.reactive.messaging.health.HealthReporter;
import io.smallrye.reactive.messaging.providers.connectors.ExecutionHolder;
import io.vertx.mutiny.core.Vertx;

@ApplicationScoped
@Connector(SolaceConnector.CONNECTOR_NAME)
// TODO only persisted is implemented
@ConnectorAttribute(name = "client.type", type = "string", direction = INCOMING_AND_OUTGOING, description = "Direct or persisted", defaultValue = "persisted")
@ConnectorAttribute(name = "client.lazy.start", type = "boolean", direction = INCOMING_AND_OUTGOING, description = "Whether the receiver or publisher is started at initialization or lazily at subscription time", defaultValue = "false")
@ConnectorAttribute(name = "subscriptions", type = "string", direction = INCOMING, description = "The comma separated list of subscriptions, the channel name if empty")
@ConnectorAttribute(name = "persistent.queue.type", type = "string", direction = INCOMING, description = "The queue type of receiver", defaultValue = "durable-non-exclusive")
@ConnectorAttribute(name = "persistent.queue.name", type = "string", direction = INCOMING, description = "The queue name of receiver")
@ConnectorAttribute(name = "persistent.missing-resource-creation-strategy", type = "string", direction = INCOMING, description = "Missing resource creation strategy", defaultValue = "create-on-start")
@ConnectorAttribute(name = "persistent.selector-query", type = "string", direction = INCOMING, description = "The receiver selector query")
@ConnectorAttribute(name = "persistent.replay.strategy", type = "string", direction = INCOMING, description = "The receiver replay strategy")
@ConnectorAttribute(name = "persistent.replay.timebased-start-time", type = "string", direction = INCOMING, description = "The receiver replay timebased start time")
@ConnectorAttribute(name = "persistent.replay.replication-group-message-id", type = "string", direction = INCOMING, description = "The receiver replay replication group message id")
@ConnectorAttribute(name = "topic", type = "string", direction = OUTGOING, description = "The topic to publish messages, by default the channel name")
@ConnectorAttribute(name = "max-inflight-messages", type = "long", direction = OUTGOING, description = "The maximum number of messages to be written to Solace broker. It limits the number of messages waiting to be written and acknowledged by the broker. You can set this attribute to `0` remove the limit", defaultValue = "1024")
@ConnectorAttribute(name = "waitForPublishReceipt", type = "boolean", direction = OUTGOING, description = "Whether the client waits to receive the publish receipt from Solace broker before acknowledging the message", defaultValue = "true")
@ConnectorAttribute(name = "delivery.ack.timeout", type = "int", direction = OUTGOING, description = "Delivery ack timeout")
@ConnectorAttribute(name = "delivery.ack.window.size", type = "int", direction = OUTGOING, description = "Delivery ack window size")
@ConnectorAttribute(name = "back-pressure.strategy", type = "string", direction = OUTGOING, description = "Outgoing messages backpressure strategy", defaultValue = "elastic")
@ConnectorAttribute(name = "back-pressure.buffer-capacity", type = "int", direction = OUTGOING, description = "Outgoing messages backpressure buffer capacity", defaultValue = "1024")
public class SolaceConnector implements InboundConnector, OutboundConnector, HealthReporter {

    public static final String CONNECTOR_NAME = "quarkus-solace";

    @Inject
    ExecutionHolder executionHolder;

    @Inject
    MessagingService solace;

    Vertx vertx;

    List<SolaceIncomingChannel> incomingChannels = new CopyOnWriteArrayList<>();
    List<SolaceOutgoingChannel> outgoingChannels = new CopyOnWriteArrayList<>();

    public void terminate(
            @Observes(notifyObserver = Reception.IF_EXISTS) @Priority(50) @BeforeDestroyed(ApplicationScoped.class) Object event) {
        incomingChannels.forEach(SolaceIncomingChannel::close);
        outgoingChannels.forEach(SolaceOutgoingChannel::close);
    }

    @PostConstruct
    void init() {
        this.vertx = executionHolder.vertx();
    }

    @Override
    public Flow.Publisher<? extends Message<?>> getPublisher(Config config) {
        var ic = new SolaceConnectorIncomingConfiguration(config);
        SolaceIncomingChannel channel = new SolaceIncomingChannel(vertx, ic, solace);
        incomingChannels.add(channel);
        return channel.getStream();
    }

    @Override
    public Flow.Subscriber<? extends Message<?>> getSubscriber(Config config) {
        var oc = new SolaceConnectorOutgoingConfiguration(config);
        SolaceOutgoingChannel channel = new SolaceOutgoingChannel(vertx, oc, solace);
        outgoingChannels.add(channel);
        return channel.getSubscriber();
    }

    @Override
    public HealthReport getStartup() {
        HealthReport.HealthReportBuilder builder = HealthReport.builder();
        for (SolaceIncomingChannel in : incomingChannels) {
            in.isStarted(builder);
        }
        for (SolaceOutgoingChannel sink : outgoingChannels) {
            sink.isStarted(builder);
        }
        return builder.build();
    }

    @Override
    public HealthReport getReadiness() {
        HealthReport.HealthReportBuilder builder = HealthReport.builder();
        for (SolaceIncomingChannel in : incomingChannels) {
            in.isReady(builder);
        }
        for (SolaceOutgoingChannel sink : outgoingChannels) {
            sink.isReady(builder);
        }
        return builder.build();

    }

    @Override
    public HealthReport getLiveness() {
        HealthReport.HealthReportBuilder builder = HealthReport.builder();
        for (SolaceIncomingChannel in : incomingChannels) {
            in.isAlive(builder);
        }
        for (SolaceOutgoingChannel out : outgoingChannels) {
            out.isAlive(builder);
        }
        return builder.build();
    }
}
