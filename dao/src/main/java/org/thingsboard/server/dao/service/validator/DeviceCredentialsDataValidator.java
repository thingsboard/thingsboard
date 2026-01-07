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
package org.thingsboard.server.dao.service.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.dao.device.DeviceCredentialsDao;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.exception.DeviceCredentialsValidationException;
import org.thingsboard.server.dao.service.DataValidator;

@Component
public class DeviceCredentialsDataValidator extends DataValidator<DeviceCredentials> {

    @Autowired
    private DeviceCredentialsDao deviceCredentialsDao;

    @Autowired @Lazy
    private DeviceService deviceService;

    @Override
    protected void validateCreate(TenantId tenantId, DeviceCredentials deviceCredentials) {
        if (deviceCredentialsDao.findByDeviceId(tenantId, deviceCredentials.getDeviceId().getId()) != null) {
            throw new DeviceCredentialsValidationException("Credentials for this device are already specified!");
        }
        if (deviceCredentialsDao.findByCredentialsId(tenantId, deviceCredentials.getCredentialsId()) != null) {
            throw new DeviceCredentialsValidationException("Device credentials are already assigned to another device!");
        }
    }

    @Override
    protected DeviceCredentials validateUpdate(TenantId tenantId, DeviceCredentials deviceCredentials) {
        if (deviceCredentialsDao.findById(tenantId, deviceCredentials.getUuidId()) == null) {
            throw new DeviceCredentialsValidationException("Unable to update non-existent device credentials!");
        }
        DeviceCredentials existingCredentials = deviceCredentialsDao.findByCredentialsId(tenantId, deviceCredentials.getCredentialsId());
        if (existingCredentials != null && !existingCredentials.getId().equals(deviceCredentials.getId())) {
            throw new DeviceCredentialsValidationException("Device credentials are already assigned to another device!");
        }
        return existingCredentials;
    }

    @Override
    protected void validateDataImpl(TenantId tenantId, DeviceCredentials deviceCredentials) {
        if (deviceCredentials.getDeviceId() == null) {
            throw new DeviceCredentialsValidationException("Device credentials should be assigned to device!");
        }
        if (deviceCredentials.getCredentialsType() == null) {
            throw new DeviceCredentialsValidationException("Device credentials type should be specified!");
        }
        if (StringUtils.isEmpty(deviceCredentials.getCredentialsId())) {
            throw new DeviceCredentialsValidationException("Device credentials id should be specified!");
        }
        Device device = deviceService.findDeviceById(tenantId, deviceCredentials.getDeviceId());
        if (device == null) {
            throw new DeviceCredentialsValidationException("Can't assign device credentials to non-existent device!");
        }
    }
}
