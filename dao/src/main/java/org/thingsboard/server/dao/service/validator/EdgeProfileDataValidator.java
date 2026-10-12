// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.service.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.edge.EdgeProfile;
import org.thingsboard.server.common.data.edge.EdgeProfileData;
import org.thingsboard.server.common.data.edge.EdgeProfileEntityGroup;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.scheduler.SchedulerEventInfo;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.edge.EdgeProfileDao;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.integration.IntegrationService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.scheduler.SchedulerEventService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.exception.DataValidationException;


@Component
public class EdgeProfileDataValidator extends DataValidator<EdgeProfile> {

    @Autowired
    private EdgeProfileDao edgeProfileDao;
    @Autowired
    private TenantService tenantService;
    @Autowired
    private CustomerDao customerDao;
    @Autowired
    @Lazy
    private RuleChainService ruleChainService;
    @Autowired
    @Lazy
    private EntityGroupService entityGroupService;
    @Autowired
    @Lazy
    private IntegrationService integrationService;
    @Autowired
    @Lazy
    private SchedulerEventService schedulerEventService;

    @Override
    protected EdgeProfile validateUpdate(TenantId tenantId, EdgeProfile profile) {
        EdgeProfile old = edgeProfileDao.findById(profile.getTenantId(), profile.getId().getId());
        if (old == null || !old.getTenantId().equals(tenantId)) {
            throw new DataValidationException("Can't update non existing edge profile!");
        }
        if (old.getRootRuleChainId() != null && profile.getRootRuleChainId() == null) {
            throw new DataValidationException("Edge profile root rule chain can be changed but not removed!");
        }
        return old;
    }

    @Override
    protected void validateDataImpl(TenantId tenantId, EdgeProfile profile) {
        validateString("Edge profile name", profile.getName());
        if (profile.getTenantId() == null) {
            throw new DataValidationException("Edge profile should be assigned to tenant!");
        }
        if (!tenantService.tenantExists(profile.getTenantId())) {
            throw new DataValidationException("Edge profile is referencing to non-existent tenant!");
        }
        if (profile.isDefault()) {
            EdgeProfile defaultProfile = edgeProfileDao.findDefaultEdgeProfile(tenantId.getId());
            if (defaultProfile != null && !defaultProfile.getId().equals(profile.getId())) {
                throw new DataValidationException("Another default edge profile is present in scope of current tenant!");
            }
        }
        EdgeProfileData data = profile.getProfileData();
        if (profile.getRootRuleChainId() != null && !data.getRuleChainIds().contains(profile.getRootRuleChainId())) {
            throw new DataValidationException("Edge profile root rule chain must be one of the assigned rule chains!");
        }
        for (RuleChainId ruleChainId : data.getRuleChainIds()) {
            RuleChain ruleChain = ruleChainService.findRuleChainById(tenantId, ruleChainId);
            if (ruleChain == null || !tenantId.equals(ruleChain.getTenantId())) {
                throw new DataValidationException("Edge profile is referencing to non-existent rule chain [" + ruleChainId + "]!");
            }
            if (!RuleChainType.EDGE.equals(ruleChain.getType())) {
                throw new DataValidationException("Edge profile can only assign EDGE rule chains, [" + ruleChain.getName() + "] is " + ruleChain.getType() + "!");
            }
        }
        for (EdgeProfileEntityGroup row : data.getEntityGroups()) {
            if (row.getGroupType() == null || !EdgeProfileData.SYNCABLE_GROUP_TYPES.contains(row.getGroupType())) {
                throw new DataValidationException("Edge profile entity group type must be one of " + EdgeProfileData.SYNCABLE_GROUP_TYPES + "!");
            }
            if (row.getEntityGroupId() == null) {
                throw new DataValidationException("Edge profile entity group id should be specified!");
            }
            EntityGroup group = entityGroupService.findEntityGroupById(tenantId, row.getEntityGroupId());
            if (group == null) {
                throw new DataValidationException("Edge profile is referencing to non-existent entity group [" + row.getEntityGroupId() + "]!");
            }
            if (!row.getGroupType().equals(group.getType())) {
                throw new DataValidationException("Entity group [" + group.getName() + "] is of type " + group.getType() + ", not " + row.getGroupType() + "!");
            }
            if (!isBelongToTenant(tenantId, group.getOwnerId())) {
                throw new DataValidationException("Edge profile can't reference entity group [" + group.getName() + "] from a different tenant!");
            }
            if (group.isGroupAll() || group.isEdgeGroupAll()) {
                throw new DataValidationException("'All' groups cannot be assigned through an edge profile!");
            }
        }
        for (IntegrationId integrationId : data.getIntegrationIds()) {
            Integration integration = integrationService.findIntegrationById(tenantId, integrationId);
            if (integration == null || !tenantId.equals(integration.getTenantId())) {
                throw new DataValidationException("Edge profile is referencing to non-existent integration [" + integrationId + "]!");
            }
            if (!integration.isEdgeTemplate()) {
                throw new DataValidationException("Edge profile can only assign edge template integrations, [" + integration.getName() + "] is not one!");
            }
        }
        for (SchedulerEventId schedulerEventId : data.getSchedulerEventIds()) {
            SchedulerEventInfo event = schedulerEventService.findSchedulerEventInfoById(tenantId, schedulerEventId);
            if (event == null || !tenantId.equals(event.getTenantId())) {
                throw new DataValidationException("Edge profile is referencing to non-existent scheduler event [" + schedulerEventId + "]!");
            }
        }
    }

    // entity groups have no tenantId; ownership is either the tenant itself or a customer belonging to it
    private boolean isBelongToTenant(TenantId tenantId, EntityId ownerId) {
        if (ownerId instanceof TenantId) {
            return ownerId.equals(tenantId);
        }
        if (ownerId instanceof CustomerId customerId) {
            Customer customer = customerDao.findById(tenantId, customerId.getId());
            return customer != null && tenantId.equals(customer.getTenantId());
        }
        return false;
    }

}
