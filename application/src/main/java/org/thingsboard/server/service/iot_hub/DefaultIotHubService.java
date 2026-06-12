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
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.IotHubInstalledItemId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.iot_hub.AlarmRuleInstalledItemDescriptor;
import org.thingsboard.server.common.data.iot_hub.CalculatedFieldInstalledItemDescriptor;
import org.thingsboard.server.common.data.iot_hub.DashboardInstalledItemDescriptor;
import org.thingsboard.server.common.data.iot_hub.DeviceInstalledItemDescriptor;
import org.thingsboard.server.common.data.iot_hub.IotHubInstalledItem;
import org.thingsboard.server.common.data.iot_hub.IotHubInstalledItemDescriptor;
import org.thingsboard.server.common.data.iot_hub.RuleChainInstalledItemDescriptor;
import org.thingsboard.server.common.data.iot_hub.SolutionTemplateInstalledItemDescriptor;
import org.thingsboard.server.common.data.iot_hub.WidgetInstalledItemDescriptor;
import org.thingsboard.server.exception.EntitiesLimitExceededException;
import org.thingsboard.server.service.solutions.SolutionService;
import org.thingsboard.server.service.solutions.data.solution.SolutionInstallResponse;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.NodeConnectionInfo;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.dao.asset.AssetProfileService;
import org.thingsboard.server.dao.cf.CalculatedFieldService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.iot_hub.IotHubInstalledItemService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.widget.WidgetTypeService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.asset.profile.TbAssetProfileService;
import org.thingsboard.server.service.entitiy.cf.TbCalculatedFieldService;
import org.thingsboard.server.service.entitiy.dashboard.TbDashboardService;
import org.thingsboard.server.service.entitiy.device.TbDeviceService;
import org.thingsboard.server.service.entitiy.device.profile.TbDeviceProfileService;
import org.thingsboard.server.service.entitiy.widgets.type.TbWidgetTypeService;
import org.thingsboard.server.service.rule.TbRuleChainService;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@TbCoreComponent
@RequiredArgsConstructor
@Slf4j
public class DefaultIotHubService implements IotHubService {

    private static final String ACTION_INSTALL = "install";
    private static final String ACTION_UPDATE = "update";
    private static final String SECTION_PACKAGE_DATA = "package data";
    private static final String SECTION_RULE_CHAIN_METADATA = "rule chain metadata";

    private static final String ITEM_TYPE_WIDGET = "widget";
    private static final String ITEM_TYPE_DASHBOARD = "dashboard";
    private static final String ITEM_TYPE_CALCULATED_FIELD = "calculated field";
    private static final String ITEM_TYPE_RULE_CHAIN = "rule chain";
    private static final String ITEM_TYPE_DEVICE_PROFILE = "device profile";

    private final IotHubRestClient iotHubRestClient;
    private final TbWidgetTypeService tbWidgetTypeService;
    private final TbDashboardService tbDashboardService;
    private final TbCalculatedFieldService tbCalculatedFieldService;
    private final RuleChainService ruleChainService;
    private final TbRuleChainService tbRuleChainService;
    private final TbDeviceProfileService tbDeviceProfileService;
    private final AssetProfileService assetProfileService;
    private final TbAssetProfileService tbAssetProfileService;
    private final IotHubInstalledItemService iotHubInstalledItemService;
    private final WidgetTypeService widgetTypeService;
    private final DashboardService dashboardService;
    private final CalculatedFieldService calculatedFieldService;
    private final DeviceProfileService deviceProfileService;
    private final DeviceService deviceService;
    private final TbDeviceService tbDeviceService;
    private final SolutionService solutionService;

    @Override
    public InstallItemVersionResult installItemVersion(SecurityUser user, String versionId, JsonNode data, HttpServletRequest request) {
        TenantId tenantId = user.getTenantId();
        log.info("[{}] Installing IoT Hub item version: {}", tenantId, versionId);

        try {
            JsonNode versionInfo = iotHubRestClient.getVersionInfo(versionId);
            if (versionInfo == null) {
                throw new IllegalArgumentException("Failed to get version info from IoT Hub");
            }
            String itemType = versionInfo.get("type").asText();
            String itemName = versionInfo.get("name").asText();
            UUID itemId = UUID.fromString(versionInfo.get("itemId").asText());
            String version = versionInfo.get("version").asText();
            log.debug("[{}] Fetched version info: {} (type: {})", tenantId, itemName, itemType);

            byte[] fileData = iotHubRestClient.getVersionFileData(versionId);
            log.debug("[{}] Fetched file data, size: {} bytes", tenantId, fileData != null ? fileData.length : 0);

            IotHubInstalledItemDescriptor descriptor = switch (itemType) {
                case "WIDGET" -> installWidget(user, tenantId, fileData);
                case "DASHBOARD" -> installDashboard(user, tenantId, fileData);
                case "CALCULATED_FIELD" -> installCalculatedField(user, tenantId, fileData, data);
                case "ALARM_RULE" -> installAlarmRule(user, tenantId, fileData, data);
                case "RULE_CHAIN" -> installRuleChain(user, tenantId, fileData, data);
                case "DEVICE" -> installDeviceProfile(user, tenantId, fileData);
                case "SOLUTION_TEMPLATE" -> installSolution(user, tenantId, fileData, request);
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
            if (e instanceof EntitiesLimitExceededException el) {
                throw el;
            }
            return InstallItemVersionResult.error(e.getMessage());
        }
    }

    private WidgetInstalledItemDescriptor installWidget(SecurityUser user, TenantId tenantId, byte[] fileData) throws Exception {
        WidgetTypeDetails widgetTypeDetails;
        try {
            widgetTypeDetails = JacksonUtil.fromString(new String(fileData), WidgetTypeDetails.class, true);
        } catch (Exception e) {
            throw parseFailure(ACTION_INSTALL, ITEM_TYPE_WIDGET, e);
        }
        widgetTypeDetails.setId(null);
        widgetTypeDetails.setTenantId(tenantId);
        WidgetTypeDetails saved = tbWidgetTypeService.save(widgetTypeDetails, false, user);
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
            throw parseFailure(ACTION_INSTALL, ITEM_TYPE_DASHBOARD, e);
        }
        dashboard.setId(null);
        dashboard.setTenantId(tenantId);
        Dashboard saved = tbDashboardService.save(dashboard, user);
        log.debug("[{}] Dashboard installed: {}", tenantId, saved.getTitle());
        DashboardInstalledItemDescriptor descriptor = new DashboardInstalledItemDescriptor();
        descriptor.setDashboardId(saved.getId());
        return descriptor;
    }

    private CalculatedFieldInstalledItemDescriptor installCalculatedField(SecurityUser user, TenantId tenantId, byte[] fileData, JsonNode data) throws Exception {
        CalculatedField calculatedField;
        try {
            calculatedField = JacksonUtil.fromString(new String(fileData), CalculatedField.class, true);
        } catch (Exception e) {
            throw parseFailure(ACTION_INSTALL, ITEM_TYPE_CALCULATED_FIELD, e);
        }
        calculatedField.setId(null);
        calculatedField.setTenantId(tenantId);
        if (data != null && data.has("entityId")) {
            EntityId entityId = JacksonUtil.treeToValue(data.get("entityId"), EntityId.class);
            calculatedField.setEntityId(entityId);
        }
        CalculatedField saved = tbCalculatedFieldService.save(calculatedField, user);
        log.debug("[{}] Calculated field installed: {}", tenantId, saved.getName());
        CalculatedFieldInstalledItemDescriptor descriptor = new CalculatedFieldInstalledItemDescriptor();
        descriptor.setCalculatedFieldId(saved.getId());
        descriptor.setEntityId(saved.getEntityId());
        return descriptor;
    }

    private AlarmRuleInstalledItemDescriptor installAlarmRule(SecurityUser user, TenantId tenantId, byte[] fileData, JsonNode data) throws Exception {
        CalculatedField alarmRule;
        try {
            alarmRule = JacksonUtil.fromString(new String(fileData), CalculatedField.class, true);
        } catch (Exception e) {
            throw new Exception("Failed to parse alarm rule data: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()), e);
        }
        if (alarmRule.getType() != CalculatedFieldType.ALARM) {
            throw new Exception("Expected ALARM-type calculated field for ALARM_RULE install; got: " + alarmRule.getType());
        }
        alarmRule.setId(null);
        alarmRule.setTenantId(tenantId);
        if (data != null && data.has("entityId")) {
            EntityId entityId = JacksonUtil.treeToValue(data.get("entityId"), EntityId.class);
            alarmRule.setEntityId(entityId);
        }
        CalculatedField saved = tbCalculatedFieldService.save(alarmRule, user);
        log.debug("[{}] Alarm rule installed: {}", tenantId, saved.getName());
        AlarmRuleInstalledItemDescriptor descriptor = new AlarmRuleInstalledItemDescriptor();
        descriptor.setCalculatedFieldId(saved.getId());
        descriptor.setEntityId(saved.getEntityId());
        return descriptor;
    }

    private RuleChainInstalledItemDescriptor installRuleChain(
            SecurityUser user, TenantId tenantId, byte[] fileData, JsonNode data) throws Exception {
        JsonNode json = JacksonUtil.toJsonNode(new String(fileData));

        RuleChain ruleChain;
        try {
            ruleChain = JacksonUtil.fromString(json.get("ruleChain").toString(), RuleChain.class, true);
        } catch (Exception e) {
            throw parseFailure(ACTION_INSTALL, ITEM_TYPE_RULE_CHAIN, e);
        }

        RuleChainMetaData metadata;
        try {
            metadata = JacksonUtil.fromString(json.get("metadata").toString(), RuleChainMetaData.class, true);
        } catch (Exception e) {
            throw parseFailure(ACTION_INSTALL, ITEM_TYPE_RULE_CHAIN, SECTION_RULE_CHAIN_METADATA, e);
        }

        ruleChain.setId(null);
        ruleChain.setTenantId(tenantId);
        RuleChain savedRuleChain = ruleChainService.saveRuleChain(ruleChain);

        metadata.setRuleChainId(savedRuleChain.getId());
        metadata.setVersion(savedRuleChain.getVersion());
        ruleChainService.saveRuleChainMetaData(tenantId, metadata, tbRuleChainService::updateRuleNodeConfiguration);

        log.debug("[{}] Rule chain installed: {}", tenantId, savedRuleChain.getName());

        RuleChainInstalledItemDescriptor descriptor = new RuleChainInstalledItemDescriptor();
        descriptor.setRuleChainId(savedRuleChain.getId());

        if (data != null && data.has("entityId") && !data.get("entityId").isNull()) {
            EntityId entityId = JacksonUtil.treeToValue(data.get("entityId"), EntityId.class);
            setAsDefaultRuleChain(user, tenantId, entityId, savedRuleChain.getId());
            descriptor.setTargetProfileId(entityId);
        }

        return descriptor;
    }

    private void setAsDefaultRuleChain(SecurityUser user, TenantId tenantId,
                                       EntityId entityId, RuleChainId ruleChainId) throws Exception {
        switch (entityId.getEntityType()) {
            case DEVICE_PROFILE -> {
                DeviceProfile profile = deviceProfileService.findDeviceProfileById(
                        tenantId, (DeviceProfileId) entityId);
                if (profile == null) {
                    throw new IllegalArgumentException(
                            "Device profile not found: " + entityId.getId());
                }
                profile.setDefaultRuleChainId(ruleChainId);
                tbDeviceProfileService.save(profile, user);
                log.debug("[{}] Set rule chain {} as default on device profile {}",
                        tenantId, ruleChainId.getId(), entityId.getId());
            }
            case ASSET_PROFILE -> {
                AssetProfile profile = assetProfileService.findAssetProfileById(
                        tenantId, (AssetProfileId) entityId);
                if (profile == null) {
                    throw new IllegalArgumentException(
                            "Asset profile not found: " + entityId.getId());
                }
                profile.setDefaultRuleChainId(ruleChainId);
                tbAssetProfileService.save(profile, user);
                log.debug("[{}] Set rule chain {} as default on asset profile {}",
                        tenantId, ruleChainId.getId(), entityId.getId());
            }
            default -> throw new IllegalArgumentException(
                    "Rule chain can only be set as default on device or asset profile, got: "
                            + entityId.getEntityType());
        }
    }

    private DeviceInstalledItemDescriptor installDeviceProfile(SecurityUser user, TenantId tenantId, byte[] fileData) throws Exception {
        DeviceProfile deviceProfile;
        try {
            deviceProfile = JacksonUtil.fromString(new String(fileData), DeviceProfile.class, true);
        } catch (Exception e) {
            throw parseFailure(ACTION_INSTALL, ITEM_TYPE_DEVICE_PROFILE, e);
        }
        deviceProfile.setId(null);
        deviceProfile.setTenantId(tenantId);
        tbDeviceProfileService.save(deviceProfile, user);
        log.debug("[{}] Device profile installed: {}", tenantId, deviceProfile.getName());
        return new DeviceInstalledItemDescriptor();
    }

    private SolutionTemplateInstalledItemDescriptor installSolution(SecurityUser user, TenantId tenantId, byte[] fileData, HttpServletRequest request) throws Exception {
        SolutionInstallResponse response = solutionService.installSolution(user, tenantId, fileData, request);
        if (!response.isSuccess()) {
            throw new RuntimeException(response.getDetails());
        }
        SolutionTemplateInstalledItemDescriptor descriptor = new SolutionTemplateInstalledItemDescriptor();
        descriptor.setCreatedEntityIds(response.getCreatedEntityIds());
        descriptor.setTenantTelemetryKeys(response.getTenantTelemetryKeys());
        descriptor.setTenantAttributeKeys(response.getTenantAttributeKeys());
        descriptor.setDashboardId(response.getDashboardId());
        descriptor.setPublicId(response.getPublicId());
        descriptor.setMainDashboardPublic(response.isMainDashboardPublic());
        descriptor.setDetails(response.getDetails());
        return descriptor;
    }

    @Override
    public UpdateItemVersionResult updateItemVersion(SecurityUser user, IotHubInstalledItemId installedItemId, String versionId, boolean force, HttpServletRequest request) {
        TenantId tenantId = user.getTenantId();
        log.info("[{}] Updating IoT Hub installed item {} to version: {}", tenantId, installedItemId, versionId);

        try {
            IotHubInstalledItem installedItem = iotHubInstalledItemService.findById(tenantId, installedItemId);
            if (installedItem == null) {
                throw new IllegalArgumentException("Installed item not found");
            }

            String itemType = installedItem.getItemType();
            IotHubInstalledItemDescriptor descriptor = installedItem.getDescriptor();

            // Skip checksum validation for solution templates
            if (!"SOLUTION_TEMPLATE".equals(itemType)) {
                JsonNode installedVersionInfo = iotHubRestClient.getVersionInfo(installedItem.getItemVersionId().toString());
                if (installedVersionInfo == null) {
                    throw new IllegalArgumentException("Failed to get installed version info from IoT Hub");
                }
                String installedChecksum = installedVersionInfo.has("checksum") ? installedVersionInfo.get("checksum").asText() : null;
                log.info("[{}] Installed version info: name={}, version={}, checksum={}", tenantId, installedItem.getItemName(), installedItem.getVersion(), installedChecksum);

                if (!force) {
                    String entityChecksum = calculateEntityChecksum(tenantId, installedItem);
                    boolean entityModified = installedChecksum != null && !installedChecksum.equals(entityChecksum);
                    log.info("[{}] Entity checksum: {}, modified: {}", tenantId, entityChecksum, entityModified);

                    if (entityModified) {
                        return UpdateItemVersionResult.entityModified();
                    }
                }
            }

            JsonNode versionInfo = iotHubRestClient.getVersionInfo(versionId);

            if (versionInfo == null) {
                throw new IllegalArgumentException("Failed to get version info from IoT Hub");
            }

            String updateItemType = versionInfo.get("type").asText();
            if (!itemType.equals(updateItemType)) {
                throw new IllegalArgumentException("Installed item type does not match the new version's item type.");
            }

            String itemName = versionInfo.get("name").asText();
            String version = versionInfo.get("version").asText();

            byte[] fileData = iotHubRestClient.getVersionFileData(versionId);
            log.info("[{}] Fetched update file data, size: {} bytes", tenantId, fileData != null ? fileData.length : 0);

            switch (itemType) {
                case "WIDGET" -> updateWidget(user, tenantId, (WidgetInstalledItemDescriptor) descriptor, fileData);
                case "DASHBOARD" -> updateDashboard(user, tenantId, (DashboardInstalledItemDescriptor) descriptor, fileData);
                case "CALCULATED_FIELD" -> updateCalculatedField(user, tenantId, (CalculatedFieldInstalledItemDescriptor) descriptor, fileData);
                case "ALARM_RULE" -> updateAlarmRule(user, tenantId, (AlarmRuleInstalledItemDescriptor) descriptor, fileData);
                case "RULE_CHAIN" -> updateRuleChain(tenantId, (RuleChainInstalledItemDescriptor) descriptor, fileData);
                case "DEVICE" -> {
                    DeviceInstalledItemDescriptor dd = (DeviceInstalledItemDescriptor) descriptor;
                    deleteDevicePackageEntities(tenantId, dd.getCreatedEntityIds(), user);
                    dd.setCreatedEntityIds(null);
                    dd.setDashboardId(null);
                }
                case "SOLUTION_TEMPLATE" -> {
                    SolutionTemplateInstalledItemDescriptor stDescriptor = (SolutionTemplateInstalledItemDescriptor) descriptor;
                    solutionService.deleteSolution(tenantId, stDescriptor, user);
                    SolutionInstallResponse response = solutionService.installSolution(user, tenantId, fileData, request);
                    if (!response.isSuccess()) {
                        throw new RuntimeException(response.getDetails());
                    }
                    stDescriptor.setCreatedEntityIds(response.getCreatedEntityIds());
                    stDescriptor.setTenantTelemetryKeys(response.getTenantTelemetryKeys());
                    stDescriptor.setTenantAttributeKeys(response.getTenantAttributeKeys());
                    stDescriptor.setDashboardId(response.getDashboardId());
                    stDescriptor.setPublicId(response.getPublicId());
                    stDescriptor.setMainDashboardPublic(response.isMainDashboardPublic());
                    stDescriptor.setDetails(response.getDetails());
                }
                default -> throw new IllegalArgumentException("Unsupported IoT Hub item type: " + itemType);
            }

            installedItem.setItemVersionId(UUID.fromString(versionId));
            installedItem.setItemName(itemName);
            installedItem.setVersion(version);
            iotHubInstalledItemService.save(tenantId, installedItem);

            log.info("[{}] Successfully updated IoT Hub item {} to version: {}", tenantId, itemName, version);

            return UpdateItemVersionResult.success(installedItem.getDescriptor());
        } catch (Exception e) {
            log.error("[{}] Failed to update IoT Hub installed item {} to version: {}", tenantId, installedItemId, versionId, e);
            return UpdateItemVersionResult.error(e.getMessage());
        }
    }

    private void updateWidget(SecurityUser user, TenantId tenantId, WidgetInstalledItemDescriptor descriptor, byte[] fileData) throws Exception {
        WidgetTypeDetails newWidgetType;
        try {
            newWidgetType = JacksonUtil.fromString(new String(fileData), WidgetTypeDetails.class, true);
        } catch (Exception e) {
            throw parseFailure(ACTION_UPDATE, ITEM_TYPE_WIDGET, e);
        }
        WidgetTypeDetails existing = widgetTypeService.findWidgetTypeDetailsById(tenantId, descriptor.getWidgetTypeId());
        if (existing == null) {
            throw new Exception("Widget not found for update");
        }
        existing.setName(newWidgetType.getName());
        existing.setDescriptor(newWidgetType.getDescriptor());
        tbWidgetTypeService.save(existing, false, user);
    }

    private void updateDashboard(SecurityUser user, TenantId tenantId, DashboardInstalledItemDescriptor descriptor, byte[] fileData) throws Exception {
        Dashboard newDashboard;
        try {
            newDashboard = JacksonUtil.fromString(new String(fileData), Dashboard.class, true);
        } catch (Exception e) {
            throw parseFailure(ACTION_UPDATE, ITEM_TYPE_DASHBOARD, e);
        }
        Dashboard existing = dashboardService.findDashboardById(tenantId, descriptor.getDashboardId());
        if (existing == null) {
            throw new Exception("Dashboard not found for update");
        }
        existing.setTitle(newDashboard.getTitle());
        existing.setConfiguration(newDashboard.getConfiguration());
        tbDashboardService.save(existing, user);
    }

    private void updateCalculatedField(SecurityUser user, TenantId tenantId, CalculatedFieldInstalledItemDescriptor descriptor, byte[] fileData) throws Exception {
        CalculatedField newCf;
        try {
            newCf = JacksonUtil.fromString(new String(fileData), CalculatedField.class, true);
        } catch (Exception e) {
            throw parseFailure(ACTION_UPDATE, ITEM_TYPE_CALCULATED_FIELD, e);
        }
        CalculatedField existing = calculatedFieldService.findById(tenantId, descriptor.getCalculatedFieldId());
        if (existing == null) {
            throw new Exception("Calculated field not found for update");
        }
        existing.setName(newCf.getName());
        existing.setType(newCf.getType());
        existing.setConfiguration(newCf.getConfiguration());
        tbCalculatedFieldService.save(existing, user);
    }

    private void updateAlarmRule(SecurityUser user, TenantId tenantId, AlarmRuleInstalledItemDescriptor descriptor, byte[] fileData) throws Exception {
        CalculatedField newCf;
        try {
            newCf = JacksonUtil.fromString(new String(fileData), CalculatedField.class, true);
        } catch (Exception e) {
            throw new Exception("Failed to parse alarm rule data: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()), e);
        }
        if (newCf.getType() != CalculatedFieldType.ALARM) {
            throw new Exception("Expected ALARM-type calculated field for ALARM_RULE update; got: " + newCf.getType());
        }
        CalculatedField existing = calculatedFieldService.findById(tenantId, descriptor.getCalculatedFieldId());
        if (existing == null) {
            throw new Exception("Alarm rule not found for update");
        }
        existing.setName(newCf.getName());
        existing.setType(newCf.getType());
        existing.setConfiguration(newCf.getConfiguration());
        tbCalculatedFieldService.save(existing, user);
    }

    private void updateRuleChain(TenantId tenantId, RuleChainInstalledItemDescriptor descriptor, byte[] fileData) throws Exception {
        JsonNode json = JacksonUtil.toJsonNode(new String(fileData));
        RuleChainMetaData metadata;
        try {
            metadata = JacksonUtil.fromString(json.get("metadata").toString(), RuleChainMetaData.class, true);
        } catch (Exception e) {
            throw parseFailure(ACTION_UPDATE, ITEM_TYPE_RULE_CHAIN, SECTION_RULE_CHAIN_METADATA, e);
        }
        RuleChain existing = ruleChainService.findRuleChainById(tenantId, descriptor.getRuleChainId());
        if (existing == null) {
            throw new Exception("Rule chain not found for update");
        }
        RuleChain newRuleChain;
        try {
            newRuleChain = JacksonUtil.fromString(json.get("ruleChain").toString(), RuleChain.class, true);
        } catch (Exception e) {
            throw parseFailure(ACTION_UPDATE, ITEM_TYPE_RULE_CHAIN, e);
        }
        existing.setName(newRuleChain.getName());
        RuleChain savedRuleChain = ruleChainService.saveRuleChain(existing);
        metadata.setRuleChainId(savedRuleChain.getId());
        metadata.setVersion(savedRuleChain.getVersion());
        ruleChainService.saveRuleChainMetaData(tenantId, metadata, tbRuleChainService::updateRuleNodeConfiguration);
    }

    private String calculateEntityChecksum(TenantId tenantId, IotHubInstalledItem installedItem) {
        IotHubInstalledItemDescriptor descriptor = installedItem.getDescriptor();
        if (descriptor instanceof WidgetInstalledItemDescriptor wd) {
            return calculateWidgetChecksum(tenantId, wd);
        } else if (descriptor instanceof DashboardInstalledItemDescriptor dd) {
            return calculateDashboardChecksum(tenantId, dd);
        } else if (descriptor instanceof CalculatedFieldInstalledItemDescriptor cd) {
            return calculateCalculatedFieldChecksum(tenantId, cd.getCalculatedFieldId());
        } else if (descriptor instanceof AlarmRuleInstalledItemDescriptor ad) {
            return calculateCalculatedFieldChecksum(tenantId, ad.getCalculatedFieldId());
        } else if (descriptor instanceof RuleChainInstalledItemDescriptor rd) {
            return calculateRuleChainChecksum(tenantId, rd);
        }
        return null;
    }

    private String calculateCalculatedFieldChecksum(TenantId tenantId, CalculatedFieldId calculatedFieldId) {
        CalculatedField cf = calculatedFieldService.findById(tenantId, calculatedFieldId);
        if (cf == null) {
            return null;
        }
        String content = (cf.getName() != null ? cf.getName() : "") +
                (cf.getType() != null ? cf.getType().name() : "") +
                (cf.getConfiguration() != null ? JacksonUtil.valueToTree(cf.getConfiguration()).toString() : "");
        return sha256(content);
    }

    private String calculateDashboardChecksum(TenantId tenantId, DashboardInstalledItemDescriptor descriptor) {
        Dashboard dashboard = dashboardService.findDashboardById(tenantId, descriptor.getDashboardId());
        if (dashboard == null) {
            return null;
        }
        String content = (dashboard.getTitle() != null ? dashboard.getTitle() : "") +
                (dashboard.getConfiguration() != null ? dashboard.getConfiguration().toString() : "");
        return sha256(content);
    }

    private String calculateWidgetChecksum(TenantId tenantId, WidgetInstalledItemDescriptor descriptor) {
        WidgetTypeDetails widgetType = widgetTypeService.findWidgetTypeDetailsById(tenantId, descriptor.getWidgetTypeId());
        if (widgetType == null) {
            return null;
        }
        String content = (widgetType.getFqn() != null ? widgetType.getFqn() : "") +
                (widgetType.getName() != null ? widgetType.getName() : "") +
                (widgetType.getDescriptor() != null ? widgetType.getDescriptor().toString() : "");
        return sha256(content);
    }

    private String calculateRuleChainChecksum(TenantId tenantId, RuleChainInstalledItemDescriptor descriptor) {
        RuleChain ruleChain = ruleChainService.findRuleChainById(tenantId, descriptor.getRuleChainId());
        if (ruleChain == null) {
            return null;
        }
        RuleChainMetaData metadata = ruleChainService.loadRuleChainMetaData(tenantId, descriptor.getRuleChainId());
        StringBuilder content = new StringBuilder();
        content.append(ruleChain.getName() != null ? ruleChain.getName() : "");
        content.append(ruleChain.getType() != null ? ruleChain.getType().name() : "");
        if (metadata.getNodes() != null) {
            for (RuleNode node : metadata.getNodes()) {
                content.append(node.getType() != null ? node.getType() : "");
                content.append(node.getName() != null ? node.getName() : "");
                content.append(node.getConfiguration() != null ? node.getConfiguration().toString() : "");
            }
        }
        if (metadata.getConnections() != null) {
            for (NodeConnectionInfo conn : metadata.getConnections()) {
                content.append(conn.getFromIndex());
                content.append(conn.getToIndex());
                content.append(conn.getType() != null ? conn.getType() : "");
            }
        }
        return sha256(content.toString());
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate SHA-256", e);
        }
    }

    @Override
    public InstallItemVersionResult registerDeviceInstall(SecurityUser user, String versionId, DeviceInstalledItemDescriptor descriptor) {
        TenantId tenantId = user.getTenantId();
        log.info("[{}] Registering device package install for version: {}", tenantId, versionId);

        try {
            JsonNode versionInfo = iotHubRestClient.getVersionInfo(versionId);

            if (versionInfo == null) {
                throw new IllegalArgumentException("Failed to get version info from IoT Hub");
            }

            String itemName = versionInfo.get("name").asText();
            UUID itemId = UUID.fromString(versionInfo.get("itemId").asText());
            String version = versionInfo.get("version").asText();

            IotHubInstalledItem installedItem = new IotHubInstalledItem();
            installedItem.setTenantId(tenantId);
            installedItem.setItemId(itemId);
            installedItem.setItemVersionId(UUID.fromString(versionId));
            installedItem.setItemName(itemName);
            installedItem.setItemType("DEVICE");
            installedItem.setVersion(version);
            installedItem.setDescriptor(descriptor);
            iotHubInstalledItemService.save(tenantId, installedItem);

            iotHubRestClient.reportVersionInstalled(versionId);
            log.info("[{}] Registered device package install: {} (version {})", tenantId, itemName, version);

            return InstallItemVersionResult.success(descriptor);
        } catch (Exception e) {
            log.error("[{}] Failed to register device package install: {}", tenantId, versionId, e);
            return InstallItemVersionResult.error(e.getMessage());
        }
    }

    private void deleteDevicePackageEntities(TenantId tenantId, List<EntityId> createdEntityIds, SecurityUser user) {
        if (createdEntityIds == null || createdEntityIds.isEmpty()) {
            return;
        }
        List<EntityId> reversed = new ArrayList<>(createdEntityIds);
        Collections.reverse(reversed);
        for (EntityId entityId : reversed) {
            try {
                deleteDevicePackageEntity(tenantId, entityId, user);
            } catch (Exception e) {
                log.error("[{}] Failed to delete device package entity: {}", tenantId, entityId, e);
            }
        }
    }

    private void deleteDevicePackageEntity(TenantId tenantId, EntityId entityId, SecurityUser user) {
        switch (entityId.getEntityType()) {
            case DEVICE -> {
                var device = deviceService.findDeviceById(tenantId, new DeviceId(entityId.getId()));
                if (device != null) tbDeviceService.delete(device, user);
            }
            case DEVICE_PROFILE -> {
                var profile = deviceProfileService.findDeviceProfileById(tenantId, new DeviceProfileId(entityId.getId()));
                if (profile != null) tbDeviceProfileService.delete(profile, user);
            }
            case DASHBOARD -> {
                var dashboard = dashboardService.findDashboardById(tenantId, new DashboardId(entityId.getId()));
                if (dashboard != null) tbDashboardService.delete(dashboard, user);
            }
            case RULE_CHAIN -> {
                var ruleChain = ruleChainService.findRuleChainById(tenantId, new RuleChainId(entityId.getId()));
                if (ruleChain != null) tbRuleChainService.delete(ruleChain, user);
            }
            default -> log.warn("[{}] Unsupported entity type for device package delete: {}", tenantId, entityId.getEntityType());
        }
    }

    @Override
    public void deleteInstalledItem(SecurityUser user, IotHubInstalledItemId installedItemId) {
        TenantId tenantId = user.getTenantId();
        IotHubInstalledItem installedItem = iotHubInstalledItemService.findById(tenantId, installedItemId);
        if (installedItem == null) {
            throw new IllegalArgumentException("Installed item not found");
        }

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
        } else if (descriptor instanceof AlarmRuleInstalledItemDescriptor ad) {
            CalculatedField alarmRule = calculatedFieldService.findById(tenantId, ad.getCalculatedFieldId());
            if (alarmRule != null) {
                tbCalculatedFieldService.delete(alarmRule, user);
            }
        } else if (descriptor instanceof RuleChainInstalledItemDescriptor rd) {
            if (rd.getRuleChainId() != null) {
                RuleChain ruleChain = ruleChainService.findRuleChainById(tenantId, rd.getRuleChainId());
                if (ruleChain != null) {
                    tbRuleChainService.delete(ruleChain, user);
                }
            }
        } else if (descriptor instanceof DeviceInstalledItemDescriptor dd) {
            deleteDevicePackageEntities(tenantId, dd.getCreatedEntityIds(), user);
        } else if (descriptor instanceof SolutionTemplateInstalledItemDescriptor st) {
            try {
                solutionService.deleteSolution(tenantId, st, user);
            } catch (ThingsboardException e) {
                log.error("[{}] Failed to delete solution for installed item {}", tenantId, installedItemId, e);
            }
        }

        iotHubInstalledItemService.deleteById(tenantId, installedItemId);
        log.info("[{}] Deleted installed IoT Hub item: {}", tenantId, installedItem.getItemName());
    }

    private static Exception parseFailure(String action, String itemTypeName, Exception cause) {
        return parseFailure(action, itemTypeName, SECTION_PACKAGE_DATA, cause);
    }

    private static Exception parseFailure(String action, String itemTypeName, String section, Exception cause) {
        return new Exception("Unable to " + action + " this " + itemTypeName + " — the " + section + " could not be parsed. " +
                "The package may be corrupted or in an unexpected format; please contact the creator to have it re-published.", cause);
    }
}
