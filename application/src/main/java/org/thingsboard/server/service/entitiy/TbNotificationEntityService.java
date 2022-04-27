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
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.service.security.model.SecurityUser;

public interface TbNotificationEntityService {

    <E extends HasName, I extends EntityId> void sendNotification(TenantId tenantId, I entityId, E entity, CustomerId customerId,
                                                                  ActionType actionType, SecurityUser user, Exception e,
                                                                  Object... additionalInfo);

    void notifyCreateOrUpdateDevice(TenantId tenantId, DeviceId deviceId, CustomerId customerId, Device device,
                                    Device oldDevice, ActionType actionType, SecurityUser user, Object... additionalInfo);

    void notifyDeleteDevice(TenantId tenantId, DeviceId deviceId, CustomerId customerId, Device device,
                            SecurityUser user, Object... additionalInfo);

    void notifyAssignOrUnassignDeviceToCustomer(TenantId tenantId, DeviceId deviceId, CustomerId customerId,
                                                Device device, ActionType actionType, EdgeEventActionType edgeActionType,
                                                SecurityUser user, boolean sendToEdge, Object... additionalInfo);

    void notifyUpdateDeviceCredentials(TenantId tenantId, DeviceId deviceId, CustomerId customerId, Device device,
                                       DeviceCredentials deviceCredentials, SecurityUser user);

    void notifyAssignDeviceToTenant(TenantId tenantId, TenantId newTenantId, DeviceId deviceId, CustomerId customerId,
                                    Device device, Tenant tenant, SecurityUser user, Object... additionalInfo);

    void notifyAssignOrUnassignDeviceToEdge(TenantId tenantId, DeviceId deviceId, CustomerId customerId, EdgeId edgeId,
                                            Device device, ActionType actionType, EdgeEventActionType edgeActionType,
                                            SecurityUser user, Object... additionalInfo);
}
