/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.service.gateway_device;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.device.GatewayInfo;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


@Service
@Slf4j
@RequiredArgsConstructor
@TbCoreComponent
public class DefaultGatewayInfoManagementService implements GatewayInfoManagementService {

    private final String LATEST_VERSION = "latest";

    @Autowired
    AdminSettingsService adminSettingsService;

    @Override
    public void updateAvailableVersions(GatewayInfo newGatewayInfo) {
        AdminSettings gatewaySettings = adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, "gateway");
        GatewayInfo gatewayInfo;

        if (gatewaySettings == null) {
            gatewaySettings = new AdminSettings();
            gatewaySettings.setKey("gateway");
            gatewayInfo = new GatewayInfo();
            List<String> availableVersions = new ArrayList<>(Collections.singletonList(LATEST_VERSION));
            availableVersions.addAll(newGatewayInfo.getAvailableVersions());
            gatewayInfo.setAvailableVersions(availableVersions);
        } else {
            gatewayInfo = JacksonUtil.treeToValue(gatewaySettings.getJsonValue(), GatewayInfo.class);
            List<String> incomingVersions = newGatewayInfo.getAvailableVersions();
            if (!incomingVersions.contains(LATEST_VERSION)) {
                incomingVersions.add(LATEST_VERSION);
            }
            if (!incomingVersions.equals(gatewayInfo.getAvailableVersions())) {
                gatewayInfo.setAvailableVersions(incomingVersions);
            }
        }

        if (gatewayInfo.getVersion() == null) {
            gatewayInfo.setVersion(LATEST_VERSION);
        }

        gatewaySettings.setJsonValue(JacksonUtil.valueToTree(gatewayInfo));
        adminSettingsService.saveAdminSettings(TenantId.SYS_TENANT_ID, gatewaySettings);
    }

}
