/**
 * Copyright © 2016-2022 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.entitiy.edge;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;
import org.thingsboard.server.service.security.model.SecurityUser;

@AllArgsConstructor
@TbCoreComponent
@Service
@Slf4j
public class DefaultTbEdgeService extends AbstractTbEntityService implements TbEdgeService {

    @Override
    public Edge saveEdge(Edge edge, RuleChain edgeTemplateRootRuleChain, SecurityUser user) throws ThingsboardException {
        ActionType actionType = edge.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = edge.getTenantId();
        try {
            Edge savedEdge = checkNotNull(edgeService.saveEdge(edge));
            EdgeId edgeId = savedEdge.getId();

            if (actionType == ActionType.ADDED) {
                ruleChainService.assignRuleChainToEdge(tenantId, edgeTemplateRootRuleChain.getId(), edgeId);
                edgeNotificationService.setEdgeRootRuleChain(tenantId, savedEdge, edgeTemplateRootRuleChain.getId());
                edgeService.assignDefaultRuleChainsToEdge(tenantId, edgeId);
            }

            notificationEntityService.notifyEdge(tenantId, edgeId, savedEdge.getCustomerId(), savedEdge, actionType, user);

            return savedEdge;
        } catch (Exception e) {
            notificationEntityService.notifyEntity(tenantId, emptyId(EntityType.EDGE), edge, null, actionType, user, e);
            throw handleException(e);
        }
    }

    @Override
    public void deleteEdge(Edge edge, SecurityUser user) throws ThingsboardException {
        ActionType actionType = ActionType.DELETED;
        EdgeId edgeId = edge.getId();
        TenantId tenantId = edge.getTenantId();
        try {
            edgeService.deleteEdge(tenantId, edgeId);
            notificationEntityService.notifyEdge(tenantId, edgeId, edge.getCustomerId(), edge, actionType, user, edgeId.toString());

        } catch (Exception e) {
            notificationEntityService.notifyEntity(tenantId, emptyId(EntityType.EDGE), edge, null, actionType, user, e);
            throw handleException(e);
        }
    }

    @Override
    public Edge assignEdgeToCustomer(TenantId tenantId, EdgeId edgeId, Customer customer, SecurityUser user) throws ThingsboardException {
        ActionType actionType = ActionType.ASSIGNED_TO_CUSTOMER;
        CustomerId customerId = customer.getId();
        try {
            Edge savedEdge = checkNotNull(edgeService.assignEdgeToCustomer(tenantId, edgeId, customerId));

            notificationEntityService.notifyEdge(tenantId, edgeId, customerId, savedEdge, actionType, user,
                    edgeId.toString(), customerId.toString(), customer.getName());

            return savedEdge;
        } catch (Exception e) {
            notificationEntityService.notifyEntity(tenantId, emptyId(EntityType.EDGE), null, null,
                    actionType, user, e, edgeId.toString(), customerId.toString());
            throw handleException(e);
        }
    }

    @Override
    public Edge unassignEdgeFromCustomer(Edge edge, Customer customer, SecurityUser user) throws ThingsboardException {
        ActionType actionType = ActionType.UNASSIGNED_FROM_CUSTOMER;
        TenantId tenantId = edge.getTenantId();
        EdgeId edgeId = edge.getId();
        CustomerId customerId = customer.getId();
        try {
            Edge savedEdge = checkNotNull(edgeService.unassignEdgeFromCustomer(tenantId, edgeId));

            notificationEntityService.notifyEdge(tenantId, edgeId, customerId, savedEdge, actionType, user,
                    edgeId.toString(), customerId.toString(), customer.getName());
            return savedEdge;
        } catch (Exception e) {
            notificationEntityService.notifyEntity(tenantId, emptyId(EntityType.EDGE), null, null,
                    actionType, user, e, edgeId.toString());
            throw handleException(e);
        }
    }

    @Override
    public Edge assignEdgeToPublicCustomer(TenantId tenantId, EdgeId edgeId, SecurityUser user) throws ThingsboardException {
        ActionType actionType = ActionType.ASSIGNED_TO_CUSTOMER;
        try {
            Customer publicCustomer = customerService.findOrCreatePublicCustomer(tenantId);
            CustomerId customerId = publicCustomer.getId();
            Edge savedEdge = checkNotNull(edgeService.assignEdgeToCustomer(tenantId, edgeId, customerId));

            notificationEntityService.notifyEdge(tenantId, edgeId, customerId, savedEdge, actionType, user,
                    edgeId.toString(), customerId.toString(), publicCustomer.getName());

            return savedEdge;
        } catch (Exception e) {
            notificationEntityService.notifyEntity(tenantId, emptyId(EntityType.EDGE), null, null,
                    actionType, user, e, edgeId.toString());
            throw handleException(e);
        }
    }

    @Override
    public Edge setEdgeRootRuleChain(Edge edge, RuleChainId ruleChainId, SecurityUser user) throws ThingsboardException {
        ActionType actionType = ActionType.UPDATED;
        TenantId tenantId = edge.getTenantId();
        EdgeId edgeId = edge.getId();
        try {
            Edge updatedEdge = edgeNotificationService.setEdgeRootRuleChain(tenantId, edge, ruleChainId);
            notificationEntityService.notifyEdge(tenantId, edgeId, null, updatedEdge, actionType, user);
            return updatedEdge;
        } catch (Exception e) {
            notificationEntityService.notifyEntity(tenantId, emptyId(EntityType.EDGE), null, null,
                    actionType, user, e, edgeId.toString());
            throw handleException(e);
        }
    }
}
