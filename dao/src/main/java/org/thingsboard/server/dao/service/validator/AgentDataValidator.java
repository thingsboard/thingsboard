// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.service.validator;

import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentDao;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.subscription.SubscriptionService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.exception.DataValidationException;

import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

@Component
@AllArgsConstructor
public class AgentDataValidator extends DataValidator<Agent> {

    private final AgentDao agentDao;
    private final CustomerDao customerDao;
    private final TenantService tenantService;

    @Lazy
    private final SubscriptionService subscriptionService;

    @Override
    protected void validateCreate(TenantId tenantId, Agent agent) {
        subscriptionService.createAgentAllowed(agent.getTenantId());
        validateNumberOfEntitiesPerTenant(tenantId, EntityType.AGENT);
    }

    @Override
    protected Agent validateUpdate(TenantId tenantId, Agent agent) {
        Agent old = agentDao.findById(agent.getTenantId(), agent.getId().getId());
        if (old == null) {
            throw new DataValidationException("Can't update non existing agent!");
        }
        return old;
    }

    @Override
    protected void validateDataImpl(TenantId tenantId, Agent agent) {
        validateString("Agent name", agent.getName());
        if (StringUtils.isEmpty(agent.getRoutingKey())) {
            throw new DataValidationException("Agent routing key should be specified!");
        }
        if (StringUtils.isEmpty(agent.getSecret())) {
            throw new DataValidationException("Agent secret should be specified!");
        }
        if (agent.getTenantId() == null) {
            throw new DataValidationException("Agent should be assigned to tenant!");
        } else {
            if (!tenantService.tenantExists(agent.getTenantId())) {
                throw new DataValidationException("Agent is referencing to non-existent tenant!");
            }
        }
        if (agent.getCustomerId() == null) {
            agent.setCustomerId(new CustomerId(NULL_UUID));
        } else if (!agent.getCustomerId().getId().equals(NULL_UUID)) {
            Customer customer = customerDao.findById(tenantId, agent.getCustomerId().getId());
            if (customer == null) {
                throw new DataValidationException("Can't assign agent to non-existent customer!");
            }
            if (!customer.getTenantId().equals(agent.getTenantId())) {
                throw new DataValidationException("Can't assign agent to customer from different tenant!");
            }
        }
    }
}
