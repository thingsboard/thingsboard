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
import org.thingsboard.server.common.data.iot_hub.CalculatedFieldInstalledItemDescriptor;
import org.thingsboard.server.common.data.iot_hub.DashboardInstalledItemDescriptor;
import org.thingsboard.server.common.data.iot_hub.DeviceInstalledItemDescriptor;
import org.thingsboard.server.common.data.iot_hub.IotHubInstalledItem;
import org.thingsboard.server.common.data.iot_hub.IotHubInstalledItemDescriptor;
import org.thingsboard.server.common.data.iot_hub.RuleChainInstalledItemDescriptor;
import org.thingsboard.server.common.data.iot_hub.WidgetInstalledItemDescriptor;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainData;
import org.thingsboard.server.common.data.rule.RuleChainImportResult;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.dao.cf.CalculatedFieldService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.iot_hub.IotHubInstalledItemService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.widget.WidgetTypeService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.cf.TbCalculatedFieldService;
import org.thingsboard.server.service.entitiy.dashboard.TbDashboardService;
import org.thingsboard.server.service.entitiy.device.profile.TbDeviceProfileService;
import org.thingsboard.server.service.entitiy.widgets.type.TbWidgetTypeService;
import org.thingsboard.server.service.rule.TbRuleChainService;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.List;
import java.util.UUID;

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
    private final IotHubInstalledItemService iotHubInstalledItemService;
    private final WidgetTypeService widgetTypeService;
    private final DashboardService dashboardService;
    private final CalculatedFieldService calculatedFieldService;
    private final DeviceProfileService deviceProfileService;

    @Override
    public InstallItemVersionResult installItemVersion(SecurityUser user, String versionId) {
        TenantId tenantId = user.getTenantId();
        log.info("[{}] Installing IoT Hub item version: {}", tenantId, versionId);

        try {
            JsonNode versionInfo = iotHubRestClient.getVersionInfo(versionId);
            String itemType = versionInfo.get("type").asText();
            String itemName = versionInfo.get("name").asText();
            UUID itemId = UUID.fromString(versionInfo.get("itemId").asText());
            String version = versionInfo.get("version").asText();
            log.debug("[{}] Fetched version info: {} (type: {})", tenantId, itemName, itemType);

            if (iotHubInstalledItemService.findByTenantIdAndItemId(tenantId, itemId).isPresent()) {
                return InstallItemVersionResult.error("Item '" + itemName + "' is already installed");
            }

            byte[] fileData = iotHubRestClient.getVersionFileData(versionId);
            log.debug("[{}] Fetched file data, size: {} bytes", tenantId, fileData != null ? fileData.length : 0);

            IotHubInstalledItemDescriptor descriptor = switch (itemType) {
                case "WIDGET" -> installWidget(user, tenantId, fileData);
                case "DASHBOARD" -> installDashboard(user, tenantId, fileData);
                case "CALCULATED_FIELD" -> installCalculatedField(user, tenantId, fileData);
                case "RULE_CHAIN" -> installRuleChain(tenantId, fileData);
                case "DEVICE" -> installDeviceProfile(user, tenantId, fileData);
                default -> throw new IllegalArgumentException("Unsupported IoT Hub item type: " + itemType);
            };

            IotHubInstalledItem installedItem = new IotHubInstalledItem();
            installedItem.setTenantId(tenantId);
            installedItem.setItemId(itemId);
            installedItem.setItemVersionId(UUID.fromString(versionId));
            installedItem.setItemName(itemName);
            installedItem.setItemType(itemType);
            installedItem.setVersion(version);
            installedItem.setDescriptor(descriptor);
            iotHubInstalledItemService.save(tenantId, installedItem);

            iotHubRestClient.reportVersionInstalled(versionId);
            log.info("[{}] Successfully installed IoT Hub item version: {} (type: {})", tenantId, itemName, itemType);

            return InstallItemVersionResult.success(descriptor);
        } catch (Exception e) {
            log.error("[{}] Failed to install IoT Hub item version: {}", tenantId, versionId, e);
            return InstallItemVersionResult.error(e.getMessage());
        }
    }

    private WidgetInstalledItemDescriptor installWidget(SecurityUser user, TenantId tenantId, byte[] fileData) throws Exception {
        WidgetTypeDetails widgetTypeDetails;
        try {
            widgetTypeDetails = JacksonUtil.fromString(new String(fileData), WidgetTypeDetails.class, true);
        } catch (Exception e) {
            throw new Exception("Failed to parse widget data: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()), e);
        }
        widgetTypeDetails.setId(null);
        widgetTypeDetails.setTenantId(tenantId);
        WidgetTypeDetails saved = tbWidgetTypeService.save(widgetTypeDetails, true, user);
        log.debug("[{}] Widget installed: {}", tenantId, saved.getName());
        WidgetInstalledItemDescriptor descriptor = new WidgetInstalledItemDescriptor();
        descriptor.setWidgetTypeId(saved.getId());
        return descriptor;
    }

    private DashboardInstalledItemDescriptor installDashboard(SecurityUser user, TenantId tenantId, byte[] fileData) throws Exception {
        Dashboard dashboard;
        try {
            dashboard = JacksonUtil.fromString(new String(fileData), Dashboard.class, true);
        } catch (Exception e) {
            throw new Exception("Failed to parse dashboard data: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()), e);
        }
        dashboard.setId(null);
        dashboard.setTenantId(tenantId);
        Dashboard saved = tbDashboardService.save(dashboard, user);
        log.debug("[{}] Dashboard installed: {}", tenantId, saved.getTitle());
        DashboardInstalledItemDescriptor descriptor = new DashboardInstalledItemDescriptor();
        descriptor.setDashboardId(saved.getId());
        return descriptor;
    }

    private CalculatedFieldInstalledItemDescriptor installCalculatedField(SecurityUser user, TenantId tenantId, byte[] fileData) throws Exception {
        CalculatedField calculatedField;
        try {
            calculatedField = JacksonUtil.fromString(new String(fileData), CalculatedField.class, true);
        } catch (Exception e) {
            throw new Exception("Failed to parse calculated field data: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()), e);
        }
        calculatedField.setId(null);
        calculatedField.setTenantId(tenantId);
        CalculatedField saved = tbCalculatedFieldService.save(calculatedField, user);
        log.debug("[{}] Calculated field installed: {}", tenantId, saved.getName());
        CalculatedFieldInstalledItemDescriptor descriptor = new CalculatedFieldInstalledItemDescriptor();
        descriptor.setCalculatedFieldId(saved.getId());
        return descriptor;
    }

    private RuleChainInstalledItemDescriptor installRuleChain(TenantId tenantId, byte[] fileData) throws Exception {
        JsonNode json = JacksonUtil.toJsonNode(new String(fileData));

        RuleChain ruleChain;
        try {
            ruleChain = JacksonUtil.fromString(json.get("ruleChain").toString(), RuleChain.class, true);
        } catch (Exception e) {
            throw new Exception("Failed to parse rule chain: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()), e);
        }

        RuleChainMetaData metadata;
        try {
            metadata = JacksonUtil.fromString(json.get("metadata").toString(), RuleChainMetaData.class, true);
        } catch (Exception e) {
            throw new Exception("Failed to parse rule chain metadata: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()), e);
        }

        ruleChain.setId(null);
        ruleChain.setTenantId(tenantId);
        RuleChain savedRuleChain = ruleChainService.saveRuleChain(ruleChain);

        metadata.setRuleChainId(savedRuleChain.getId());
        ruleChainService.saveRuleChainMetaData(tenantId, metadata, tbRuleChainService::updateRuleNodeConfiguration);

        log.debug("[{}] Rule chain installed: {}", tenantId, savedRuleChain.getName());
        RuleChainInstalledItemDescriptor descriptor = new RuleChainInstalledItemDescriptor();
        descriptor.setRuleChainId(savedRuleChain.getId());
        return descriptor;
    }

    private DeviceInstalledItemDescriptor installDeviceProfile(SecurityUser user, TenantId tenantId, byte[] fileData) throws Exception {
        DeviceProfile deviceProfile;
        try {
            deviceProfile = JacksonUtil.fromString(new String(fileData), DeviceProfile.class, true);
        } catch (Exception e) {
            throw new Exception("Failed to parse device profile data: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()), e);
        }
        deviceProfile.setId(null);
        deviceProfile.setTenantId(tenantId);
        tbDeviceProfileService.save(deviceProfile, user);
        log.debug("[{}] Device profile installed: {}", tenantId, deviceProfile.getName());
        return new DeviceInstalledItemDescriptor();
    }

    @Override
    public UpdateItemVersionResult updateItemVersion(SecurityUser user, UUID itemId, String versionId) {
        TenantId tenantId = user.getTenantId();
        log.info("[{}] Updating IoT Hub item {} to version: {}", tenantId, itemId, versionId);

        try {
            IotHubInstalledItem installedItem = iotHubInstalledItemService.findByTenantIdAndItemId(tenantId, itemId)
                    .orElseThrow(() -> new IllegalArgumentException("Installed item not found"));

            JsonNode installedVersionInfo = iotHubRestClient.getVersionInfo(installedItem.getItemVersionId().toString());
            String installedChecksum = installedVersionInfo.has("checksum") ? installedVersionInfo.get("checksum").asText() : null;
            log.info("[{}] Installed version info: name={}, version={}, checksum={}", tenantId, installedItem.getItemName(), installedItem.getVersion(), installedChecksum);

            JsonNode versionInfo = iotHubRestClient.getVersionInfo(versionId);
            String itemName = versionInfo.get("name").asText();
            String version = versionInfo.get("version").asText();

            byte[] fileData = iotHubRestClient.getVersionFileData(versionId);
            log.info("[{}] Fetched update file data, size: {} bytes", tenantId, fileData != null ? fileData.length : 0);

            // TODO: apply update per item type

            log.info("[{}] Successfully updated IoT Hub item {} to version: {}", tenantId, itemName, version);

            return UpdateItemVersionResult.success(installedItem.getDescriptor());
        } catch (Exception e) {
            log.error("[{}] Failed to update IoT Hub item {} to version: {}", tenantId, itemId, versionId, e);
            return UpdateItemVersionResult.error(e.getMessage());
        }
    }

    @Override
    public void deleteInstalledItem(SecurityUser user, UUID itemId) {
        TenantId tenantId = user.getTenantId();
        IotHubInstalledItem installedItem = iotHubInstalledItemService.findByTenantIdAndItemId(tenantId, itemId)
                .orElseThrow(() -> new IllegalArgumentException("Installed item not found"));

        IotHubInstalledItemDescriptor descriptor = installedItem.getDescriptor();
        if (descriptor instanceof WidgetInstalledItemDescriptor wd) {
            WidgetTypeDetails widgetType = widgetTypeService.findWidgetTypeDetailsById(tenantId, wd.getWidgetTypeId());
            if (widgetType != null) {
                tbWidgetTypeService.delete(widgetType, user);
            }
        } else if (descriptor instanceof DashboardInstalledItemDescriptor dd) {
            Dashboard dashboard = dashboardService.findDashboardById(tenantId, dd.getDashboardId());
            if (dashboard != null) {
                tbDashboardService.delete(dashboard, user);
            }
        } else if (descriptor instanceof CalculatedFieldInstalledItemDescriptor cd) {
            CalculatedField calculatedField = calculatedFieldService.findById(tenantId, cd.getCalculatedFieldId());
            if (calculatedField != null) {
                tbCalculatedFieldService.delete(calculatedField, user);
            }
        } else if (descriptor instanceof RuleChainInstalledItemDescriptor rd) {
            if (rd.getRuleChainId() != null) {
                RuleChain ruleChain = ruleChainService.findRuleChainById(tenantId, rd.getRuleChainId());
                if (ruleChain != null) {
                    tbRuleChainService.delete(ruleChain, user);
                }
            }
        } else if (descriptor instanceof DeviceInstalledItemDescriptor) {
            // no entity to delete for now
        }

        iotHubInstalledItemService.deleteByTenantIdAndItemId(tenantId, itemId);
        log.info("[{}] Deleted installed IoT Hub item: {}", tenantId, installedItem.getItemName());
    }
}
