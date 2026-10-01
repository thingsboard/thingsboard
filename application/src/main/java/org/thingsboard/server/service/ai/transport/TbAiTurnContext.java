// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai.transport;

import org.thingsboard.ai.common.client.TbAiClient;
import org.thingsboard.server.service.security.model.SecurityUser;

public record TbAiTurnContext(
        SecurityUser user,
        String tbAccessToken,
        String acceptLanguage,
        TbAiClient.TokenProvider tokenProvider
) {}
