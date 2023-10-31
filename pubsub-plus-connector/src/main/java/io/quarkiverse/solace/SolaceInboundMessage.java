package io.quarkiverse.solace;

import static io.smallrye.reactive.messaging.providers.locals.ContextAwareMessage.captureContextMetadata;

import java.util.concurrent.CompletionStage;

import org.eclipse.microprofile.reactive.messaging.Metadata;

import com.solace.messaging.receiver.InboundMessage;

import io.netty.handler.codec.http.HttpHeaderValues;
import io.quarkiverse.solace.i18n.SolaceLogging;
import io.smallrye.reactive.messaging.providers.MetadataInjectableMessage;
import io.smallrye.reactive.messaging.providers.locals.ContextAwareMessage;
import io.vertx.core.buffer.Buffer;

public class SolaceInboundMessage<T> implements ContextAwareMessage<T>, MetadataInjectableMessage<T> {

    private final InboundMessage msg;
    private final SolaceAckHandler ackHandler;
    private final SolaceFailureHandler nackHandler;
    private final T payload;

    private Metadata metadata;

    public SolaceInboundMessage(InboundMessage message, SolaceAckHandler ackHandler, SolaceFailureHandler nackHandler) {
        this.msg = message;
        this.payload = (T) convertPayload();
        this.ackHandler = ackHandler;
        this.nackHandler = nackHandler;
        this.metadata = captureContextMetadata(new SolaceInboundMetadata(message));
    }

    public InboundMessage getMessage() {
        return msg;
    }

    @Override
    public T getPayload() {
        return this.payload;
    }

    private Object convertPayload() {
        // Neither of these are guaranteed to be non-null
        final String contentType = msg.getRestInteroperabilitySupport().getHTTPContentType();
        final String contentEncoding = msg.getRestInteroperabilitySupport().getHTTPContentEncoding();
        final Buffer body = Buffer.buffer(msg.getPayloadAsBytes());

        // If there is a content encoding specified, we don't try to unwrap
        if (contentEncoding == null || contentEncoding.isBlank()) {
            try {
                // Do our best with text and json
                if (HttpHeaderValues.APPLICATION_JSON.toString().equalsIgnoreCase(contentType)) {
                    // This could be  JsonArray, JsonObject, String etc. depending on buffer contents
                    return body.toJson();
                } else if (HttpHeaderValues.TEXT_PLAIN.toString().equalsIgnoreCase(contentType)) {
                    return body.toString();
                }
            } catch (Throwable t) {
                SolaceLogging.log.typeConversionFallback();
            }
            // Otherwise fall back to raw byte array
        } else {
            // Just silence the warning if we have a binary message
            if (!HttpHeaderValues.APPLICATION_OCTET_STREAM.toString().equalsIgnoreCase(contentType)) {
                SolaceLogging.log.typeConversionFallback();
            }
        }
        return body.getBytes();
    }

    @Override
    public Metadata getMetadata() {
        return metadata;
    }

    @Override
    public CompletionStage<Void> ack() {
        return ackHandler.handle(this);
    }

    @Override
    public CompletionStage<Void> nack(Throwable reason, Metadata nackMetadata) {
        return nackHandler.handle(this, reason, nackMetadata);
    }

    @Override
    public void injectMetadata(Object metadataObject) {
        this.metadata = this.metadata.with(metadataObject);
    }
}
