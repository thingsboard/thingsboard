// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai.dashboard;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.ai.common.channel.ChannelProtocol;
import org.thingsboard.ai.common.channel.DashboardOperationRequest;
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
class DefaultAiDeviceDashboardService implements AiDeviceDashboardService {

    private final TbAiService tbAiService;
    private final TbAiOperations operations;

    @Override
    public JsonNode generateDashboard(UUID deviceId, JsonNode request, String tbAccessToken, TbAiClientRequest clientRequest, SecurityUser user) {
        return tbAiService.process((client, tokenProvider) -> operations.execute(
                new TbAiOperation(ChannelProtocol.DASHBOARD_GENERATE, new DashboardOperationRequest(deviceId, request),
                        () -> client.generateDashboard(deviceId, request, tbAccessToken, tokenProvider)),
                new TbAiTurnContext(user, tbAccessToken, null, tokenProvider, clientRequest)), user);
    }

}
