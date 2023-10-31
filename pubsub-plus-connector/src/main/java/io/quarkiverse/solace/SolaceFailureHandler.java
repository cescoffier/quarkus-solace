package io.quarkiverse.solace;

import java.util.concurrent.CompletionStage;

import org.eclipse.microprofile.reactive.messaging.Metadata;

import com.solace.messaging.config.MessageAcknowledgementConfiguration;
import com.solace.messaging.receiver.AcknowledgementSupport;

import io.quarkiverse.solace.i18n.SolaceLogging;
import io.smallrye.mutiny.Uni;

public class SolaceFailureHandler {

    private final String channel;
    private final AcknowledgementSupport ackSupport;

    public SolaceFailureHandler(String channel, AcknowledgementSupport ackSupport) {
        this.channel = channel;
        this.ackSupport = ackSupport;
    }

    public CompletionStage<Void> handle(SolaceInboundMessage<?> msg, Throwable reason, Metadata metadata) {
        final MessageAcknowledgementConfiguration.Outcome outcome = metadata.get(SettleMetadata.class)
                .map(SettleMetadata::getOutcome)
                .orElseGet(() -> MessageAcknowledgementConfiguration.Outcome.REJECTED /* TODO get outcome from reason */);
        SolaceLogging.log.messageNacked(channel, outcome.toString().toLowerCase());
        return Uni.createFrom().voidItem()
                .invoke(() -> ackSupport.settle(msg.getMessage(), outcome))
                .runSubscriptionOn(msg::runOnMessageContext)
                .subscribeAsCompletionStage();
    }
}
