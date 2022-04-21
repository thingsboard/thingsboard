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
package org.thingsboard.server.service.entity;

import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rule.RuleChain;

public interface EntityDeleteService {

    void  deleteEntity(TenantId tenantId, EntityId entityId)  throws ThingsboardException;

    Boolean  deleteAlarm( AlarmId alarmId)  throws ThingsboardException;

    Asset deleteUnassignAsset(AssetId assetId)  throws ThingsboardException;

    Asset deleteUnassignAsset(AssetId assetId, EdgeId edgeId)  throws ThingsboardException;

    Dashboard deleteUnassignDashboard(DashboardId dashboardId)  throws ThingsboardException;

    Dashboard deleteUnassignDashboard(DashboardId dashboardId, CustomerId customerId)  throws ThingsboardException;

    Dashboard deleteUnassignDashboard(DashboardId dashboardId, EdgeId edgeId)  throws ThingsboardException;

    Device deleteUnassignDevice(DeviceId deviceId)  throws ThingsboardException;

    Device deleteUnassignDevice(DeviceId deviceId, EdgeId edgeId)  throws ThingsboardException;

    DeferredResult<ResponseEntity> deleteReClaimDevice(String deviceName)  throws ThingsboardException;

    Edge deleteUnassignEdge(EdgeId edgeId)  throws ThingsboardException;

    void  deleteRelation(EntityId fromId, EntityId toId, String strRelationType, String strRelationTypeGroup)  throws ThingsboardException;

    void  deleteRelations(EntityId entityId)  throws ThingsboardException;

    EntityView deleteUnassignEntityView(EntityViewId entityViewId)  throws ThingsboardException;

    EntityView deleteUnassignEntityView(EntityViewId entityViewId, EdgeId edgeId)  throws ThingsboardException;

    RuleChain deleteUnsetAutoAssignToEdgeRuleChain(RuleChainId ruleChainId)  throws ThingsboardException;

    RuleChain deleteUnassignRuleChain(RuleChainId ruleChainId, EdgeId edgeId)  throws ThingsboardException;
}
