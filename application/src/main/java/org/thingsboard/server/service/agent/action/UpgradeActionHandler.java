// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent.action;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentAppEventRequest;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.dao.agent.config.ProfileConfigResolver;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.queue.util.TbCoreComponent;

@Component
@TbCoreComponent
@RequiredArgsConstructor
public class UpgradeActionHandler implements AgentAppActionHandler {

    private final ProfileConfigResolver profileConfigResolver;

    @Override
    public AgentAppEventActionType getActionType() {
        return AgentAppEventActionType.UPGRADE;
    }

    @Override
    public void handle(AgentApplication application, AgentAppEventRequest request, AgentAppActionContext ctx) {
        AgentApplication upgradedApp = request.getApplication();
        if (upgradedApp == null) {
            throw new DataValidationException("Upgrade request must include an application");
        }
        if (application.getApplicationProfileId() != null) {
            var profile = profileConfigResolver.resolve(ctx.getTenantId(), application);
            application.setDesiredTemplateVersion(profile.getTemplateVersion());
        } else if (upgradedApp.getConfig() != null) {
            if (StringUtils.isBlank(upgradedApp.getTemplateVersion())) {
                throw new DataValidationException("Upgrade request must include a target template version for an application without an application profile");
            }
            application.setConfig(upgradedApp.getConfig());
            application.setDesiredTemplateVersion(upgradedApp.getTemplateVersion());
        } else {
            throw new DataValidationException("Upgrade request must include a config for an application without an application profile");
        }
    }
}
