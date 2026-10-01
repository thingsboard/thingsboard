// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai;

import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.Map;

public interface TbAiTokenProvider {

    String getToken(SecurityUser user);

    default Map<String, String> getAdditionalInfo(SecurityUser user) {
        return Map.of();
    }

    /**
     * Checks that TB AI can call back the user's ThingsBoard at the origin sent with the token. Only the legacy
     * HTTP paths (SSE chat, HTTP solution and dashboard operations) need this: over the channel TB AI calls TB
     * through the WebSocket, so a localhost origin is fine there.
     */
    default void validateCallbackOrigin(SecurityUser user) {}

    default boolean isTokenAvailable() {
        return true;
    }

    String getToken(TenantId tenantId, UserId userId);

    default Map<String, String> getAdditionalInfo(TenantId tenantId, UserId userId) {
        return Map.of();
    }

}
