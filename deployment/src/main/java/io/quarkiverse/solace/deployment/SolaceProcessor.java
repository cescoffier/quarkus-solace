package io.quarkiverse.solace.deployment;

import java.util.Optional;

import com.solacesystems.jcsmp.JCSMPFactory;

import io.quarkiverse.solace.MessagingServiceClientCustomizer;
import io.quarkiverse.solace.runtime.SolaceClient;
import io.quarkiverse.solace.runtime.SolaceConfig;
import io.quarkiverse.solace.runtime.SolaceRecorder;
import io.quarkiverse.solace.runtime.observability.SolaceMetricBinder;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.metrics.MetricsCapabilityBuildItem;
import io.quarkus.runtime.metrics.MetricsFactory;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;

class SolaceProcessor {

    private static final String FEATURE = "solace";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void registerBean(BuildProducer<AdditionalBeanBuildItem> producer) {
        producer.produce(AdditionalBeanBuildItem.unremovableOf(SolaceClient.class));
        producer.produce(AdditionalBeanBuildItem.unremovableOf(MessagingServiceClientCustomizer.class));
    }

    @BuildStep
    ExtensionSslNativeSupportBuildItem ssl() {
        return new ExtensionSslNativeSupportBuildItem(FEATURE);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void init(SolaceConfig config, SolaceRecorder recorder,
            SolaceBuildTimeConfig btConfig, Optional<MetricsCapabilityBuildItem> metrics, SolaceMetricBinder metricRecorder) {
        recorder.init(config);

        if (metrics.isPresent() && btConfig.metrics().enabled()) {
            if (metrics.get().metricsSupported(MetricsFactory.MICROMETER)) {
                metricRecorder.initMetrics();
            }
        }
    }

    @BuildStep
    void configureNativeCompilation(BuildProducer<RuntimeInitializedClassBuildItem> producer) {
        producer.produce(new RuntimeInitializedClassBuildItem(JCSMPFactory.class.getName()));
    }

    @BuildStep
    HealthBuildItem addHealthCheck(SolaceBuildTimeConfig buildTimeConfig) {
        return new HealthBuildItem("io.quarkiverse.solace.runtime.observability.SolaceHealthCheck",
                buildTimeConfig.health().enabled());
    }

}
