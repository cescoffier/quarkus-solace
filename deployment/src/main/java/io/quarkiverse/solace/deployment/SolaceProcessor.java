package io.quarkiverse.solace.deployment;

import com.solacesystems.jcsmp.JCSMPFactory;

import io.quarkiverse.solace.runtime.SolaceClient;
import io.quarkiverse.solace.runtime.SolaceConfig;
import io.quarkiverse.solace.runtime.SolaceRecorder;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;

class SolaceProcessor {

    private static final String FEATURE = "solace";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void registerBean(BuildProducer<AdditionalBeanBuildItem> producer) {
        producer.produce(AdditionalBeanBuildItem.unremovableOf(SolaceClient.class));
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void init(SolaceConfig config, SolaceRecorder recorder) {
        recorder.init(config);
    }

    @BuildStep
    void configureNativeCompilation(BuildProducer<RuntimeInitializedClassBuildItem> producer) {
        producer.produce(new RuntimeInitializedClassBuildItem(JCSMPFactory.class.getName()));
    }

}
