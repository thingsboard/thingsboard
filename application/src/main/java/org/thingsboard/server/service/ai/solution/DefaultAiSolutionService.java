// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai.solution;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.ai.common.data.solution.SolutionStep;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.ai.TbAiService;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.UUID;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DefaultAiSolutionService implements AiSolutionService {

    private final TbAiService tbAiService;

    @Override
    public JsonNode startNew(SecurityUser user) {
        return tbAiService.process((client, tokenProvider) -> {
            return client.startNewSolution(tokenProvider);
        }, user, false);
    }

    @Override
    public JsonNode getSolution(UUID solutionId, SecurityUser user) {
        return tbAiService.process((client, tokenProvider) -> {
            return client.getSolution(solutionId, tokenProvider);
        }, user, false);
    }

    @Override
    public JsonNode getSolutions(SecurityUser user) {
        return tbAiService.process((client, tokenProvider) -> {
            return client.getSolutions(tokenProvider);
        }, user, false);
    }

    @Override
    public JsonNode chat(UUID solutionId, SolutionStep step, String message, SecurityUser user) {
        return tbAiService.process((client, tokenProvider) -> {
            return client.sendSolutionMessage(solutionId, step, message, tokenProvider);
        }, user);
    }

    @Override
    public JsonNode createSolution(UUID solutionId, SecurityUser user) {
        return tbAiService.process((client, tokenProvider) -> {
            return client.createSolution(solutionId, tokenProvider);
        }, user);
    }

    @Override
    public JsonNode updateData(UUID solutionId, String dataKey, JsonNode value, SecurityUser user) {
        return tbAiService.process((client, tokenProvider) -> {
            return client.updateData(solutionId, dataKey, value, tokenProvider);
        }, user);
    }

    @Override
    public void clearStep(UUID solutionId, SolutionStep step, SecurityUser user) {
        tbAiService.process((client, tokenProvider) -> {
            return client.clearStep(solutionId, step, tokenProvider);
        }, user, false);
    }

    @Override
    public JsonNode installSolution(UUID solutionId, String tbAccessToken, SecurityUser user) {
        return tbAiService.process((client, tokenProvider) -> {
            return client.installSolution(solutionId, tbAccessToken, tokenProvider);
        }, user, false);
    }

    @Override
    public JsonNode uninstallSolution(UUID solutionId, String tbAccessToken, SecurityUser user) {
        return tbAiService.process((client, tokenProvider) -> {
            return client.uninstallSolution(solutionId, tbAccessToken, tokenProvider);
        }, user, false);
    }

    @Override
    public void deleteSolution(UUID solutionId, SecurityUser user) {
        tbAiService.process((client, tokenProvider) -> {
            return client.deleteSolution(solutionId, tokenProvider);
        }, user, false);
    }

}
