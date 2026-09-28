// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.agent.step;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.agent.step.state.RunJobStepState;

import static org.assertj.core.api.Assertions.assertThat;

class AgentAppStepDeserializationTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // A RUN_JOB step in the wire shape app templates are published in; pins the JSON property names.
    private static final String RUN_JOB_STEP = """
            {
              "id": "580e8400-e29b-41d4-a716-446655440014",
              "type": "RUN_JOB",
              "templateOnly": false,
              "state": {
                "image": { "value": "thingsboard/tb-edge-pe:4.3.0EDGEPE", "userChoice": false },
                "binds": { "value": ["${compose.svcImgRegex(thingsboard/tb-edge-pe:.+).volumes}"], "userChoice": false },
                "env": { "value": ["${compose.svcImgRegex(thingsboard/tb-edge-pe:.+).environment}"], "userChoice": false },
                "networkFromServiceImageRegexPattern": { "value": "postgres:.+", "userChoice": false }
              }
            }
            """;

    // A COMPOSE upgrade step; 'serviceImageRegexPatterns' is the published name of the
    // 'servicesImagesRegexPatterns' field, so it has to be asserted from JSON rather than inferred.
    private static final String COMPOSE_STEP = """
            {
              "id": "580e8400-e29b-41d4-a716-446655440013",
              "type": "COMPOSE",
              "templateOnly": false,
              "state": {
                "serviceImageRegexPatterns": { "value": ["postgres:.+"], "userChoice": false }
              }
            }
            """;

    @Test
    void runJobStateBindsToTypedFields() throws Exception {
        AgentAppStep step = MAPPER.readValue(RUN_JOB_STEP, AgentAppStep.class);
        assertThat(step).isInstanceOf(RunJobStep.class);
        RunJobStepState state = ((RunJobStep) step).getState();
        assertThat(state.getImage().getValue()).isEqualTo("thingsboard/tb-edge-pe:4.3.0EDGEPE");
        assertThat(state.getBinds().getValue()).containsExactly("${compose.svcImgRegex(thingsboard/tb-edge-pe:.+).volumes}");
        assertThat(state.getEnv().getValue()).containsExactly("${compose.svcImgRegex(thingsboard/tb-edge-pe:.+).environment}");
        assertThat(state.getNetworkFromServiceImageRegexPattern().getValue()).isEqualTo("postgres:.+");
        assertThat(state.getImage().isUserChoice()).isFalse();
    }

    @Test
    void composeServiceRegexBindsViaJsonProperty() throws Exception {
        AgentAppStep step = MAPPER.readValue(COMPOSE_STEP, AgentAppStep.class);
        assertThat(step).isInstanceOf(ComposeStep.class);
        assertThat(((ComposeStep) step).getState().getServicesImagesRegexPatterns().getValue())
                .containsExactly("postgres:.+");
    }
}
