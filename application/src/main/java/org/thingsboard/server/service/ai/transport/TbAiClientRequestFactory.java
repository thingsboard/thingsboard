// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai.transport;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.system.SystemSecurityService;

@Component
@TbCoreComponent
@RequiredArgsConstructor
public class TbAiClientRequestFactory {

    private final SystemSecurityService systemSecurityService;

    public TbAiClientRequest create(SecurityUser user, HttpServletRequest request) {
        String clientOrigin = systemSecurityService.getBaseUrl(user.getTenantId(), user.getCustomerId(), request);
        return TbAiClientRequest.of(clientOrigin, request);
    }

    // For calls without a browser request at hand; the origin only seeds a new channel's hello.
    public TbAiClientRequest forUser(SecurityUser user) {
        return TbAiClientRequest.withDefaultOrigin(
                systemSecurityService.getBaseUrl(user.getAuthority(), user.getTenantId(), user.getCustomerId(), null));
    }

    public TbAiClientRequest forTenant(TenantId tenantId) {
        return TbAiClientRequest.withDefaultOrigin(systemSecurityService.getBaseUrl(tenantId, null, null));
    }

}
