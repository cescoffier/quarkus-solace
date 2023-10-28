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
     * Metrics configuration.
     */
    interface MetricsConfig {
        /**
         * Whether a metrics is enabled in case the micrometer is present.
         */
        @WithDefault("true")
        boolean enabled();
    }
}
