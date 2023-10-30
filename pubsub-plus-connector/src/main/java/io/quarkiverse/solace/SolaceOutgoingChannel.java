package io.quarkiverse.solace;

import java.util.concurrent.Flow;

import org.eclipse.microprofile.reactive.messaging.Message;

import com.solace.messaging.MessagingService;
import com.solace.messaging.PersistentMessagePublisherBuilder;
import com.solace.messaging.PubSubPlusClientException;
import com.solace.messaging.publisher.OutboundMessage;
import com.solace.messaging.publisher.OutboundMessageBuilder;
import com.solace.messaging.publisher.PersistentMessagePublisher;
import com.solace.messaging.publisher.PersistentMessagePublisher.PublishReceipt;
import com.solace.messaging.resources.Topic;

import io.netty.handler.codec.http.HttpHeaderValues;
import io.quarkiverse.solace.i18n.SolaceLogging;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniEmitter;
import io.smallrye.reactive.messaging.OutgoingMessageMetadata;
import io.smallrye.reactive.messaging.health.HealthReport;
import io.smallrye.reactive.messaging.providers.helpers.MultiUtils;
import io.vertx.core.json.Json;
import io.vertx.mutiny.core.Vertx;

public class SolaceOutgoingChannel implements PersistentMessagePublisher.MessagePublishReceiptListener {

    private final PersistentMessagePublisher publisher;
    private final String channel;
    private final Flow.Subscriber<? extends Message<?>> subscriber;
    private final Topic topic;

    public SolaceOutgoingChannel(Vertx vertx, SolaceConnectorOutgoingConfiguration oc, MessagingService solace) {
        this.channel = oc.getChannel();
        PersistentMessagePublisherBuilder builder = solace.createPersistentMessagePublisherBuilder();
        switch (oc.getBackPressureStrategy()) {
            case "reject":
                builder.onBackPressureReject(oc.getBackPressureBufferCapacity());
                break;
            case "wait":
                builder.onBackPressureWait(oc.getBackPressureBufferCapacity());
                break;
            default:
                builder.onBackPressureElastic();
                break;
        }
        oc.getDeliveryAckTimeout().ifPresent(builder::withDeliveryAckTimeout);
        oc.getDeliveryAckWindowSize().ifPresent(builder::withDeliveryAckWindowSize);
        this.publisher = builder.build();
        publisher.setMessagePublishReceiptListener(this);
        this.topic = Topic.of(oc.getTopic().orElse(this.channel));
        // TODO send with in flight messages
        this.subscriber = MultiUtils.via(multi -> multi.call(m -> publishMessage(publisher, m, solace.messageBuilder())
                .onItem().transformToUni(receipt -> {
                    OutgoingMessageMetadata.setResultOnMessage(m, receipt);
                    return Uni.createFrom().completionStage(m.ack());
                })
                .onFailure().recoverWithUni(t -> Uni.createFrom().completionStage(m.nack(t))))
                .onSubscription().call(() -> Uni.createFrom().completionStage(publisher.startAsync())));
    }

    private Uni<PublishReceipt> publishMessage(PersistentMessagePublisher publisher, Message<?> m,
            OutboundMessageBuilder msgBuilder) {
        Topic topic = this.topic;
        OutboundMessage outboundMessage;
        m.getMetadata(SolaceOutboundMetadata.class).ifPresent(metadata -> {
            metadata.getHttpContentHeaders().forEach(msgBuilder::withHTTPContentHeader);
            metadata.getProperties().forEach(msgBuilder::withProperty);
            if (metadata.getExpiration() != null) {
                msgBuilder.withExpiration(metadata.getExpiration());
            }
            if (metadata.getPriority() != null) {
                msgBuilder.withPriority(metadata.getPriority());
            }
            if (metadata.getSenderId() != null) {
                msgBuilder.withSenderId(metadata.getSenderId());
            }
            if (metadata.getApplicationMessageType() != null) {
                msgBuilder.withApplicationMessageType(metadata.getApplicationMessageType());
            }
            if (metadata.getTimeToLive() != null) {
                msgBuilder.withTimeToLive(metadata.getTimeToLive());
            }
            if (metadata.getApplicationMessageId() != null) {
                msgBuilder.withApplicationMessageId(metadata.getApplicationMessageId());
            }
            if (metadata.getClassOfService() != null) {
                msgBuilder.withClassOfService(metadata.getClassOfService());
            }
        });
        Object payload = m.getPayload();
        if (payload instanceof OutboundMessage) {
            outboundMessage = (OutboundMessage) payload;
        } else if (payload instanceof String) {
            outboundMessage = msgBuilder.build((String) payload);
        } else if (payload instanceof byte[]) {
            outboundMessage = msgBuilder.build((byte[]) payload);
        } else {
            outboundMessage = msgBuilder
                    .withHTTPContentHeader(HttpHeaderValues.APPLICATION_JSON.toString(), "")
                    .build(Json.encode(payload));
        }
        return Uni.createFrom().<PublishReceipt> emitter(e -> {
            try {
                publisher.publish(outboundMessage, topic, e);
            } catch (Throwable t) {
                e.fail(t);
            }
        }).invoke(() -> SolaceLogging.log.successfullyToTopic(channel, topic.getName()));
    }

    public Flow.Subscriber<? extends Message<?>> getSubscriber() {
        return this.subscriber;
    }

    void close() {
        publisher.terminate(5000);
    }

    @Override
    public void onPublishReceipt(PublishReceipt publishReceipt) {
        UniEmitter<PublishReceipt> uniEmitter = (UniEmitter<PublishReceipt>) publishReceipt.getUserContext();
        PubSubPlusClientException exception = publishReceipt.getException();
        if (exception != null) {
            uniEmitter.fail(exception);
        } else {
            uniEmitter.complete(publishReceipt);
        }
    }

    public void isStarted(HealthReport.HealthReportBuilder builder) {

    }

    public void isReady(HealthReport.HealthReportBuilder builder) {

    }

    public void isAlive(HealthReport.HealthReportBuilder builder) {

    }
}
