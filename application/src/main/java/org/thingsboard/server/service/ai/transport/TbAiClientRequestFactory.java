// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai.transport;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
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

}
