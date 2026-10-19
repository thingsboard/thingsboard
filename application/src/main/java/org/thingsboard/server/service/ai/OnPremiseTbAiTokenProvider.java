// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.ai.common.client.TbAiHeaders;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.dao.subscription.SubscriptionService;
import org.thingsboard.server.exception.ThingsboardRuntimeException;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.install.ProjectInfo;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

// The handshake carries no Origin: the origin TB AI uses comes from the channel's hello and start frames.
@Component
@TbCoreComponent
@RequiredArgsConstructor
@ConditionalOnExpression("'${ai.jwt.signing_key:}' == ''")
class OnPremiseTbAiTokenProvider implements TbAiTokenProvider {

    private final SubscriptionService subscriptionService;
    private final ProjectInfo projectInfo;

    @Override
    public String getToken(SecurityUser user) {
        return requireAiToken(() -> new ThingsboardRuntimeException("AI is not available for the current subscription plan", ThingsboardErrorCode.PERMISSION_DENIED));
    }

    @Override
    public Map<String, String> getAdditionalInfo(SecurityUser user) {
        return additionalInfo(user.getUuidId(), user.getTenantId());
    }

    @Override
    public boolean isTokenAvailable() {
        return subscriptionService.getAiToken() != null;
    }

    @Override
    public String getToken(TenantId tenantId, UserId userId) {
        return requireAiToken(() -> new IllegalStateException("AI token is not available"));
    }

    @Override
    public Map<String, String> getAdditionalInfo(TenantId tenantId, UserId userId) {
        return additionalInfo(userId.getId(), tenantId);
    }

    private String requireAiToken(Supplier<RuntimeException> onMissingToken) {
        String token = subscriptionService.getAiToken();
        if (token == null) {
            throw onMissingToken.get();
        }
        return token;
    }

    private Map<String, String> additionalInfo(UUID userId, TenantId tenantId) {
        return Map.of(
                TbAiHeaders.USER_ID, userId.toString(),
                TbAiHeaders.TENANT_ID, tenantId.getId().toString(),
                TbAiHeaders.TB_VERSION, projectInfo.getProjectVersion()
        );
    }

}
