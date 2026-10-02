// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.thingsboard.ai.common.client.TbAiHeaders;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.dao.subscription.SubscriptionService;
import org.thingsboard.server.exception.ThingsboardRuntimeException;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.install.ProjectInfo;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.system.SystemSecurityService;

import java.net.URI;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

@Component
@TbCoreComponent
@ConditionalOnExpression("'${ai.jwt.signing_key:}' == ''")
class OnPremiseTbAiTokenProvider implements TbAiTokenProvider {

    private final SubscriptionService subscriptionService;
    private final SystemSecurityService systemSecurityService;
    private final ProjectInfo projectInfo;
    private final String coreBaseUrl;

    OnPremiseTbAiTokenProvider(SubscriptionService subscriptionService,
                              SystemSecurityService systemSecurityService,
                              ProjectInfo projectInfo,
                              @Value("${TB_CORE_BASE_URL:}") String coreBaseUrl) {
        this.subscriptionService = subscriptionService;
        this.systemSecurityService = systemSecurityService;
        this.projectInfo = projectInfo;
        this.coreBaseUrl = coreBaseUrl;
    }

    @Override
    public String getToken(SecurityUser user) {
        return requireAiToken(() -> new ThingsboardRuntimeException("AI is not available for the current subscription plan", ThingsboardErrorCode.PERMISSION_DENIED));
    }

    @Override
    public Map<String, String> getAdditionalInfo(SecurityUser user) {
        return additionalInfo(user.getUuidId(), user.getTenantId(), resolveOrigin(user));
    }

    @Override
    public void validateCallbackOrigin(SecurityUser user) {
        if ("localhost".equals(URI.create(resolveOrigin(user)).getHost())) {
            throw new ThingsboardRuntimeException("Please configure the base URL under Login white labeling or General settings at the sysadmin level", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
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
        // Background deletion tasks only need the Origin header to be present; the AI service never calls back
        // to ThingsBoard for a delete. Skip TB_CORE_BASE_URL and the white-labeling chain (the tenant may already
        // be gone by the time the task runs) and take the system-level general settings base URL instead.
        String origin = systemSecurityService.getBaseUrl(tenantId, null, null);
        return additionalInfo(userId.getId(), tenantId, origin);
    }

    private String requireAiToken(Supplier<RuntimeException> onMissingToken) {
        String token = subscriptionService.getAiToken();
        if (token == null) {
            throw onMissingToken.get();
        }
        return token;
    }

    private String resolveOrigin(SecurityUser user) {
        if (StringUtils.isNotEmpty(coreBaseUrl)) {
            return coreBaseUrl;
        }
        return systemSecurityService.getBaseUrl(user.getAuthority(), user.getTenantId(), user.getCustomerId(), null);
    }

    private Map<String, String> additionalInfo(UUID userId, TenantId tenantId, String origin) {
        return Map.of(
                TbAiHeaders.USER_ID, userId.toString(),
                TbAiHeaders.TENANT_ID, tenantId.getId().toString(),
                TbAiHeaders.TB_VERSION, projectInfo.getProjectVersion(),
                HttpHeaders.ORIGIN, origin
        );
    }

}
