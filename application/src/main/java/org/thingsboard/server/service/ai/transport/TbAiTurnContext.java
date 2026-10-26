// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai.transport;

import org.thingsboard.ai.common.client.TbAiClient;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.service.security.model.SecurityUser;

/**
 * Everything one TB AI call needs on the TB side. {@code user} is null for background work (housekeeper deletions);
 * {@code tbAccessToken} is null for calls during which TB AI never calls ThingsBoard back. {@code clientOrigin} is the
 * ThingsBoard base URL the user reached for this call: TB AI shows it to the user (device connectivity commands) and
 * audits it, and the calls back into ThingsBoard are forwarded as if made through it.
 */
public record TbAiTurnContext(
        TenantId tenantId,
        UserId userId,
        SecurityUser user,
        String tbAccessToken,
        String acceptLanguage,
        TbAiClient.TokenProvider tokenProvider,
        String clientOrigin
) {

    public TbAiTurnContext(SecurityUser user, String tbAccessToken, String acceptLanguage,
                           TbAiClient.TokenProvider tokenProvider, String clientOrigin) {
        this(user.getTenantId(), user.getId(), user, tbAccessToken, acceptLanguage, tokenProvider, clientOrigin);
    }

    public static TbAiTurnContext background(TenantId tenantId, UserId userId, TbAiClient.TokenProvider tokenProvider,
                                             String clientOrigin) {
        return new TbAiTurnContext(tenantId, userId, null, null, null, tokenProvider, clientOrigin);
    }

    public boolean isBackground() {
        return user == null;
    }

}
