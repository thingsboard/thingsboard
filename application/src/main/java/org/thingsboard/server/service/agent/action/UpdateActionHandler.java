// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent.action;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentAppProfileService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.common.data.agent.AgentAppEventRequest;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.dao.agent.config.ProfileConfigResolver;

import java.util.Objects;

@Component
@TbCoreComponent
@RequiredArgsConstructor
public class UpdateActionHandler implements AgentAppActionHandler {

    private final ProfileConfigResolver profileConfigResolver;
    private final AgentAppProfileService profileService;

    @Override
    public AgentAppEventActionType getActionType() {
        return AgentAppEventActionType.UPDATE;
    }

    @Override
    public void handle(AgentApplication application, AgentAppEventRequest request, AgentAppActionContext ctx) {
        AgentApplication incoming = request.getApplication();
        if (application.getApplicationProfileId() != null) {
            // Profile-managed: by default re-resolve compose from the profile so any profile config drift is picked up.
            // When the caller asks to skip the refetch (credentials-only update), or the profile moved to another
            // template version (version moves are the UPGRADE flow's job), keep the existing compose and apply
            // just the incoming creds via setConfig.
            if (request.isSkipProfileRefetch() || isProfileTemplateDrifted(ctx.getTenantId(), application)) {
                if (incoming != null && incoming.getConfig() != null) {
                    application.setConfig(incoming.getConfig());
                }
            } else {
                profileConfigResolver.resolve(ctx.getTenantId(), application);
            }
            if (incoming != null && incoming.getName() != null && !incoming.getName().isBlank()) {
                application.setName(incoming.getName());
            }
            return;
        }
        if (incoming != null) {
            if (incoming.getName() != null && !incoming.getName().isBlank()) {
                application.setName(incoming.getName());
            }
            if (incoming.getConfig() != null) {
                application.setConfig(incoming.getConfig());
            }
        }
    }

    private boolean isProfileTemplateDrifted(TenantId tenantId, AgentApplication application) {
        AgentAppProfile profile = profileService.findProfileById(tenantId, application.getApplicationProfileId());
        return profile != null && !Objects.equals(profile.getTemplateVersion(), application.getTemplateVersion());
    }
}
