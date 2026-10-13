// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai.dashboard;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.ai.TbAiService;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.UUID;

@Service
@TbCoreComponent
@RequiredArgsConstructor
class DefaultAiDeviceDashboardService implements AiDeviceDashboardService {

    private final TbAiService tbAiService;

    @Override
    public JsonNode generateDashboard(UUID deviceId, JsonNode request, String tbAccessToken, SecurityUser user) {
        return tbAiService.process((client, tokenProvider) ->
                client.generateDashboard(deviceId, request, tbAccessToken, tokenProvider), user);
    }

}
