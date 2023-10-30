package io.quarkiverse.solace.samples;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Incoming;

import io.quarkus.logging.Log;
import io.vertx.core.json.JsonObject;

@ApplicationScoped
public class HelloConsumer {

    @Incoming("hello-in")
    void consume(JsonObject p) {
        Log.infof("Received message: %s - %d", p.getString("name"), p.getInteger("age"));
    }

}
