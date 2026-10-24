// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.entitiy.device;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.NameConflictStrategy;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.dao.device.claim.ClaimResult;
import org.thingsboard.server.dao.device.claim.ReclaimResult;

import java.util.List;

public interface TbDeviceService {

    Device save(Device device, EntityGroup entityGroup) throws Exception;

    Device save(Device device, String accessToken, EntityGroup entityGroup, User user) throws Exception;

    Device save(Device device, String accessToken, List<EntityGroup> entityGroups, User user) throws Exception;

    Device save(Device device, String accessToken, List<EntityGroup> entityGroups, NameConflictStrategy nameConflictStrategy, User user) throws Exception;

    Device saveDeviceWithCredentials(Device device, DeviceCredentials deviceCredentials, EntityGroup entityGroup, User user) throws ThingsboardException;

    Device saveDeviceWithCredentials(Device device, DeviceCredentials deviceCredentials, List<EntityGroup> entityGroups, User user) throws ThingsboardException;

    Device saveDeviceWithCredentials(Device device, DeviceCredentials deviceCredentials, List<EntityGroup> entityGroups, NameConflictStrategy nameConflictStrategy, User user) throws ThingsboardException;

    void delete(Device device, User user);

    void delete(DeviceId deviceId, User user);

    DeviceCredentials getDeviceCredentialsByDeviceId(Device device, User user) throws ThingsboardException;

    DeviceCredentials updateDeviceCredentials(Device device, DeviceCredentials deviceCredentials, User user) throws ThingsboardException;

    ListenableFuture<ClaimResult> claimDevice(TenantId tenantId, Device device, CustomerId customerId, String secretKey, User user);

    ListenableFuture<ReclaimResult> reclaimDevice(TenantId tenantId, Device device, User user);

    Device assignDeviceToTenant(Device device, Tenant newTenant, User user);

}
