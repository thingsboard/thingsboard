// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent.template;

import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.config.AgentAppConfigType;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.List;

public interface TbAgentAppTemplateService {

    List<AgentAppTemplate> findAll(TenantId tenantId);

    AgentAppTemplate findLatestByAppTypeAndConfigType(AgentApplicationType appType, AgentAppConfigType configType);

    List<AgentAppTemplate> findByAppTypeAndConfigType(AgentApplicationType appType, AgentAppConfigType configType);

    AgentAppTemplate findByAppTypeAndConfigTypeAndCurrentVersion(AgentApplicationType appType,
                                                                  AgentAppConfigType configType,
                                                                  String currentVersion);
}
