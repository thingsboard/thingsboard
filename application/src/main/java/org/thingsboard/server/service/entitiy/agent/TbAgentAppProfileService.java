// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.entitiy.agent;

import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.AppConfigMergeCtx;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.id.TenantId;

public interface TbAgentAppProfileService {

    AgentAppProfile save(AgentAppProfile profile, User currentUser) throws Exception;

    void delete(AgentAppProfile profile, User user);

    AgentAppProfile mergeForPreview(TenantId tenantId, AgentAppProfile appProfile, AppConfigMergeCtx ctx);

    AgentAppProfile createFromTemplate(TenantId tenantId, AgentAppTemplate template, String composeType, String baseUrl, User user) throws Exception;
}
