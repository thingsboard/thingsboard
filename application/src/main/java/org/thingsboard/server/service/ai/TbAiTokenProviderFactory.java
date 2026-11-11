// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai;

import org.thingsboard.ai.common.client.TbAiClient;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.Map;

public final class TbAiTokenProviderFactory {

    private TbAiTokenProviderFactory() {}

    public static TbAiClient.TokenProvider userScoped(TbAiTokenProvider tokenProvider, SecurityUser user) {
        return new UserScopedTokenProvider(tokenProvider, user);
    }

    public static TbAiClient.TokenProvider idScoped(TbAiTokenProvider tokenProvider, TenantId tenantId, UserId userId) {
        return new IdScopedTokenProvider(tokenProvider, tenantId, userId);
    }

    private record UserScopedTokenProvider(TbAiTokenProvider tokenProvider, SecurityUser user) implements TbAiClient.TokenProvider {

        @Override
        public String getToken() {
            return tokenProvider.getToken(user);
        }

        @Override
        public Map<String, String> getAdditionalInfo() {
            return tokenProvider.getAdditionalInfo(user);
        }

    }

    private record IdScopedTokenProvider(TbAiTokenProvider tokenProvider, TenantId tenantId, UserId userId) implements TbAiClient.TokenProvider {

        @Override
        public String getToken() {
            return tokenProvider.getToken(tenantId, userId);
        }

        @Override
        public Map<String, String> getAdditionalInfo() {
            return tokenProvider.getAdditionalInfo(tenantId, userId);
        }

    }

}
