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
package org.thingsboard.server.service.entitiy.dashboard;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ShortCustomerInfo;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.thingsboard.server.service.entitiy.DefaultTbNotificationEntityService.edgeTypeByActionType;

@Service
@TbCoreComponent
@AllArgsConstructor
public class DefaultTbDashboardService extends AbstractTbEntityService implements TbDashboardService {

    @Override
    public Dashboard save(Dashboard dashboard, SecurityUser user) throws ThingsboardException {
        ActionType actionType = dashboard.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = dashboard.getTenantId();
        try {
            Dashboard savedDashboard = checkNotNull(dashboardService.saveDashboard(dashboard));
            autoCommit(user, savedDashboard.getId());
            notificationEntityService.notifyCreateOrUpdateEntity(tenantId, savedDashboard.getId(), savedDashboard,
                    null, actionType, user);
            return savedDashboard;
        } catch (Exception e) {
            notificationEntityService.notifyEntity(tenantId, emptyId(EntityType.DASHBOARD), dashboard, null, actionType, user, e);
            throw handleException(e);
        }
    }

    @Override
    public void delete(Dashboard dashboard, SecurityUser user) throws ThingsboardException {
        DashboardId dashboardId = dashboard.getId();
        TenantId tenantId = dashboard.getTenantId();
        try {
            List<EdgeId> relatedEdgeIds = findRelatedEdgeIds(tenantId, dashboardId);
            dashboardService.deleteDashboard(tenantId, dashboardId);
            notificationEntityService.notifyDeleteEntity(tenantId, dashboardId, dashboard, null,
                    ActionType.DELETED, relatedEdgeIds, user, dashboardId.toString());
        } catch (Exception e) {
            notificationEntityService.notifyEntity(tenantId, emptyId(EntityType.DASHBOARD), null, null,
                    ActionType.DELETED, user, e, dashboardId.toString());
            throw handleException(e);
        }
    }

    @Override
    public Dashboard assignDashboardToCustomer(DashboardId dashboardId, Customer customer, SecurityUser user) throws ThingsboardException {
        ActionType actionType = ActionType.ASSIGNED_TO_CUSTOMER;
        CustomerId customerId = customer.getId();
        try {
            Dashboard savedDashboard = checkNotNull(dashboardService.assignDashboardToCustomer(user.getTenantId(), dashboardId, customerId));
            notificationEntityService.notifyAssignOrUnassignEntityToCustomer(user.getTenantId(), dashboardId, customerId, savedDashboard,
                    actionType, edgeTypeByActionType(actionType), user, true, dashboardId.toString(), customerId.toString(), customer.getName());
            return savedDashboard;
        } catch (Exception e) {
            notificationEntityService.notifyEntity(user.getTenantId(), emptyId(EntityType.DASHBOARD), null, null,
                    actionType, user, e, dashboardId.toString(), customerId.toString());
            throw handleException(e);
        }
    }

    @Override
    public Dashboard assignDashboardToPublicCustomer(DashboardId dashboardId, SecurityUser user) throws ThingsboardException {
        ActionType actionType = ActionType.ASSIGNED_TO_CUSTOMER;
        try {
            Customer publicCustomer = customerService.findOrCreatePublicCustomer(user.getTenantId());
            Dashboard savedDashboard = checkNotNull(dashboardService.assignDashboardToCustomer(user.getTenantId(), dashboardId, publicCustomer.getId()));
            notificationEntityService.notifyAssignOrUnassignEntityToCustomer(user.getTenantId(), dashboardId, user.getCustomerId(), savedDashboard,
                    actionType, null, user, false, dashboardId.toString(),
                    publicCustomer.getId().toString(), publicCustomer.getName());
            return savedDashboard;
        } catch (Exception e) {
            notificationEntityService.notifyEntity(user.getTenantId(), emptyId(EntityType.DASHBOARD), null, null,
                    actionType, user, e, dashboardId.toString());
            throw handleException(e);
        }
    }

    @Override
    public Dashboard unassignDashboardFromPublicCustomer(Dashboard dashboard, SecurityUser user) throws ThingsboardException {
        ActionType actionType = ActionType.UNASSIGNED_FROM_CUSTOMER;
        try {
            Customer publicCustomer = customerService.findOrCreatePublicCustomer(dashboard.getTenantId());
            Dashboard savedDashboard = checkNotNull(dashboardService.unassignDashboardFromCustomer(user.getTenantId(), dashboard.getId(), publicCustomer.getId()));
            notificationEntityService.notifyAssignOrUnassignEntityToCustomer(user.getTenantId(), dashboard.getId(), user.getCustomerId(), savedDashboard,
                    actionType, null, user, false, dashboard.getId().toString(),
                    publicCustomer.getId().toString(), publicCustomer.getName());
            return savedDashboard;
        } catch (Exception e) {
            notificationEntityService.notifyEntity(user.getTenantId(), emptyId(EntityType.DASHBOARD), null, null,
                    actionType, user, e, dashboard.getId().toString());
            throw handleException(e);
        }
    }

    @Override
    public Dashboard updateDashboardCustomers(Dashboard dashboard, Set<CustomerId> customerIds, SecurityUser user) throws ThingsboardException {
        ActionType actionType = ActionType.ASSIGNED_TO_CUSTOMER;
        TenantId tenantId = user.getTenantId();
        try {
            Set<CustomerId> addedCustomerIds = new HashSet<>();
            Set<CustomerId> removedCustomerIds = new HashSet<>();
            for (CustomerId customerId : customerIds) {
                if (!dashboard.isAssignedToCustomer(customerId)) {
                    addedCustomerIds.add(customerId);
                }
            }

            Set<ShortCustomerInfo> assignedCustomers = dashboard.getAssignedCustomers();
            if (assignedCustomers != null) {
                for (ShortCustomerInfo customerInfo : assignedCustomers) {
                    if (!customerIds.contains(customerInfo.getCustomerId())) {
                        removedCustomerIds.add(customerInfo.getCustomerId());
                    }
                }
            }

            if (addedCustomerIds.isEmpty() && removedCustomerIds.isEmpty()) {
                return dashboard;
            } else {
                Dashboard savedDashboard = null;
                for (CustomerId customerId : addedCustomerIds) {
                    savedDashboard = checkNotNull(dashboardService.assignDashboardToCustomer(tenantId, dashboard.getId(), customerId));
                    ShortCustomerInfo customerInfo = savedDashboard.getAssignedCustomerInfo(customerId);
                    notificationEntityService.notifyAssignOrUnassignEntityToCustomer(tenantId, savedDashboard.getId(), customerId, savedDashboard,
                            actionType, edgeTypeByActionType(actionType), user, true, dashboard.getId().toString(),
                            customerId.toString(), customerInfo.getTitle());
                }
                actionType = ActionType.UNASSIGNED_FROM_CUSTOMER;
                for (CustomerId customerId : removedCustomerIds) {
                    ShortCustomerInfo customerInfo = dashboard.getAssignedCustomerInfo(customerId);
                    savedDashboard = checkNotNull(dashboardService.unassignDashboardFromCustomer(tenantId, dashboard.getId(), customerId));
                    notificationEntityService.notifyAssignOrUnassignEntityToCustomer(tenantId, savedDashboard.getId(), customerId, savedDashboard,
                            actionType, edgeTypeByActionType(actionType), user, true, dashboard.getId().toString(),
                            customerId.toString(), customerInfo.getTitle());
                }
                return savedDashboard;
            }
        } catch (Exception e) {
            notificationEntityService.notifyEntity(tenantId, emptyId(EntityType.DASHBOARD), null, null,
                    actionType, user, e, dashboard.getId().toString());
            throw handleException(e);
        }
    }

    @Override
    public Dashboard addDashboardCustomers(Dashboard dashboard, Set<CustomerId> customerIds, SecurityUser user) throws ThingsboardException {
        ActionType actionType = ActionType.ASSIGNED_TO_CUSTOMER;
        TenantId tenantId = user.getTenantId();
        try {
            if (customerIds.isEmpty()) {
                return dashboard;
            } else {
                Dashboard savedDashboard = null;
                for (CustomerId customerId : customerIds) {
                    savedDashboard = checkNotNull(dashboardService.assignDashboardToCustomer(tenantId, dashboard.getId(), customerId));
                    ShortCustomerInfo customerInfo = savedDashboard.getAssignedCustomerInfo(customerId);
                    notificationEntityService.notifyAssignOrUnassignEntityToCustomer(tenantId, savedDashboard.getId(), customerId, savedDashboard,
                            actionType, edgeTypeByActionType(actionType), user, true, dashboard.getId().toString(),
                            customerId.toString(), customerInfo.getTitle());
                }
                return savedDashboard;
            }
        } catch (Exception e) {
            notificationEntityService.notifyEntity(tenantId, emptyId(EntityType.DASHBOARD), null, null,
                    actionType, user, e, dashboard.getId().toString());
            throw handleException(e);
        }
    }

    @Override
    public Dashboard removeDashboardCustomers(Dashboard dashboard, Set<CustomerId> customerIds, SecurityUser user) throws ThingsboardException {
        ActionType actionType = ActionType.UNASSIGNED_FROM_CUSTOMER;
        TenantId tenantId = user.getTenantId();
        try {
             if (customerIds.isEmpty()) {
                return dashboard;
            } else {
                Dashboard savedDashboard = null;
                for (CustomerId customerId : customerIds) {
                    ShortCustomerInfo customerInfo = dashboard.getAssignedCustomerInfo(customerId);
                    savedDashboard = checkNotNull(dashboardService.unassignDashboardFromCustomer(tenantId, dashboard.getId(), customerId));
                    notificationEntityService.notifyAssignOrUnassignEntityToCustomer(tenantId, savedDashboard.getId(), customerId, savedDashboard,
                            actionType, edgeTypeByActionType(actionType), user, true, dashboard.getId().toString(),
                            customerId.toString(), customerInfo.getTitle());
                }
                return savedDashboard;
            }
        } catch (Exception e) {
            notificationEntityService.notifyEntity(tenantId, emptyId(EntityType.DASHBOARD), null, null,
                    actionType, user, e, dashboard.getId().toString());
            throw handleException(e);
        }
    }

    @Override
    public Dashboard assignDashboardToEdge(DashboardId dashboardId, Edge edge, SecurityUser user) throws ThingsboardException {
        ActionType actionType = ActionType.ASSIGNED_TO_EDGE;
        TenantId tenantId = user.getTenantId();
        EdgeId edgeId = edge.getId();
        try {
            Dashboard savedDashboard = checkNotNull(dashboardService.assignDashboardToEdge(tenantId, dashboardId, edgeId));
            notificationEntityService.notifyAssignOrUnassignEntityToEdge(tenantId, dashboardId, user.getCustomerId(),
                    edgeId, savedDashboard, actionType, user, dashboardId.toString(),
                    edgeId.toString(), edge.getName());
            return savedDashboard;
        } catch (Exception e) {
            notificationEntityService.notifyEntity(tenantId, emptyId(EntityType.DEVICE), null, null,
                    actionType, user, e, dashboardId.toString(), edgeId.toString());
            throw handleException(e);
        }
    }

    @Override
    public Dashboard unassignDashboardFromEdge(Dashboard dashboard, Edge edge, SecurityUser user) throws ThingsboardException {
        ActionType actionType = ActionType.UNASSIGNED_FROM_EDGE;
        TenantId tenantId = dashboard.getTenantId();
        DashboardId dashboardId = dashboard.getId();
        EdgeId edgeId = edge.getId();
        try {
            Dashboard savedDevice = checkNotNull(dashboardService.unassignDashboardFromEdge(tenantId, dashboardId, edgeId));

            notificationEntityService.notifyAssignOrUnassignEntityToEdge(tenantId, dashboardId, user.getCustomerId(),
                    edgeId, dashboard, actionType, user, dashboardId.toString(),
                    edgeId.toString(), edge.getName());
            return savedDevice;
        } catch (Exception e) {
            notificationEntityService.notifyEntity(tenantId, emptyId(EntityType.DASHBOARD), null, null,
                    actionType, user, e, dashboardId.toString(), edgeId.toString());
            throw handleException(e);
        }
    }

    @Override
    public Dashboard unassignDashboardFromCustomer(Dashboard dashboard, Customer customer, SecurityUser user) throws ThingsboardException {
        ActionType actionType = ActionType.UNASSIGNED_FROM_CUSTOMER;
        TenantId tenantId = dashboard.getTenantId();
        try {
            Dashboard savedDashboard = checkNotNull(dashboardService.unassignDashboardFromCustomer(tenantId, dashboard.getId(), customer.getId()));
            notificationEntityService.notifyAssignOrUnassignEntityToCustomer(tenantId, dashboard.getId(), customer.getId(), savedDashboard,
                    actionType, edgeTypeByActionType(actionType), user, true, dashboard.getId().toString(), customer.getId().toString(),
                    customer.getName());
            return savedDashboard;
        } catch (Exception e) {
            notificationEntityService.notifyEntity(tenantId, emptyId(EntityType.DASHBOARD), null, null,
                    actionType, user, e, dashboard.getId().toString());
            throw handleException(e);
        }
    }

}
