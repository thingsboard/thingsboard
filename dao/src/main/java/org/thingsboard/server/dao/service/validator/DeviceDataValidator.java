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
package org.thingsboard.server.dao.service.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.device.data.DeviceTransportConfiguration;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.device.DeviceDao;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.ota.OtaPackageService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.dao.tenant.TenantService;

import java.util.Optional;

import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

@Component
public class DeviceDataValidator extends DataValidator<Device> {

    @Autowired
    private DeviceDao deviceDao;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private CustomerDao customerDao;

    @Autowired
    @Lazy
    private TbTenantProfileCache tenantProfileCache;

    @Autowired
    private OtaPackageService otaPackageService;

    @Override
    protected void validateCreate(TenantId tenantId, Device device) {
        DefaultTenantProfileConfiguration profileConfiguration =
                (DefaultTenantProfileConfiguration) tenantProfileCache.get(tenantId).getProfileData().getConfiguration();
        long maxDevices = profileConfiguration.getMaxDevices();
        validateNumberOfEntitiesPerTenant(tenantId, deviceDao, maxDevices, EntityType.DEVICE);
    }

    @Override
    protected Device validateUpdate(TenantId tenantId, Device device) {
        Device old = deviceDao.findById(device.getTenantId(), device.getId().getId());
        if (old == null) {
            throw new DataValidationException("Can't update non existing device!");
        }
        return old;
    }

    @Override
    protected void validateDataImpl(TenantId tenantId, Device device) {
        if (StringUtils.isEmpty(device.getName()) || device.getName().trim().length() == 0) {
            throw new DataValidationException("Device name should be specified!");
        }
        if (device.getTenantId() == null) {
            throw new DataValidationException("Device should be assigned to tenant!");
        } else {
            if (!tenantService.tenantExists(device.getTenantId())) {
                throw new DataValidationException("Device is referencing to non-existent tenant!");
            }
        }
        if (device.getCustomerId() == null) {
            device.setCustomerId(new CustomerId(NULL_UUID));
        } else if (!device.getCustomerId().getId().equals(NULL_UUID)) {
            Customer customer = customerDao.findById(device.getTenantId(), device.getCustomerId().getId());
            if (customer == null) {
                throw new DataValidationException("Can't assign device to non-existent customer!");
            }
            if (!customer.getTenantId().getId().equals(device.getTenantId().getId())) {
                throw new DataValidationException("Can't assign device to customer from different tenant!");
            }
        }
        Optional.ofNullable(device.getDeviceData())
                .flatMap(deviceData -> Optional.ofNullable(deviceData.getTransportConfiguration()))
                .ifPresent(DeviceTransportConfiguration::validate);

        if (device.getFirmwareId() != null) {
            OtaPackage firmware = otaPackageService.findOtaPackageById(tenantId, device.getFirmwareId());
            if (firmware == null) {
                throw new DataValidationException("Can't assign non-existent firmware!");
            }
            if (!firmware.getType().equals(OtaPackageType.FIRMWARE)) {
                throw new DataValidationException("Can't assign firmware with type: " + firmware.getType());
            }
            if (firmware.getData() == null && !firmware.hasUrl()) {
                throw new DataValidationException("Can't assign firmware with empty data!");
            }
            if (!firmware.getDeviceProfileId().equals(device.getDeviceProfileId())) {
                throw new DataValidationException("Can't assign firmware with different deviceProfile!");
            }
        }

        if (device.getSoftwareId() != null) {
            OtaPackage software = otaPackageService.findOtaPackageById(tenantId, device.getSoftwareId());
            if (software == null) {
                throw new DataValidationException("Can't assign non-existent software!");
            }
            if (!software.getType().equals(OtaPackageType.SOFTWARE)) {
                throw new DataValidationException("Can't assign software with type: " + software.getType());
            }
            if (software.getData() == null && !software.hasUrl()) {
                throw new DataValidationException("Can't assign software with empty data!");
            }
            if (!software.getDeviceProfileId().equals(device.getDeviceProfileId())) {
                throw new DataValidationException("Can't assign firmware with different deviceProfile!");
            }
        }
    }
}
