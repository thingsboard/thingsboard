// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai.solution;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.ai.common.data.solution.SolutionStep;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.ai.common.channel.ChannelProtocol;
import org.thingsboard.ai.common.channel.SolutionChatRequest;
import org.thingsboard.ai.common.channel.SolutionDataRequest;
import org.thingsboard.ai.common.channel.SolutionOperationRequest;
import org.thingsboard.ai.common.channel.SolutionStepRequest;
import org.thingsboard.server.service.ai.TbAiService;
import org.thingsboard.server.service.ai.transport.TbAiClientRequest;
import org.thingsboard.server.service.ai.transport.TbAiOperation;
import org.thingsboard.server.service.ai.transport.TbAiOperations;
import org.thingsboard.server.service.ai.transport.TbAiTurnContext;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.UUID;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DefaultAiSolutionService implements AiSolutionService {

    private final TbAiService tbAiService;
    private final TbAiOperations operations;

    @Override
    public JsonNode startNew(SecurityUser user) {
        return tbAiService.process((client, tokenProvider) -> operations.execute(
                new TbAiOperation(ChannelProtocol.SOLUTION_START, null,
                        () -> client.startNewSolution(tokenProvider)), user, tokenProvider), user, false);
    }

    @Override
    public JsonNode getSolution(UUID solutionId, SecurityUser user) {
        return tbAiService.process((client, tokenProvider) -> operations.execute(
                new TbAiOperation(ChannelProtocol.SOLUTION_GET, new SolutionOperationRequest(solutionId),
                        () -> client.getSolution(solutionId, tokenProvider)), user, tokenProvider), user, false);
    }

    @Override
    public JsonNode getSolutions(SecurityUser user) {
        return tbAiService.process((client, tokenProvider) -> operations.execute(
                new TbAiOperation(ChannelProtocol.SOLUTION_LIST, null,
                        () -> client.getSolutions(tokenProvider)), user, tokenProvider), user, false);
    }

    @Override
    public JsonNode chat(UUID solutionId, SolutionStep step, String message, SecurityUser user) {
        return tbAiService.process((client, tokenProvider) -> operations.execute(
                new TbAiOperation(ChannelProtocol.SOLUTION_CHAT, new SolutionChatRequest(solutionId, step, message),
                        () -> client.sendSolutionMessage(solutionId, step, message, tokenProvider)), user, tokenProvider), user);
    }

    @Override
    public JsonNode createSolution(UUID solutionId, SecurityUser user) {
        return tbAiService.process((client, tokenProvider) -> operations.execute(
                new TbAiOperation(ChannelProtocol.SOLUTION_CREATE, new SolutionOperationRequest(solutionId),
                        () -> client.createSolution(solutionId, tokenProvider)), user, tokenProvider), user);
    }

    @Override
    public JsonNode updateData(UUID solutionId, String dataKey, JsonNode value, SecurityUser user) {
        return tbAiService.process((client, tokenProvider) -> operations.execute(
                new TbAiOperation(ChannelProtocol.SOLUTION_DATA_UPDATE, new SolutionDataRequest(solutionId, dataKey, value),
                        () -> client.updateData(solutionId, dataKey, value, tokenProvider)), user, tokenProvider), user);
    }

    @Override
    public void clearStep(UUID solutionId, SolutionStep step, SecurityUser user) {
        tbAiService.process((client, tokenProvider) -> operations.execute(
                new TbAiOperation(ChannelProtocol.SOLUTION_STEP_CLEAR, new SolutionStepRequest(solutionId, step),
                        () -> client.clearStep(solutionId, step, tokenProvider)), user, tokenProvider), user, false);
    }

    @Override
    public JsonNode installSolution(UUID solutionId, String tbAccessToken, TbAiClientRequest clientRequest, SecurityUser user) {
        return tbAiService.process((client, tokenProvider) -> operations.execute(
                new TbAiOperation(ChannelProtocol.SOLUTION_INSTALL, new SolutionOperationRequest(solutionId),
                        () -> client.installSolution(solutionId, tbAccessToken, tokenProvider)),
                new TbAiTurnContext(user, tbAccessToken, null, tokenProvider, clientRequest)), user, false);
    }

    @Override
    public JsonNode uninstallSolution(UUID solutionId, String tbAccessToken, TbAiClientRequest clientRequest, SecurityUser user) {
        return tbAiService.process((client, tokenProvider) -> operations.execute(
                new TbAiOperation(ChannelProtocol.SOLUTION_UNINSTALL, new SolutionOperationRequest(solutionId),
                        () -> client.uninstallSolution(solutionId, tbAccessToken, tokenProvider)),
                new TbAiTurnContext(user, tbAccessToken, null, tokenProvider, clientRequest)), user, false);
    }

    @Override
    public void deleteSolution(UUID solutionId, SecurityUser user) {
        tbAiService.process((client, tokenProvider) -> operations.execute(
                new TbAiOperation(ChannelProtocol.SOLUTION_DELETE, new SolutionOperationRequest(solutionId),
                        () -> client.deleteSolution(solutionId, tokenProvider)), user, tokenProvider), user, false);
    }

}
