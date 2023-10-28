package io.quarkiverse.solace.runtime;

import java.util.Map;
import java.util.Properties;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import com.solace.messaging.MessagingService;
import com.solace.messaging.MessagingServiceClientBuilder;
import com.solace.messaging.config.SolaceProperties;
import com.solace.messaging.config.profile.ConfigurationProfile;

import io.quarkiverse.solace.MessagingServiceClientCustomizer;
import io.quarkus.runtime.ShutdownEvent;

@Singleton
public class SolaceClient {

    private MessagingService service;

    @Inject
    Instance<MessagingServiceClientCustomizer> customizers;

    public void configure(SolaceConfig config) {
        Properties properties = new Properties();
        properties.put(SolaceProperties.TransportLayerProperties.HOST, config.host());
        properties.put(SolaceProperties.ServiceProperties.VPN_NAME, config.vpn());
        for (Map.Entry<String, String> entry : config.extra().entrySet()) {
            properties.put(entry.getKey(), entry.getValue());
            if (!entry.getKey().startsWith("solace.messaging.")) {
                properties.put("solace.messaging." + entry.getKey(), entry.getValue());
            }
        }

        MessagingServiceClientBuilder builder = MessagingService.builder(ConfigurationProfile.V1)
                .fromProperties(properties);

        if (customizers.isUnsatisfied()) {
            service = builder.build();
        } else {
            if (!customizers.isResolvable()) {
                throw new IllegalStateException("Multiple MessagingServiceClientCustomizer instances found");
            } else {
                service = customizers.get().customize(builder).build();
            }
        }

    }

    @Produces
    public MessagingService connectAndGet() {
        return service.connect();
    }

    public void shutdown(@Observes ShutdownEvent event) {
        service.disconnect();
    }

}
