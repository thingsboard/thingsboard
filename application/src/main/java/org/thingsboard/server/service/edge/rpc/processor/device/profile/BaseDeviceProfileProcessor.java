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
package org.thingsboard.server.service.edge.rpc.processor.device.profile;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.gen.edge.v1.DeviceProfileUpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

@Slf4j
public abstract class BaseDeviceProfileProcessor extends BaseEdgeProcessor {

    @Autowired
    private DataValidator<DeviceProfile> deviceProfileValidator;

    protected Pair<Boolean, Boolean> saveOrUpdateDeviceProfile(TenantId tenantId, DeviceProfileId deviceProfileId, DeviceProfileUpdateMsg deviceProfileUpdateMsg) {
        boolean created = false;
        boolean deviceProfileNameUpdated = false;
        deviceCreationLock.lock();
        try {
            DeviceProfile deviceProfile = JacksonUtil.fromString(deviceProfileUpdateMsg.getEntity(), DeviceProfile.class, true);
            if (deviceProfile == null) {
                throw new RuntimeException("[{" + tenantId + "}] deviceProfileUpdateMsg {" + deviceProfileUpdateMsg + "} cannot be converted to device profile");
            }
            DeviceProfile deviceProfileById = edgeCtx.getDeviceProfileService().findDeviceProfileById(tenantId, deviceProfileId);
            if (deviceProfileById == null) {
                created = true;
                deviceProfile.setId(null);
                deviceProfile.setDefault(false);
            } else {
                deviceProfile.setId(deviceProfileId);
                deviceProfile.setDefault(deviceProfileById.isDefault());
            }
            String deviceProfileName = deviceProfile.getName();
            DeviceProfile deviceProfileByName = edgeCtx.getDeviceProfileService().findDeviceProfileByName(tenantId, deviceProfileName);
            if (deviceProfileByName != null && !deviceProfileByName.getId().equals(deviceProfileId)) {
                deviceProfileName = deviceProfileName + "_" + StringUtils.randomAlphabetic(15);
                log.warn("[{}] Device profile with name {} already exists. Renaming device profile name to {}",
                        tenantId, deviceProfile.getName(), deviceProfileName);
                deviceProfileNameUpdated = true;
            }
            deviceProfile.setName(deviceProfileName);

            RuleChainId ruleChainId = deviceProfile.getDefaultRuleChainId();
            setDefaultRuleChainId(tenantId, deviceProfile, created ? null : deviceProfileById.getDefaultRuleChainId());
            setDefaultEdgeRuleChainId(deviceProfile, ruleChainId, deviceProfileUpdateMsg);
            setDefaultDashboardId(tenantId, created ? null : deviceProfileById.getDefaultDashboardId(), deviceProfile, deviceProfileUpdateMsg);

            deviceProfileValidator.validate(deviceProfile, DeviceProfile::getTenantId);
            if (created) {
                deviceProfile.setId(deviceProfileId);
            }
            edgeCtx.getDeviceProfileService().saveDeviceProfile(deviceProfile, false, true);
        } catch (Exception e) {
            log.error("[{}] Failed to process device profile update msg [{}]", tenantId, deviceProfileUpdateMsg, e);
            throw e;
        } finally {
            deviceCreationLock.unlock();
        }
        return Pair.of(created, deviceProfileNameUpdated);
    }

    protected abstract void setDefaultRuleChainId(TenantId tenantId, DeviceProfile deviceProfile, RuleChainId ruleChainId);

    protected abstract void setDefaultEdgeRuleChainId(DeviceProfile deviceProfile, RuleChainId ruleChainId, DeviceProfileUpdateMsg deviceProfileUpdateMsg);

    protected abstract void setDefaultDashboardId(TenantId tenantId, DashboardId dashboardId, DeviceProfile deviceProfile, DeviceProfileUpdateMsg deviceProfileUpdateMsg);

}
