// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent.template;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.config.AgentAppConfigType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.sql.agent.AppTemplateRegistry;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.List;

@Service
@TbCoreComponent
@RequiredArgsConstructor
@Slf4j
public class DefaultTbAgentAppTemplateService implements TbAgentAppTemplateService {

    private final AppTemplateRegistry templateRegistry;

    @Override
    public List<AgentAppTemplate> findAll(TenantId tenantId) {
        return templateRegistry.all();
    }

    @Override
    public AgentAppTemplate findLatestByAppTypeAndConfigType(AgentApplicationType appType, AgentAppConfigType configType) {
        return templateRegistry.latest(appType, configType);
    }

    @Override
    public List<AgentAppTemplate> findByAppTypeAndConfigType(AgentApplicationType appType, AgentAppConfigType configType) {
        return templateRegistry.list(appType, configType);
    }

    @Override
    public AgentAppTemplate findByAppTypeAndConfigTypeAndCurrentVersion(AgentApplicationType appType,
                                                                        AgentAppConfigType configType,
                                                                        String currentVersion) {
        return templateRegistry.get(appType, configType, currentVersion);
    }
}
