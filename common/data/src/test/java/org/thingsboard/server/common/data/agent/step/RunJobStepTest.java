// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.agent.step;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.agent.step.state.RunJobStepState;
import org.thingsboard.server.common.data.agent.step.state.StepField;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RunJobStepTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String COMPOSE = """
            {
              "services": {
                "mytbedge": {
                  "image": "thingsboard/tb-edge-pe:4.3.0EDGEPE",
                  "environment": { "SPRING_DATASOURCE_URL": "jdbc:postgresql://postgres:5432/tb" },
                  "volumes": ["tb-edge-data:/data", "tb-edge-logs:/var/log/tb-edge"]
                },
                "postgres": { "image": "postgres:16" }
              }
            }
            """;

    @Test
    void resolvesBindsEnvAndNetworkService() throws Exception {
        RunJobStepState state = new RunJobStepState();
        state.setImage(new StepField<>("thingsboard/tb-edge-pe:4.3.0EDGEPE", false));
        state.setBinds(new StepField<>(List.of("${compose.svcImgRegex(thingsboard/tb-edge-pe:.+).volumes}"), false));
        state.setEnv(new StepField<>(List.of("${compose.svcImgRegex(thingsboard/tb-edge-pe:.+).environment}"), false));
        state.setNetworkFromServiceImageRegexPattern(new StepField<>("postgres:.+", false));

        RunJobStep step = new RunJobStep();
        step.setState(state);

        Map<String, String> metadata = step.getCommandMetadata(application(), null);

        JsonNode job = MAPPER.readTree(metadata.get(RunJobStepState.JOB));
        assertThat(job.get("image").asText()).isEqualTo("thingsboard/tb-edge-pe:4.3.0EDGEPE");
        assertThat(job.get("binds")).extracting(JsonNode::asText)
                .containsExactly("tb-edge-data:/data", "tb-edge-logs:/var/log/tb-edge");
        assertThat(job.get("env")).extracting(JsonNode::asText)
                .containsExactly("SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/tb");
        assertThat(job.get("service").asText()).isEqualTo("postgres");
    }

    @Test
    void keepsNonRefEntriesAndOmitsServiceWhenNoRegex() throws Exception {
        RunJobStepState state = new RunJobStepState();
        state.setImage(new StepField<>("busybox", false));
        state.setBinds(new StepField<>(List.of("plain-vol:/mnt"), false));

        RunJobStep step = new RunJobStep();
        step.setState(state);

        JsonNode job = MAPPER.readTree(step.getCommandMetadata(application(), null).get(RunJobStepState.JOB));
        assertThat(job.get("binds")).extracting(JsonNode::asText).containsExactly("plain-vol:/mnt");
        assertThat(job.has("service")).isFalse();
    }

    private static AgentApplication application() throws Exception {
        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(MAPPER.readTree(COMPOSE));
        AgentApplication app = new AgentApplication();
        app.setConfig(config);
        return app;
    }
}
