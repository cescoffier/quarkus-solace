package io.quarkiverse.solace.deployment;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.solace")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface SolaceBuildTimeConfig {
    /**
     * Metrics configuration.
     */
    MetricsConfig metrics();

    /**
     * Health configuration.
     */
    HealthConfig health();

    /**
     * Metrics configuration.
     */
    interface MetricsConfig {
        /**
         * Whether a metrics is enabled in case the micrometer is present.
         */
        @WithDefault("true")
        boolean enabled();
    }

    /**
     * Health configuration.
     */
    interface HealthConfig {
        /**
         * Whether the liveness health check should be exposed if the smallrye-health extension is present.
         */
        @WithDefault("true")
        boolean enabled();
    }
}