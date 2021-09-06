/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.service.install.update;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.device.profile.AbstractDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.CommonTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.device.DeviceProfileService;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Profile("install")
public class DeviceProfileRpcConfigUpdater extends PaginatedUpdater<DeviceProfileId, DeviceProfile> {

    @Value("${actors.rpc.sequential}")
    private boolean configRpcSequential;

    private final DeviceProfileService deviceProfileService;


    @Override
    protected String getName() {
        return "Update Device Profiles for all the tenants";
    }

    @Override
    protected PageData<DeviceProfile> findEntities(DeviceProfileId id, PageLink pageLink) {
        return deviceProfileService.findDeviceProfiles(pageLink);
    }

    @Override
    protected void updateEntity(DeviceProfile deviceProfile) {
        if (configRpcSequential) {
            DeviceProfileData profileData = deviceProfile.getProfileData();
            AbstractDeviceProfileTransportConfiguration transportConfiguration = (AbstractDeviceProfileTransportConfiguration) profileData.getTransportConfiguration();

            CommonTransportConfiguration commonTransportConfiguration = Optional.ofNullable(transportConfiguration.getCommonTransportConfiguration())
                    .orElseGet(CommonTransportConfiguration::new);

            commonTransportConfiguration.setSequentialRpc(configRpcSequential);

            transportConfiguration.setCommonTransportConfiguration(commonTransportConfiguration);
            deviceProfile.setProfileData(profileData);
            deviceProfileService.saveDeviceProfile(deviceProfile);
        }
    }

}
