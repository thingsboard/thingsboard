// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.service.validator;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.agent.AgentAppUnit;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentAppUnitDao;
import org.thingsboard.server.dao.agent.AgentApplicationService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.exception.DataValidationException;

@Component
@AllArgsConstructor
public class AgentAppUnitDataValidator extends DataValidator<AgentAppUnit> {

    private final AgentApplicationService agentApplicationService;
    private final AgentAppUnitDao agentAppUnitDao;

    @Override
    protected AgentAppUnit validateUpdate(TenantId tenantId, AgentAppUnit agentAppUnit) {
        AgentAppUnit old = agentAppUnitDao.findById(tenantId, agentAppUnit.getId().getId());
        if (old == null) {
            throw new DataValidationException("Can't update non existing agent app unit!");
        }
        return old;
    }

    @Override
    protected void validateDataImpl(TenantId tenantId, AgentAppUnit agentAppUnit) {
        if (agentAppUnit.getAgentApplicationId() == null) {
            throw new DataValidationException("Agent app unit should be assigned to agent application!");
        }
        AgentApplication application = agentApplicationService.findById(tenantId, agentAppUnit.getAgentApplicationId());
        if (application == null) {
            throw new DataValidationException("Agent app unit is referencing non-existent agent application!");
        }
        if (StringUtils.isBlank(agentAppUnit.getIdentifier())) {
            throw new DataValidationException("Agent app unit identifier is required!");
        }
        if (agentAppUnit.getType() == null) {
            throw new DataValidationException("Agent app unit type is required!");
        }
    }
}
