// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.service.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.agent.AgentProfile;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentProfileDao;
import org.thingsboard.server.dao.agent.AgentProfileService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.exception.DataValidationException;

@Component
public class AgentProfileDataValidator extends DataValidator<AgentProfile> {

    @Autowired
    private AgentProfileDao agentProfileDao;
    @Autowired
    @Lazy
    private AgentProfileService agentProfileService;
    @Autowired
    private TenantService tenantService;

    @Override
    protected AgentProfile validateUpdate(TenantId tenantId, AgentProfile agentProfile) {
        AgentProfile old = agentProfileDao.findById(agentProfile.getTenantId(), agentProfile.getId().getId());
        if (old == null) {
            throw new DataValidationException("Can't update non existing agent profile!");
        }
        return old;
    }

    @Override
    protected void validateDataImpl(TenantId tenantId, AgentProfile agentProfile) {
        validateString("Agent profile name", agentProfile.getName());
        if (agentProfile.getTenantId() == null) {
            throw new DataValidationException("Agent profile should be assigned to tenant!");
        }
        if (!tenantService.tenantExists(agentProfile.getTenantId())) {
            throw new DataValidationException("Agent profile is referencing to non-existent tenant!");
        }
        if (agentProfile.isDefault()) {
            AgentProfile defaultAgentProfile = agentProfileService.findDefaultAgentProfile(tenantId);
            if (defaultAgentProfile != null && !defaultAgentProfile.getId().equals(agentProfile.getId())) {
                throw new DataValidationException("Another default agent profile is present in scope of current tenant!");
            }
        }
    }
}
