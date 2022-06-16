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
package org.thingsboard.server.service.sync.ie.importing.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ShortCustomerInfo;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.sync.vc.data.EntitiesImportCtx;
import org.thingsboard.server.utils.RegexUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DashboardImportService extends BaseEntityImportService<DashboardId, Dashboard, EntityExportData<Dashboard>> {

    private static final LinkedHashSet<EntityType> HINTS = new LinkedHashSet<>(Arrays.asList(EntityType.DASHBOARD, EntityType.DEVICE, EntityType.ASSET));

    private final DashboardService dashboardService;


    @Override
    protected void setOwner(TenantId tenantId, Dashboard dashboard, IdProvider idProvider) {
        dashboard.setTenantId(tenantId);
    }

    @Override
    protected Dashboard findExistingEntity(EntitiesImportCtx ctx, Dashboard dashboard, IdProvider idProvider) {
        Dashboard existingDashboard = super.findExistingEntity(ctx, dashboard, idProvider);
        if (existingDashboard == null && ctx.isFindExistingByName()) {
            existingDashboard = dashboardService.findTenantDashboardsByTitle(ctx.getTenantId(), dashboard.getName()).stream().findFirst().orElse(null);
        }
        return existingDashboard;
    }

    @Override
    protected Dashboard prepareAndSave(EntitiesImportCtx ctx, Dashboard dashboard, Dashboard old, EntityExportData<Dashboard> exportData, IdProvider idProvider) {
        var tenantId = ctx.getTenantId();
        JsonNode configuration = dashboard.getConfiguration();
        JsonNode entityAliases = configuration.get("entityAliases");
        if (entityAliases != null && entityAliases.isObject()) {
            for (JsonNode entityAlias : entityAliases) {
                ArrayList<String> fields = Lists.newArrayList(entityAlias.fieldNames());
                for (String field : fields) {
                    if (field.equals("id")) continue;
                    JsonNode oldFieldValue = entityAlias.get(field);
                    JsonNode newFieldValue = JacksonUtil.toJsonNode(RegexUtils.replace(oldFieldValue.toString(), RegexUtils.UUID_PATTERN, uuid -> {
                        return idProvider.getInternalIdByUuid(UUID.fromString(uuid), ctx.isFetchAllUUIDs(), HINTS)
                                .map(entityId -> entityId.getId().toString()).orElse(uuid);
                    }));
                    ((ObjectNode) entityAlias).set(field, newFieldValue);
                }
            }
        }

        Set<ShortCustomerInfo> assignedCustomers = Optional.ofNullable(dashboard.getAssignedCustomers()).orElse(Collections.emptySet()).stream()
                .peek(customerInfo -> customerInfo.setCustomerId(idProvider.getInternalId(customerInfo.getCustomerId())))
                .collect(Collectors.toSet());

        if (dashboard.getId() == null) {
            dashboard.setAssignedCustomers(null);
            dashboard = dashboardService.saveDashboard(dashboard);
            for (ShortCustomerInfo customerInfo : assignedCustomers) {
                dashboard = dashboardService.assignDashboardToCustomer(tenantId, dashboard.getId(), customerInfo.getCustomerId());
            }
        } else {
            Set<CustomerId> existingAssignedCustomers = Optional.ofNullable(dashboardService.findDashboardById(tenantId, dashboard.getId()).getAssignedCustomers())
                    .orElse(Collections.emptySet()).stream().map(ShortCustomerInfo::getCustomerId).collect(Collectors.toSet());
            Set<CustomerId> newAssignedCustomers = assignedCustomers.stream().map(ShortCustomerInfo::getCustomerId).collect(Collectors.toSet());

            Set<CustomerId> toUnassign = new HashSet<>(existingAssignedCustomers);
            toUnassign.removeAll(newAssignedCustomers);
            for (CustomerId customerId : toUnassign) {
                assignedCustomers = dashboardService.unassignDashboardFromCustomer(tenantId, dashboard.getId(), customerId).getAssignedCustomers();
            }

            Set<CustomerId> toAssign = new HashSet<>(newAssignedCustomers);
            toAssign.removeAll(existingAssignedCustomers);
            for (CustomerId customerId : toAssign) {
                assignedCustomers = dashboardService.assignDashboardToCustomer(tenantId, dashboard.getId(), customerId).getAssignedCustomers();
            }

            dashboard.setAssignedCustomers(assignedCustomers);
            dashboard = dashboardService.saveDashboard(dashboard);
        }
        return dashboard;
    }

    @Override
    protected void onEntitySaved(SecurityUser user, Dashboard savedDashboard, Dashboard oldDashboard) throws ThingsboardException {
        super.onEntitySaved(user, savedDashboard, oldDashboard);
        if (oldDashboard != null) {
            entityActionService.sendEntityNotificationMsgToEdgeService(user.getTenantId(), savedDashboard.getId(), EdgeEventActionType.UPDATED);
        }
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.DASHBOARD;
    }

}
