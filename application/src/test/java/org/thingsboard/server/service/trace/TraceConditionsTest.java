// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.trace;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class TraceConditionsTest {
    private static final String LOCAL = "localMarker";
    private static final String SHARED = "sharedMarker";
    private static final String PIPELINE = "pipelineMarker";

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(PipelineConfig.class, SharedCacheConfig.class, LocalCacheConfig.class);

    @Test
    void monolithOnTheLocalCacheGetsThePipelineAndTheLocalBuffer() {
        runner.withPropertyValues("service.type=monolith", "otel.tracing.enabled=true", "cache.type=caffeine")
                .run(context -> {
                    assertThat(context).hasBean(PIPELINE);
                    assertThat(context).hasBean(LOCAL);
                    assertThat(context).doesNotHaveBean(SHARED);
                });
    }

    @Test
    void monolithOnRedisGetsThePipelineAndTheSharedBuffer() {
        runner.withPropertyValues("service.type=monolith", "otel.tracing.enabled=true", "cache.type=redis")
                .run(context -> {
                    assertThat(context).hasBean(PIPELINE);
                    assertThat(context).hasBean(SHARED);
                    assertThat(context).doesNotHaveBean(LOCAL);
                });
    }

    @Test
    void ruleEngineServiceGetsThePipelineToo() {
        runner.withPropertyValues("service.type=tb-rule-engine", "otel.tracing.enabled=true", "cache.type=redis")
                .run(context -> {
                    assertThat(context).hasBean(PIPELINE);
                    assertThat(context).hasBean(SHARED);
                });
    }

    @Test
    void unsetCacheTypeStillYieldsABuffer() {
        runner.withPropertyValues("service.type=monolith", "otel.tracing.enabled=true")
                .run(context -> {
                    assertThat(context).hasBean(PIPELINE);
                    assertThat(context).hasBean(LOCAL);
                    assertThat(context).doesNotHaveBean(SHARED);
                });
    }

    @Test
    void unrecognisedCacheTypeDegradesToTheLocalBuffer() {
        runner.withPropertyValues("service.type=monolith", "otel.tracing.enabled=true", "cache.type=bogus")
                .run(context -> {
                    assertThat(context).hasBean(PIPELINE);
                    assertThat(context).hasBean(LOCAL);
                    assertThat(context).doesNotHaveBean(SHARED);
                });
    }

    @Test
    void tracingDisabledYieldsNothingUnderEitherCacheType() {
        runner.withPropertyValues("service.type=monolith", "otel.tracing.enabled=false", "cache.type=caffeine")
                .run(context -> assertThat(context.getBeansOfType(Marker.class)).isEmpty());
        runner.withPropertyValues("service.type=monolith", "otel.tracing.enabled=false", "cache.type=redis")
                .run(context -> assertThat(context.getBeansOfType(Marker.class)).isEmpty());
    }

    @Test
    void coreOnlyServiceYieldsNothing() {
        runner.withPropertyValues("service.type=tb-core", "otel.tracing.enabled=true", "cache.type=redis")
                .run(context -> assertThat(context.getBeansOfType(Marker.class)).isEmpty());
    }

    private static final class Marker {
    }

    @Configuration
    @ConditionalOnExpression(TraceConditions.TRACE_ENABLED)
    static class PipelineConfig {
        @Bean(PIPELINE)
        Marker pipelineMarker() {
            return new Marker();
        }
    }

    @Configuration
    @ConditionalOnExpression(TraceConditions.SHARED_CACHE)
    static class SharedCacheConfig {
        @Bean(SHARED)
        Marker sharedMarker() {
            return new Marker();
        }
    }

    @Configuration
    @ConditionalOnExpression(TraceConditions.LOCAL_CACHE)
    static class LocalCacheConfig {
        @Bean(LOCAL)
        Marker localMarker() {
            return new Marker();
        }
    }
}
