// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.service.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.device.credentials.BasicMqttCredentials;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
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
        rejectControlChars(deviceCredentials.getCredentialsId(), "credentialsId");
        if (deviceCredentials.getCredentialsType() == DeviceCredentialsType.MQTT_BASIC) {
            BasicMqttCredentials mqtt = JacksonUtil.fromString(deviceCredentials.getCredentialsValue(), BasicMqttCredentials.class);
            if (mqtt != null) {
                rejectControlChars(mqtt.getClientId(), "clientId");
                rejectControlChars(mqtt.getUserName(), "userName");
                rejectControlChars(mqtt.getPassword(), "password");
            }
        }
        Device device = deviceService.findDeviceById(tenantId, deviceCredentials.getDeviceId());
        if (device == null) {
            throw new DeviceCredentialsValidationException("Can't assign device credentials to non-existent device!");
        }
    }

    private static void rejectControlChars(String value, String fieldName) {
        if (StringUtils.containsControlChars(value)) {
            throw new DeviceCredentialsValidationException(fieldName + " must not contain control characters!");
        }
    }
}
