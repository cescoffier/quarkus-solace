package io.quarkiverse.solace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.junit.jupiter.api.Test;

import com.solace.messaging.receiver.PersistentMessageReceiver;
import com.solace.messaging.resources.Queue;
import com.solace.messaging.resources.TopicSubscription;

import io.quarkiverse.solace.base.WeldTestBase;
import io.smallrye.mutiny.Multi;
import io.smallrye.reactive.messaging.test.common.config.MapBasedConfig;

public class SolacePublisherTest extends WeldTestBase {

    @Test
    void publisher() {
        MapBasedConfig config = new MapBasedConfig()
                .with("mp.messaging.outgoing.out.connector", "quarkus-solace")
                .with("mp.messaging.outgoing.out.topic", topic);

        List<String> expected = new CopyOnWriteArrayList<>();

        // Start listening first
        PersistentMessageReceiver receiver = messagingService.createPersistentMessageReceiverBuilder()
                .withSubscriptions(TopicSubscription.of(topic))
                .build(Queue.nonDurableExclusiveQueue());
        receiver.receiveAsync(inboundMessage -> expected.add(inboundMessage.getPayloadAsString()));
        receiver.start();

        // Run app that publish messages
        MyApp app = runApplication(config, MyApp.class);
        // Assert on published messages
        await().untilAsserted(() -> assertThat(app.getAcked()).contains("1", "2", "3", "4", "5"));
        // Assert on received messages
        await().untilAsserted(() -> assertThat(expected).contains("1", "2", "3", "4", "5"));
    }

    @ApplicationScoped
    static class MyApp {
        private final List<String> acked = new CopyOnWriteArrayList<>();

        @Outgoing("out")
        Multi<Message<String>> out() {
            return Multi.createFrom().items("1", "2", "3", "4", "5")
                    .map(payload -> Message.of(payload).withAck(() -> {
                        acked.add(payload);
                        return CompletableFuture.completedFuture(null);
                    }));
        }

        public List<String> getAcked() {
            return acked;
        }
    }
}
