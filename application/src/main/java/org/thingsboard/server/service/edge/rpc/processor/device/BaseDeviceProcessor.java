/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.service.edge.rpc.processor.device;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.gen.edge.v1.DeviceCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceUpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

@Slf4j
public abstract class BaseDeviceProcessor extends BaseEdgeProcessor {

    @Autowired
    private DataValidator<Device> deviceValidator;

    protected Pair<Boolean, Boolean> saveOrUpdateDevice(TenantId tenantId, DeviceId deviceId, DeviceUpdateMsg deviceUpdateMsg) {
        boolean created = false;
        boolean deviceNameUpdated = false;
        deviceCreationLock.lock();
        try {
            Device device = JacksonUtil.fromString(deviceUpdateMsg.getEntity(), Device.class, true);
            if (device == null) {
                throw new RuntimeException("[{" + tenantId + "}] deviceUpdateMsg {" + deviceUpdateMsg + "} cannot be converted to device");
            }
            Device deviceById = edgeCtx.getDeviceService().findDeviceById(tenantId, deviceId);
            if (deviceById == null) {
                created = true;
                device.setId(null);
            } else {
                device.setId(deviceId);
            }
            if (isSaveRequired(deviceById, device)) {
                deviceNameUpdated = updateDeviceNameIfDuplicateExists(tenantId, deviceId, device);
                setCustomerId(tenantId, created ? null : deviceById.getCustomerId(), device, deviceUpdateMsg);

                deviceValidator.validate(device, Device::getTenantId);
                if (created) {
                    device.setId(deviceId);
                }
                Device savedDevice = edgeCtx.getDeviceService().saveDevice(device, false);
                edgeCtx.getClusterService().onDeviceUpdated(savedDevice, created ? null : device);
            }
        } catch (Exception e) {
            log.error("[{}] Failed to process device update msg [{}]", tenantId, deviceUpdateMsg, e);
            throw e;
        } finally {
            deviceCreationLock.unlock();
        }
        return Pair.of(created, deviceNameUpdated);
    }

    private boolean updateDeviceNameIfDuplicateExists(TenantId tenantId, DeviceId deviceId, Device device) {
        Device deviceByName = edgeCtx.getDeviceService().findDeviceByTenantIdAndName(tenantId, device.getName());

        return generateUniqueNameIfDuplicateExists(tenantId, deviceId, device, deviceByName).map(uniqueName -> {
            device.setName(uniqueName);
            return true;
        }).orElse(false);
    }

    protected void updateDeviceCredentials(TenantId tenantId, DeviceCredentialsUpdateMsg deviceCredentialsUpdateMsg) {
        DeviceCredentials deviceCredentials = JacksonUtil.fromString(deviceCredentialsUpdateMsg.getEntity(), DeviceCredentials.class, true);
        if (deviceCredentials == null) {
            throw new RuntimeException("[{" + tenantId + "}] deviceCredentialsUpdateMsg {" + deviceCredentialsUpdateMsg + "} cannot be converted to device credentials");
        }
        Device device = edgeCtx.getDeviceService().findDeviceById(tenantId, deviceCredentials.getDeviceId());
        if (device != null) {
            log.debug("[{}] Updating device credentials for device [{}]. New device credentials Id [{}], value [{}]",
                    tenantId, device.getName(), deviceCredentials.getCredentialsId(), deviceCredentials.getCredentialsValue());
            try {
                DeviceCredentials deviceCredentialsByDeviceId = edgeCtx.getDeviceCredentialsService().findDeviceCredentialsByDeviceId(tenantId, device.getId());
                if (deviceCredentialsByDeviceId == null) {
                    deviceCredentialsByDeviceId = new DeviceCredentials();
                    deviceCredentialsByDeviceId.setDeviceId(device.getId());
                }
                deviceCredentialsByDeviceId.setCredentialsType(deviceCredentials.getCredentialsType());
                deviceCredentialsByDeviceId.setCredentialsId(deviceCredentials.getCredentialsId());
                deviceCredentialsByDeviceId.setCredentialsValue(deviceCredentials.getCredentialsValue());
                edgeCtx.getDeviceCredentialsService().updateDeviceCredentials(tenantId, deviceCredentialsByDeviceId);

            } catch (Exception e) {
                log.error("[{}] Can't update device credentials for device [{}], deviceCredentialsUpdateMsg [{}]",
                        tenantId, device.getName(), deviceCredentialsUpdateMsg, e);
                throw new RuntimeException(e);
            }
        } else {
            log.warn("[{}] Can't find device by id [{}], deviceCredentialsUpdateMsg [{}]", tenantId, deviceCredentials.getDeviceId(), deviceCredentialsUpdateMsg);
        }
    }

    protected abstract void setCustomerId(TenantId tenantId, CustomerId customerId, Device device, DeviceUpdateMsg deviceUpdateMsg);

    protected void deleteDevice(TenantId tenantId, DeviceId deviceId) {
        deleteDevice(tenantId, null, deviceId);
    }

    protected void deleteDevice(TenantId tenantId, Edge edge, DeviceId deviceId) {
        Device deviceById = edgeCtx.getDeviceService().findDeviceById(tenantId, deviceId);
        if (deviceById != null) {
            edgeCtx.getDeviceService().deleteDevice(tenantId, deviceId);
            pushEntityEventToRuleEngine(tenantId, edge, deviceById, TbMsgType.ENTITY_DELETED);
        }
    }
}
