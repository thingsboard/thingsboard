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
package org.thingsboard.server.service.entitiy;

import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.List;

public interface TbNotificationEntityService {

    <E extends HasName, I extends EntityId> void notifyEntity(TenantId tenantId, I entityId, E entity, CustomerId customerId,
                                                              ActionType actionType, SecurityUser user, Exception e,
                                                              Object... additionalInfo);

    <E extends HasName, I extends EntityId> void notifyCreateOrUpdateEntity(TenantId tenantId, I entityId, E entity,
                                                                            CustomerId customerId, ActionType actionType,
                                                                            SecurityUser user, Object... additionalInfo);

    <E extends HasName, I extends EntityId> void notifyDeleteEntity(TenantId tenantId, I entityId, E entity,
                                                                    CustomerId customerId, ActionType actionType,
                                                                    List<EdgeId> relatedEdgeIds,
                                                                    SecurityUser user, Object... additionalInfo);

    void notifyDeleteAlarm(TenantId tenantId, Alarm alarm, EntityId originatorId,
                           CustomerId customerId, List<EdgeId> relatedEdgeIds,
                           SecurityUser user, String body, Object... additionalInfo);

    void notifyDeleteRuleChain(TenantId tenantId, RuleChain ruleChain,
                               List<EdgeId> relatedEdgeIds, SecurityUser user);

    <I extends EntityId> void notifySendMsgToEdgeService(TenantId tenantId, I entityId, EdgeEventActionType edgeEventActionType);

    <E extends HasName, I extends EntityId> void notifyAssignOrUnassignEntityToCustomer(TenantId tenantId, I entityId,
                                                                                        CustomerId customerId, E entity,
                                                                                        ActionType actionType,
                                                                                        EdgeEventActionType edgeActionType,
                                                                                        SecurityUser user, boolean sendToEdge,
                                                                                        Object... additionalInfo);

    <E extends HasName, I extends EntityId> void notifyAssignOrUnassignEntityToEdge(TenantId tenantId, I entityId,
                                                                                    CustomerId customerId, EdgeId edgeId,
                                                                                    E entity, ActionType actionType,
                                                                                    SecurityUser user, Object... additionalInfo);

    void notifyCreateOrUpdateTenant(Tenant tenant, ComponentLifecycleEvent event);


    void notifyDeleteTenant(Tenant tenant);

    void notifyCreateOrUpdateDevice(TenantId tenantId, DeviceId deviceId, CustomerId customerId, Device device,
                                    Device oldDevice, ActionType actionType, SecurityUser user, Object... additionalInfo);

    void notifyDeleteDevice(TenantId tenantId, DeviceId deviceId, CustomerId customerId, Device device,
                            List<EdgeId> relatedEdgeIds, SecurityUser user, Object... additionalInfo);

    void notifyUpdateDeviceCredentials(TenantId tenantId, DeviceId deviceId, CustomerId customerId, Device device,
                                       DeviceCredentials deviceCredentials, SecurityUser user);

    void notifyAssignDeviceToTenant(TenantId tenantId, TenantId newTenantId, DeviceId deviceId, CustomerId customerId,
                                    Device device, Tenant tenant, SecurityUser user, Object... additionalInfo);

    void notifyEdge(TenantId tenantId, EdgeId edgeId, CustomerId customerId, Edge edge, ActionType actionType,
                    SecurityUser user, Object... additionalInfo);

    void notifyCreateOrUpdateAlarm(Alarm alarm, ActionType actionType, SecurityUser user, Object... additionalInfo);

    <E extends HasName, I extends EntityId> void notifyCreateOrUpdateOrDelete(TenantId tenantId, CustomerId customerId,
                                                                              I entityId, E entity, SecurityUser user,
                                                                              ActionType actionType, boolean sendNotifyMsgToEdge, Exception e,
                                                                              Object... additionalInfo);

    void notifyCreateOrUpdateOrDeleteRelation(TenantId tenantId, CustomerId customerId,
                                              EntityRelation relation, SecurityUser user,
                                              ActionType actionType, Exception e,
                                              Object... additionalInfo);
}
