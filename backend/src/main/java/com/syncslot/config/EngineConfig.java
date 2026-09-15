package com.syncslot.config;

import com.syncslot.algorithm.SchedulingEngine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exposes the (pure-Java) scheduling engine as a Spring bean so services can
 * inject it while the algorithm package stays free of Spring annotations.
 * Scoring weights are intentionally surfaced here so they can be tuned without
 * touching the engine.
 */
@Configuration
public class EngineConfig {

    @Bean
    public SchedulingEngine schedulingEngine() {
        SchedulingEngine engine = new SchedulingEngine();
        engine.setWeightAvailability(3.0);
        engine.setWeightPriority(1.5);
        engine.setWeightPreference(1.0);
        engine.setWeightBuffer(1.5);
        engine.setWeightDisruption(2.0);
        return engine;
    }
}
