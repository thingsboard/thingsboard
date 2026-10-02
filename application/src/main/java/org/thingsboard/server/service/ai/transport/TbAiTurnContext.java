// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai.transport;

import org.thingsboard.ai.common.client.TbAiClient;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.service.security.model.SecurityUser;

/**
 * Everything one TB AI call needs on the TB side. {@code user} is null for background work (housekeeper deletions);
 * {@code tbAccessToken} is null for calls during which TB AI never calls ThingsBoard back.
 */
public record TbAiTurnContext(
        TenantId tenantId,
        UserId userId,
        SecurityUser user,
        String tbAccessToken,
        String acceptLanguage,
        TbAiClient.TokenProvider tokenProvider,
        TbAiClientRequest clientRequest
) {

    public TbAiTurnContext(SecurityUser user, String tbAccessToken, String acceptLanguage,
                           TbAiClient.TokenProvider tokenProvider, TbAiClientRequest clientRequest) {
        this(user.getTenantId(), user.getId(), user, tbAccessToken, acceptLanguage, tokenProvider, clientRequest);
    }

    public static TbAiTurnContext background(TenantId tenantId, UserId userId, TbAiClient.TokenProvider tokenProvider,
                                             TbAiClientRequest clientRequest) {
        return new TbAiTurnContext(tenantId, userId, null, null, null, tokenProvider, clientRequest);
    }

    public boolean isBackground() {
        return user == null;
    }

}
