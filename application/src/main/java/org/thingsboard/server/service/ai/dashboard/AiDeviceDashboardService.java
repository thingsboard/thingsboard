// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai.dashboard;

import com.fasterxml.jackson.databind.JsonNode;
import org.thingsboard.server.service.ai.transport.TbAiClientRequest;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.UUID;

public interface AiDeviceDashboardService {

    JsonNode generateDashboard(UUID deviceId, JsonNode request, String tbAccessToken, TbAiClientRequest clientRequest, SecurityUser user);

}
