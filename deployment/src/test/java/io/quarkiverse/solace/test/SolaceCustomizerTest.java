package io.quarkiverse.solace.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.solace.messaging.MessagingService;
import com.solace.messaging.MessagingServiceClientBuilder;
import com.solace.messaging.config.RetryStrategy;
import com.solace.messaging.publisher.DirectMessagePublisher;

import io.quarkiverse.solace.MessagingServiceClientCustomizer;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.QuarkusTestResource;

@QuarkusTestResource(SolaceTestResource.class)
public class SolaceCustomizerTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(MyCustomizer.class));

    @Inject
    MyCustomizer customizer;
    @Inject
    MessagingService solace;

    @Test
    public void test() {
        assertThat(customizer.called()).isTrue();
        DirectMessagePublisher publisher = solace.createDirectMessagePublisherBuilder()
                .build().start();
        publisher.terminate(1);
    }

    @ApplicationScoped
    public static class MyCustomizer implements MessagingServiceClientCustomizer {

        AtomicBoolean called = new AtomicBoolean();

        @Override
        public MessagingServiceClientBuilder customize(MessagingServiceClientBuilder builder) {
            called.set(true);
            return builder.withReconnectionRetryStrategy(RetryStrategy.neverRetry());
        }

        public boolean called() {
            return called.get();
        }
    }
}
