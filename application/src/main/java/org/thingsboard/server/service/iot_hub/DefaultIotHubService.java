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
package org.thingsboard.server.service.iot_hub;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rule.RuleChainData;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.cf.TbCalculatedFieldService;
import org.thingsboard.server.service.entitiy.dashboard.TbDashboardService;
import org.thingsboard.server.service.entitiy.device.profile.TbDeviceProfileService;
import org.thingsboard.server.service.entitiy.widgets.type.TbWidgetTypeService;
import org.thingsboard.server.service.rule.TbRuleChainService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.dao.rule.RuleChainService;

@Service
@TbCoreComponent
@RequiredArgsConstructor
@Slf4j
public class DefaultIotHubService implements IotHubService {

    private final IotHubRestClient iotHubRestClient;
    private final TbWidgetTypeService tbWidgetTypeService;
    private final TbDashboardService tbDashboardService;
    private final TbCalculatedFieldService tbCalculatedFieldService;
    private final RuleChainService ruleChainService;
    private final TbRuleChainService tbRuleChainService;
    private final TbDeviceProfileService tbDeviceProfileService;

    @Override
    public JsonNode installItemVersion(SecurityUser user, String versionId) throws Exception {
        TenantId tenantId = user.getTenantId();
        log.info("[{}] Installing IoT Hub item version: {}", tenantId, versionId);

        JsonNode versionInfo = iotHubRestClient.getVersionInfo(versionId);
        String itemType = versionInfo.get("type").asText();
        String itemName = versionInfo.get("name").asText();
        log.debug("[{}] Fetched version info: {} (type: {})", tenantId, itemName, itemType);

        byte[] fileData = iotHubRestClient.getVersionFileData(versionId);
        log.debug("[{}] Fetched file data, size: {} bytes", tenantId, fileData != null ? fileData.length : 0);

        switch (itemType) {
            case "WIDGET":
                installWidget(user, tenantId, fileData);
                break;
            case "DASHBOARD":
                installDashboard(user, tenantId, fileData);
                break;
            case "CALCULATED_FIELD":
                installCalculatedField(user, tenantId, fileData);
                break;
            case "RULE_CHAIN":
                installRuleChain(tenantId, fileData);
                break;
            case "DEVICE":
                installDeviceProfile(user, tenantId, fileData);
                break;
            default:
                throw new IllegalArgumentException("Unsupported IoT Hub item type: " + itemType);
        }

        iotHubRestClient.reportVersionInstalled(versionId);
        log.info("[{}] Successfully installed IoT Hub item version: {} (type: {})", tenantId, itemName, itemType);

        return versionInfo;
    }

    private void installWidget(SecurityUser user, TenantId tenantId, byte[] fileData) throws Exception {
        WidgetTypeDetails widgetTypeDetails = JacksonUtil.fromBytes(fileData, WidgetTypeDetails.class);
        widgetTypeDetails.setId(null);
        widgetTypeDetails.setTenantId(tenantId);
        tbWidgetTypeService.save(widgetTypeDetails, true, user);
        log.debug("[{}] Widget installed: {}", tenantId, widgetTypeDetails.getName());
    }

    private void installDashboard(SecurityUser user, TenantId tenantId, byte[] fileData) throws Exception {
        Dashboard dashboard = JacksonUtil.fromBytes(fileData, Dashboard.class);
        dashboard.setId(null);
        dashboard.setTenantId(tenantId);
        tbDashboardService.save(dashboard, user);
        log.debug("[{}] Dashboard installed: {}", tenantId, dashboard.getTitle());
    }

    private void installCalculatedField(SecurityUser user, TenantId tenantId, byte[] fileData) throws Exception {
        CalculatedField calculatedField = JacksonUtil.fromBytes(fileData, CalculatedField.class);
        calculatedField.setId(null);
        calculatedField.setTenantId(tenantId);
        tbCalculatedFieldService.save(calculatedField, user);
        log.debug("[{}] Calculated field installed: {}", tenantId, calculatedField.getName());
    }

    private void installRuleChain(TenantId tenantId, byte[] fileData) {
        RuleChainData ruleChainData = JacksonUtil.fromBytes(fileData, RuleChainData.class);
        ruleChainService.importTenantRuleChains(tenantId, ruleChainData, false, tbRuleChainService::updateRuleNodeConfiguration);
        log.debug("[{}] Rule chain(s) installed", tenantId);
    }

    private void installDeviceProfile(SecurityUser user, TenantId tenantId, byte[] fileData) throws Exception {
        DeviceProfile deviceProfile = JacksonUtil.fromBytes(fileData, DeviceProfile.class);
        deviceProfile.setId(null);
        deviceProfile.setTenantId(tenantId);
        tbDeviceProfileService.save(deviceProfile, user);
        log.debug("[{}] Device profile installed: {}", tenantId, deviceProfile.getName());
    }
}
