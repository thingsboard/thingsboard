/**
 * Copyright © 2016-2019 The Thingsboard Authors
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
package org.thingsboard.server.dao.device;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.dao.device.provision.ProvisionRequest;

import java.util.UUID;

@Service
@Slf4j
public class DeviceProvisionServiceImpl implements DeviceProvisionService {

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private DeviceCredentialsService deviceCredentialsService;

    @Override
    public DeviceCredentials provisionDevice(ProvisionRequest provisionRequest) {

        TenantId tenantId = new TenantId(UUID.fromString("5f84beb0-f3eb-11e9-a161-1723558a3be4"));

        Device device = deviceService.findDeviceByTenantIdAndName(tenantId, provisionRequest.getDeviceName());
        return deviceCredentialsService.findDeviceCredentialsByDeviceId(device.getTenantId(), device.getId());
    }
}
