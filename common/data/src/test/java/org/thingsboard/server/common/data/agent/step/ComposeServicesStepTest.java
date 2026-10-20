// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.agent.step;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.agent.step.state.ComposeDownStepState;
import org.thingsboard.server.common.data.agent.step.state.ComposeStepState;
import org.thingsboard.server.common.data.agent.step.state.StepField;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ComposeServicesStepTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String COMPOSE = """
            {
              "services": {
                "mytbedge": { "image": "thingsboard/tb-edge-pe:4.3.0EDGEPE" },
                "postgres": { "image": "postgres:16" },
                "cache": { "image": "redis:7" }
              }
            }
            """;

    @Test
    void servicesAreResolvedInPatternOrderAndJoinedWithCommas() {
        Map<String, String> metadata = downStep(List.of("redis:.+", "postgres:.+"))
                .getCommandMetadata(application(), null);

        assertThat(metadata.get(ComposeServicesStep.SERVICES)).isEqualTo("cache,postgres");
    }

    @Test
    void duplicateAndUnmatchedPatternsAreCollapsed() {
        Map<String, String> metadata = downStep(Arrays.asList("postgres:.+", "postgres:1.*", "mysql:.+", null, "  "))
                .getCommandMetadata(application(), null);

        assertThat(metadata.get(ComposeServicesStep.SERVICES)).isEqualTo("postgres");
    }

    @Test
    void servicesKeyIsOmittedWhenNothingMatches() {
        Map<String, String> metadata = downStep(List.of("mysql:.+")).getCommandMetadata(application(), null);

        assertThat(metadata).doesNotContainKey(ComposeServicesStep.SERVICES);
    }

    @Test
    void servicesKeyIsOmittedWhenNoPatternsAreConfigured() {
        Map<String, String> metadata = downStep(null).getCommandMetadata(application(), null);

        assertThat(metadata).doesNotContainKey(ComposeServicesStep.SERVICES);
    }

    @Test
    void overlayStateWinsOverTheTemplatePatterns() {
        ComposeDownStepState overlay = new ComposeDownStepState();
        overlay.setServicesImagesRegexPatterns(new StepField<>(List.of("redis:.+"), true));

        Map<String, String> metadata = downStep(List.of("postgres:.+")).getCommandMetadata(application(), overlay);

        assertThat(metadata.get(ComposeServicesStep.SERVICES)).isEqualTo("cache");
    }

    @Test
    void composeIsEmittedOnlyByStepsThatOptIn() {
        Map<String, String> downMetadata = downStep(List.of("postgres:.+")).getCommandMetadata(application(), null);
        assertThat(downMetadata).doesNotContainKey(ComposeServicesStep.COMPOSE);

        ComposeStepState state = new ComposeStepState();
        state.setServicesImagesRegexPatterns(new StepField<>(List.of("postgres:.+"), false));
        ComposeStep composeStep = new ComposeStep();
        composeStep.setState(state);

        Map<String, String> composeMetadata = composeStep.getCommandMetadata(application(), null);
        assertThat(composeMetadata).containsKey(ComposeServicesStep.COMPOSE);
        assertThat(readTree(composeMetadata.get(ComposeServicesStep.COMPOSE)).at("/services/postgres/image").asText())
                .isEqualTo("postgres:16");
    }

    @Test
    void metadataIsEmptyWhenTheApplicationCarriesNoCompose() {
        AgentApplication app = new AgentApplication();
        app.setConfig(new DockerComposeConfig());

        assertThat(downStep(List.of("postgres:.+")).getCommandMetadata(app, null))
                .doesNotContainKeys(ComposeServicesStep.SERVICES, ComposeServicesStep.COMPOSE);
    }

    private static ComposeDownStep downStep(List<String> patterns) {
        ComposeDownStepState state = new ComposeDownStepState();
        if (patterns != null) {
            state.setServicesImagesRegexPatterns(new StepField<>(patterns, false));
        }
        ComposeDownStep step = new ComposeDownStep();
        step.setState(state);
        return step;
    }

    private static AgentApplication application() {
        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(readTree(COMPOSE));
        AgentApplication app = new AgentApplication();
        app.setConfig(config);
        return app;
    }

    private static JsonNode readTree(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
}
