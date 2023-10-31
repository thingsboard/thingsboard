/**
 * Copyright © 2016-2023 The Thingsboard Authors
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

import com.datastax.oss.driver.api.core.uuid.Uuids;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.edge.v1.DeviceProfileUpdateMsg;
import org.thingsboard.server.queue.util.DataDecodingEncodingService;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

@Slf4j
public abstract class BaseDeviceProfileProcessor extends BaseEdgeProcessor {

    @Autowired
    private DataDecodingEncodingService dataDecodingEncodingService;

    protected Pair<Boolean, Boolean> saveOrUpdateDeviceProfile(TenantId tenantId, DeviceProfileId deviceProfileId, DeviceProfileUpdateMsg deviceProfileUpdateMsg) {
        boolean created = false;
        boolean deviceProfileNameUpdated = false;
        deviceCreationLock.lock();
        try {
            DeviceProfile deviceProfile = deviceProfileService.findDeviceProfileById(tenantId, deviceProfileId);
            String deviceProfileName = deviceProfileUpdateMsg.getName();
            if (deviceProfile == null) {
                created = true;
                deviceProfile = new DeviceProfile();
                deviceProfile.setTenantId(tenantId);
                deviceProfile.setCreatedTime(Uuids.unixTimestamp(deviceProfileId.getId()));
            }
            DeviceProfile deviceProfileByName = deviceProfileService.findDeviceProfileByName(tenantId, deviceProfileName);
            if (deviceProfileByName != null && !deviceProfileByName.getId().equals(deviceProfileId)) {
                deviceProfileName = deviceProfileName + "_" + StringUtils.randomAlphabetic(15);
                log.warn("[{}] Device profile with name {} already exists. Renaming device profile name to {}",
                        tenantId, deviceProfileUpdateMsg.getName(), deviceProfileName);
                deviceProfileNameUpdated = true;
            }
            deviceProfile.setName(deviceProfileName);
            deviceProfile.setDescription(deviceProfileUpdateMsg.hasDescription() ? deviceProfileUpdateMsg.getDescription() : null);
            deviceProfile.setType(DeviceProfileType.valueOf(deviceProfileUpdateMsg.getType()));
            deviceProfile.setTransportType(deviceProfileUpdateMsg.hasTransportType()
                    ? DeviceTransportType.valueOf(deviceProfileUpdateMsg.getTransportType()) : DeviceTransportType.DEFAULT);
            deviceProfile.setImage(deviceProfileUpdateMsg.hasImage()
                    ? new String(deviceProfileUpdateMsg.getImage().toByteArray(), StandardCharsets.UTF_8) : null);
            deviceProfile.setProvisionType(deviceProfileUpdateMsg.hasProvisionType()
                    ? DeviceProfileProvisionType.valueOf(deviceProfileUpdateMsg.getProvisionType()) : DeviceProfileProvisionType.DISABLED);
            deviceProfile.setProvisionDeviceKey(deviceProfileUpdateMsg.hasProvisionDeviceKey()
                    ? deviceProfileUpdateMsg.getProvisionDeviceKey() : null);
            deviceProfile.setDefaultQueueName(deviceProfileUpdateMsg.getDefaultQueueName());

            Optional<DeviceProfileData> profileDataOpt =
                    dataDecodingEncodingService.decode(deviceProfileUpdateMsg.getProfileDataBytes().toByteArray());
            deviceProfile.setProfileData(profileDataOpt.orElse(null));

            setDefaultRuleChainId(tenantId, deviceProfile, deviceProfileUpdateMsg);
            setDefaultEdgeRuleChainId(tenantId, deviceProfile, deviceProfileUpdateMsg);
            setDefaultDashboardId(tenantId, deviceProfile, deviceProfileUpdateMsg);

            String defaultQueueName = StringUtils.isNotBlank(deviceProfileUpdateMsg.getDefaultQueueName())
                    ? deviceProfileUpdateMsg.getDefaultQueueName() : null;
            deviceProfile.setDefaultQueueName(defaultQueueName);

            UUID firmwareUUID = safeGetUUID(deviceProfileUpdateMsg.getFirmwareIdMSB(), deviceProfileUpdateMsg.getFirmwareIdLSB());
            deviceProfile.setFirmwareId(firmwareUUID != null ? new OtaPackageId(firmwareUUID) : null);

            UUID softwareUUID = safeGetUUID(deviceProfileUpdateMsg.getSoftwareIdMSB(), deviceProfileUpdateMsg.getSoftwareIdLSB());
            deviceProfile.setSoftwareId(softwareUUID != null ? new OtaPackageId(softwareUUID) : null);

            deviceProfileValidator.validate(deviceProfile, DeviceProfile::getTenantId);
            if (created) {
                deviceProfile.setId(deviceProfileId);
            }
            deviceProfileService.saveDeviceProfile(deviceProfile, false);
        } catch (Exception e) {
            log.error("[{}] Failed to process device profile update msg [{}]", tenantId, deviceProfileUpdateMsg, e);
            throw e;
        }  finally {
            deviceCreationLock.unlock();
        }
        return Pair.of(created, deviceProfileNameUpdated);
    }

    protected abstract void setDefaultRuleChainId(TenantId tenantId, DeviceProfile deviceProfile, DeviceProfileUpdateMsg deviceProfileUpdateMsg);

    protected abstract void setDefaultEdgeRuleChainId(TenantId tenantId, DeviceProfile deviceProfile, DeviceProfileUpdateMsg deviceProfileUpdateMsg);

    protected abstract void setDefaultDashboardId(TenantId tenantId, DeviceProfile deviceProfile, DeviceProfileUpdateMsg deviceProfileUpdateMsg);
}
