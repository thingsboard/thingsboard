// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai.solution;

import com.fasterxml.jackson.databind.JsonNode;
import org.thingsboard.ai.common.data.solution.SolutionStep;
import org.thingsboard.server.service.ai.transport.TbAiClientRequest;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.UUID;

public interface AiSolutionService {

    JsonNode startNew(SecurityUser user);

    JsonNode getSolution(UUID solutionId, SecurityUser user);

    JsonNode getSolutions(SecurityUser user);

    JsonNode chat(UUID solutionId, SolutionStep step, String message, SecurityUser user);

    JsonNode createSolution(UUID solutionId, SecurityUser user);

    JsonNode updateData(UUID solutionId, String dataKey, JsonNode value, SecurityUser user);

    void clearStep(UUID solutionId, SolutionStep step, SecurityUser user);

    JsonNode installSolution(UUID solutionId, String tbAccessToken, TbAiClientRequest clientRequest, SecurityUser user);

    JsonNode uninstallSolution(UUID solutionId, String tbAccessToken, TbAiClientRequest clientRequest, SecurityUser user);

    void deleteSolution(UUID solutionId, SecurityUser user);

}
