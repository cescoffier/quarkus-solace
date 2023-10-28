package io.quarkiverse.solace.runtime;

import jakarta.enterprise.inject.spi.CDI;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class SolaceRecorder {

    public void init(SolaceConfig config) {
        CDI.current().select(SolaceClient.class).get().configure(config);
    }

}
