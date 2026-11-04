// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.entitiy.edge;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

import java.util.Collections;
import java.util.List;

@AllArgsConstructor
@TbCoreComponent
@Service
@Slf4j
public class DefaultTbEdgeService extends AbstractTbEntityService implements TbEdgeService {

    private final RuleChainService ruleChainService;

    @Override
    public Edge save(Edge edge, RuleChain edgeTemplateRootRuleChain, EntityGroup entityGroup, User user) throws Exception {
        return save(edge, edgeTemplateRootRuleChain, entityGroup != null ? Collections.singletonList(entityGroup) : Collections.emptyList(), user);
    }

    @Override
    public Edge save(Edge edge, RuleChain edgeTemplateRootRuleChain, List<EntityGroup> entityGroups, User user) throws Exception {
        if (entityGroups == null) {
            entityGroups = Collections.emptyList();
        }
        ActionType actionType = edge.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = edge.getTenantId();
        try {
            String oldEdgeName = null;
            if (ActionType.UPDATED.equals(actionType)) {
                Edge edgeById = edgeService.findEdgeById(tenantId, edge.getId());
                if (edgeById != null) {
                    oldEdgeName = edgeById.getName();
                }
            }
            if (ActionType.ADDED.equals(actionType) && edge.getRootRuleChainId() == null) {
                edge.setRootRuleChainId(edgeTemplateRootRuleChain.getId());
            }
            Edge savedEdge = checkNotNull(edgeService.saveEdge(edge));
            EdgeId edgeId = savedEdge.getId();

            if (!entityGroups.isEmpty() && ActionType.ADDED.equals(actionType)) {
                for (EntityGroup entityGroup : entityGroups) {
                    entityGroupService.addEntityToEntityGroup(tenantId, entityGroup.getId(), edgeId);
                }
            }

            if (ActionType.ADDED.equals(actionType)) {
                ruleChainService.assignRuleChainToEdge(tenantId, edgeTemplateRootRuleChain.getId(), edgeId);
                savedEdge = edgeService.setEdgeRootRuleChain(tenantId, savedEdge, edgeTemplateRootRuleChain.getId());
                edgeService.assignDefaultRuleChainsToEdge(tenantId, savedEdge.getId());
                edgeService.assignTenantAdministratorsAndUsersGroupToEdge(tenantId, savedEdge.getId());
                if (EntityType.CUSTOMER.equals(edge.getOwnerId().getEntityType())) {
                    Customer customerById = customerService.findCustomerById(tenantId, new CustomerId(edge.getOwnerId().getId()));
                    edgeService.assignCustomerAdministratorsAndUsersGroupToEdge(tenantId, savedEdge.getId(), customerById.getId(), customerById.getParentCustomerId());
                }
            }

            if (oldEdgeName != null && !oldEdgeName.equals(savedEdge.getName())) {
                String customerName = null;
                if (EntityType.CUSTOMER.equals(edge.getOwnerId().getEntityType())) {
                    Customer customer = customerService.findCustomerById(tenantId, new CustomerId(edge.getOwnerId().getId()));
                    customerName = customer.getName();
                }
                edgeService.renameEdgeAllGroups(tenantId, savedEdge, oldEdgeName, customerName, customerName);
            }

            logEntityActionService.logEntityAction(tenantId, edgeId, savedEdge, savedEdge.getCustomerId(), actionType, user);

            return savedEdge;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.EDGE), edge, actionType, user, e);
            throw e;
        }
    }

    @Override
    public void delete(EdgeId edgeId, User user) {
        Edge edge = edgeService.findEdgeById(user.getTenantId(), edgeId);
        this.delete(edge, user);
    }

    @Override
    public void delete(Edge edge, User user) {
        ActionType actionType = ActionType.DELETED;
        EdgeId edgeId = edge.getId();
        TenantId tenantId = edge.getTenantId();
        try {
            edgeService.deleteEdge(tenantId, edgeId);
            logEntityActionService.logEntityAction(tenantId, edgeId, edge, edge.getCustomerId(), actionType, user, edgeId.toString());
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.EDGE), actionType,
                    user, e, edgeId.toString());
            throw e;
        }
    }

    @Override
    public Edge setEdgeRootRuleChain(Edge edge, RuleChainId ruleChainId, User user) throws Exception {
        TenantId tenantId = edge.getTenantId();
        EdgeId edgeId = edge.getId();
        try {
            Edge updatedEdge = edgeService.setEdgeRootRuleChain(tenantId, edge, ruleChainId);
            logEntityActionService.logEntityAction(tenantId, edgeId, edge, null, ActionType.UPDATED, user);
            return updatedEdge;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.EDGE),
                    ActionType.UPDATED, user, e, edgeId.toString());
            throw e;
        }
    }

}
