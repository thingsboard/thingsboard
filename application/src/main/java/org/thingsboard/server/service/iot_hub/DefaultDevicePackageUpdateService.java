// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.iot_hub;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClientException;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.AttributesSaveRequest;
import org.thingsboard.server.common.adaptor.JsonConverter;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.HasVersion;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.exception.EntityVersionMismatchException;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.IotHubInstalledItemId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.iot_hub.DeviceInstalledItemDescriptor;
import org.thingsboard.server.common.data.iot_hub.IotHubInstalledItem;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.converter.ConverterService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.integration.IntegrationService;
import org.thingsboard.server.dao.iot_hub.IotHubInstalledItemService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.secret.SecretConfigurationService;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.exception.EntitiesLimitExceededException;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;
import org.thingsboard.server.service.integration.IntegrationManagerService;
import org.thingsboard.server.service.rule.TbRuleChainService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Applies an update of an installed device package in one transaction. The install wizard resolves the templates
 * of the new version (the same way it installs them) and sends the entity of each step; this service checks the
 * plan, saves every entity and the new version of the installed item together, and rolls all of it back on a
 * failure. The side effects of the saves (actors, integration restart, edge, caches) run after commit, so a
 * rollback leaves no trace; the audit log, version control and the attributes, which aren't transactional,
 * are written after commit too.
 */
@Service
@TbCoreComponent
@Slf4j
@RequiredArgsConstructor
public class DefaultDevicePackageUpdateService extends AbstractTbEntityService implements DevicePackageUpdateService {

    // Step type of the package -> the alias its templates refer to it by, e.g. ${deviceProfile.id}
    private static final Map<String, String> STEP_ALIASES = Map.of(
            "DEVICE_PROFILE", "deviceProfile",
            "DEVICE", "device",
            "GATEWAY", "gateway",
            "DASHBOARD", "dashboard",
            "RULE_CHAIN", "ruleChain",
            "UPLINK_CONVERTER", "uplinkConverter",
            "DOWNLINK_CONVERTER", "downlinkConverter",
            "INTEGRATION", "integration");
    private static final Pattern ENTITY_PLACEHOLDER = Pattern.compile("\\$\\{([A-Za-z]+)\\.([A-Za-z]+)}");
    private static final int INTEGRATION_VALIDATION_TIMEOUT_SEC = 20;
    private static final String UNEXPECTED_ERROR = "Failed to update the device package";

    private final IotHubInstalledItemService installedItemService;
    private final IotHubRestClient iotHubRestClient;
    private final IotHubService iotHubService;
    private final DevicePackageEntities packageEntities;
    private final DeviceProfileService deviceProfileService;
    private final DeviceService deviceService;
    private final DeviceCredentialsService deviceCredentialsService;
    private final DashboardService dashboardService;
    private final RuleChainService ruleChainService;
    private final TbRuleChainService tbRuleChainService;
    private final ConverterService converterService;
    private final IntegrationService integrationService;
    private final IntegrationManagerService integrationManagerService;
    private final SecretConfigurationService secretConfigurationService;
    private final AttributesService attributesService;
    private final TelemetrySubscriptionService telemetryService;
    private final TransactionTemplate transactionTemplate;

    @Override
    public List<DevicePackageEntityState> check(SecurityUser user, IotHubInstalledItemId installedItemId) {
        TenantId tenantId = user.getTenantId();
        DeviceInstalledItemDescriptor descriptor = findDevicePackage(tenantId, installedItemId).descriptor();
        Set<UUID> sharedIds = new HashSet<>();
        iotHubService.findKeptEntities(tenantId, installedItemId).getEntities().forEach(e -> sharedIds.add(e.getEntityId().getId()));
        Map<UUID, EntityId> createdIds = createdIds(descriptor);

        List<DevicePackageEntityState> states = new ArrayList<>();
        if (descriptor.getInstallState() == null) {
            return states;
        }
        descriptor.getInstallState().forEach((stepName, state) -> {
            EntityId entityId = DevicePackageEntities.entityIdOf(state, createdIds);
            if (entityId == null || !DevicePackageEntities.CHECKSUM_TYPES.contains(entityId.getEntityType())) {
                return;
            }
            DevicePackageEntities.Snapshot snapshot = packageEntities.load(tenantId, entityId);
            JsonNode storedChecksum = state.get("checksum");
            Boolean modified = snapshot == null || storedChecksum == null || !storedChecksum.isTextual() ? null :
                    !storedChecksum.asText().equals(snapshot.checksum());
            states.add(new DevicePackageEntityState(stepName, entityId, snapshot != null,
                    snapshot != null ? snapshot.version() : null, modified, sharedIds.contains(entityId.getId())));
        });
        return states;
    }

    @Override
    public DeviceUpdateResult apply(SecurityUser user, IotHubInstalledItemId installedItemId, DeviceUpdateRequest request) {
        TenantId tenantId = user.getTenantId();
        log.info("[{}] Updating device package {} to version {}", tenantId, installedItemId, request.getVersionId());
        Applied applied;
        try {
            DevicePackage devicePackage = findDevicePackage(tenantId, installedItemId);
            JsonNode versionInfo = iotHubRestClient.getVersionInfo(request.getVersionId());
            if (versionInfo == null || !devicePackage.installedItem().getItemId().toString().equals(versionInfo.path("itemId").asText())) {
                throw new IllegalArgumentException("The version belongs to another item");
            }
            validatePlan(devicePackage, request);
            validateIntegrations(tenantId, request);
            applied = transactionTemplate.execute(status -> applySteps(tenantId, devicePackage, request, versionInfo));
        } catch (StepFailure e) {
            log.warn("[{}] Device package update of {} failed on step [{}], nothing was changed", tenantId, installedItemId, e.stepName, e.getCause());
            return DeviceUpdateResult.error(userMessage(e.getCause()), e.stepName);
        } catch (RestClientException e) {
            log.error("[{}] IoT Hub request failed while updating device package {}", tenantId, installedItemId, e);
            return DeviceUpdateResult.error("IoT Hub is unavailable. Try again later.", null);
        } catch (Exception e) {
            log.warn("[{}] Device package update of {} failed, nothing was changed", tenantId, installedItemId, e);
            return DeviceUpdateResult.error(userMessage(e), null);
        }
        afterCommit(user, tenantId, applied);
        log.info("[{}] Updated device package {} to version {}", tenantId, installedItemId, request.getVersionId());
        return DeviceUpdateResult.success(applied.descriptor(), applied.outputs());
    }

    private DevicePackage findDevicePackage(TenantId tenantId, IotHubInstalledItemId installedItemId) {
        IotHubInstalledItem installedItem = installedItemService.findById(tenantId, installedItemId);
        if (installedItem == null || !"DEVICE".equals(installedItem.getItemType())
                || !(installedItem.getDescriptor() instanceof DeviceInstalledItemDescriptor descriptor)) {
            throw new IllegalArgumentException("Installed device package not found");
        }
        return new DevicePackage(installedItem, descriptor);
    }

    /**
     * The update may only change what the package owns, or what the user confirmed. The device and the gateway,
     * whose credentials the firmware uses, are never changed.
     */
    private void validatePlan(DevicePackage devicePackage, DeviceUpdateRequest request) {
        if (request.getSteps() == null || request.getSteps().isEmpty()) {
            throw new IllegalArgumentException("The update has no steps");
        }
        Set<UUID> ownedIds = createdIds(devicePackage.descriptor()).keySet();
        for (DeviceUpdateRequest.Step step : request.getSteps()) {
            if (step.getName() == null || step.getAction() == null || !isKnownType(step.getType())) {
                throw new IllegalArgumentException("Invalid update step: " + step.getName());
            }
            boolean deviceOrGateway = "DEVICE".equals(step.getType()) || "GATEWAY".equals(step.getType());
            switch (step.getAction()) {
                case KEEP, OVERWRITE -> {
                    if (step.getEntityId() == null && !"GATEWAY_CONNECTOR".equals(step.getType())) {
                        throw new IllegalArgumentException("No entity for update step: " + step.getName());
                    }
                    boolean owned = step.getEntityId() != null && ownedIds.contains(step.getEntityId());
                    if (step.getAction() == DeviceUpdateRequest.Action.OVERWRITE && !owned && !step.isConfirmed()
                            && !"GATEWAY_CONNECTOR".equals(step.getType())) {
                        throw new IllegalArgumentException("Overwriting an entity of another package needs a confirmation: " + step.getName());
                    }
                    if (step.getAction() == DeviceUpdateRequest.Action.OVERWRITE && deviceOrGateway && owned) {
                        throw new IllegalArgumentException("The device and its credentials don't change on update");
                    }
                }
                case CREATE -> {
                }
            }
            if (step.getAction() != DeviceUpdateRequest.Action.KEEP && (step.getEntity() == null || !step.getEntity().isObject())) {
                throw new IllegalArgumentException("No entity data for update step: " + step.getName());
            }
        }
    }

    /** The configuration of an integration is checked by a remote call, so before the transaction. */
    private void validateIntegrations(TenantId tenantId, DeviceUpdateRequest request) throws Exception {
        for (DeviceUpdateRequest.Step step : request.getSteps()) {
            if (!"INTEGRATION".equals(step.getType()) || step.getAction() == DeviceUpdateRequest.Action.KEEP) {
                continue;
            }
            ObjectNode body = step.getEntity().deepCopy();
            // Converters this update creates have no id yet; the configuration doesn't depend on them
            body.remove(List.of("id", "defaultConverterId", "downlinkConverterId"));
            Integration integration = JacksonUtil.treeToValue(body, Integration.class);
            integration.setTenantId(tenantId);
            if (integration.isEdgeTemplate()) {
                continue;
            }
            try {
                secretConfigurationService.replaceSecretUsages(tenantId, integration.getConfiguration());
                integrationManagerService.validateIntegrationConfiguration(integration).get(INTEGRATION_VALIDATION_TIMEOUT_SEC, TimeUnit.SECONDS);
            } catch (ExecutionException e) {
                // What the integration reports, e.g. wrong credentials, is what the user has to fix
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                throw new StepFailure(step.getName(), new IllegalArgumentException(cause.getMessage(), cause));
            } catch (TimeoutException e) {
                throw new StepFailure(step.getName(), new IllegalStateException("Timeout to validate the integration configuration"));
            }
        }
    }

    private Applied applySteps(TenantId tenantId, DevicePackage devicePackage, DeviceUpdateRequest request, JsonNode versionInfo) {
        DeviceInstalledItemDescriptor previous = devicePackage.descriptor();
        Map<String, ObjectNode> aliasOutputs = new HashMap<>();
        Map<String, ObjectNode> outputs = new LinkedHashMap<>();
        Map<String, ObjectNode> installState = new LinkedHashMap<>();
        if (request.getInstallState() != null) {
            request.getInstallState().forEach((key, value) -> {
                if (value != null && value.has("formValues")) {
                    installState.put(key, JacksonUtil.newObjectNode().set("formValues", value.get("formValues")));
                }
            });
        }
        List<EntityId> stepEntityIds = new ArrayList<>();
        List<SavedEntity> saved = new ArrayList<>();
        List<DeviceUpdateRequest.Step> afterCommitSteps = new ArrayList<>();
        DashboardId dashboardId = null;

        for (DeviceUpdateRequest.Step step : request.getSteps()) {
            try {
                if ("GATEWAY_CONNECTOR".equals(step.getType())) {
                    // A connector lives in the gateway's attributes, which aren't transactional: saved after commit
                    if (step.getAction() != DeviceUpdateRequest.Action.KEEP) {
                        DeviceUpdateRequest.Step resolved = copyWithEntity(step, resolvePlaceholders(step.getEntity(), aliasOutputs));
                        afterCommitSteps.add(resolved);
                    }
                    continue;
                }
                JsonNode entity = step.getEntity() != null ? resolvePlaceholders(step.getEntity(), aliasOutputs) : null;
                StepResult result = applyStep(tenantId, step, entity);
                ObjectNode output = result.output();
                aliasOutputs.put(STEP_ALIASES.get(step.getType()), output);
                outputs.put(step.getName(), output);
                ObjectNode state = JacksonUtil.newObjectNode();
                state.set("entityOutput", output);
                state.put("resolution", step.getResolution() != null ? step.getResolution() : "created");
                // A kept entity keeps the checksum of what the package left: the current one would take the
                // user's changes for the package's, and the next update would overwrite them without a warning
                String checksum = step.getAction() == DeviceUpdateRequest.Action.KEEP ? previousChecksum(previous, step.getName()) : result.checksum();
                if (checksum != null) {
                    state.put("checksum", checksum);
                }
                installState.put(step.getName(), state);
                if (!"use-existing".equals(step.getResolution())) {
                    stepEntityIds.add(result.entityId());
                }
                if (result.entityId() instanceof DashboardId id) {
                    dashboardId = id;
                }
                if (result.saved() != null) {
                    saved.add(result.saved());
                }
                if (step.getAction() == DeviceUpdateRequest.Action.CREATE && ("DEVICE".equals(step.getType()) || "GATEWAY".equals(step.getType()))) {
                    // Its attributes are saved after commit, once the device exists
                    DeviceUpdateRequest.Step created = copyWithEntity(step, null);
                    created.setEntityId(result.entityId().getId());
                    afterCommitSteps.add(created);
                }
            } catch (StepFailure e) {
                throw e;
            } catch (Exception e) {
                throw new StepFailure(step.getName(), e);
            }
        }

        DeviceInstalledItemDescriptor descriptor = new DeviceInstalledItemDescriptor();
        // The new version may not offer the method the package was installed with
        descriptor.setSelectedInstallMethod(request.getSelectedInstallMethod() != null ? request.getSelectedInstallMethod() : previous.getSelectedInstallMethod());
        descriptor.setCreatedEntityIds(createdEntityIds(tenantId, previous, stepEntityIds));
        descriptor.setDashboardId(dashboardId != null ? dashboardId : existingDashboard(tenantId, previous));
        Map<String, JsonNode> state = new LinkedHashMap<>(installState);
        descriptor.setInstallState(state);

        IotHubInstalledItem installedItem = devicePackage.installedItem();
        installedItem.setItemVersionId(UUID.fromString(request.getVersionId()));
        installedItem.setItemName(versionInfo.get("name").asText());
        installedItem.setVersion(versionInfo.get("version").asText());
        installedItem.setDescriptor(descriptor);
        installedItemService.save(tenantId, installedItem);
        return new Applied(descriptor, outputs, saved, afterCommitSteps, aliasOutputs);
    }

    private StepResult applyStep(TenantId tenantId, DeviceUpdateRequest.Step step, JsonNode entity) throws ThingsboardException {
        return switch (step.getType()) {
            case "DEVICE_PROFILE" -> saveDeviceProfile(tenantId, step, entity);
            case "DEVICE", "GATEWAY" -> saveDevice(tenantId, step, entity);
            case "DASHBOARD" -> saveDashboard(tenantId, step, entity);
            case "RULE_CHAIN" -> saveRuleChain(tenantId, step, entity);
            case "UPLINK_CONVERTER", "DOWNLINK_CONVERTER" -> saveConverter(tenantId, step, entity);
            case "INTEGRATION" -> saveIntegration(tenantId, step, entity);
            default -> throw new IllegalArgumentException("Unsupported step type: " + step.getType());
        };
    }

    private StepResult saveDeviceProfile(TenantId tenantId, DeviceUpdateRequest.Step step, JsonNode entity) {
        DeviceProfileId id = step.getEntityId() != null ? new DeviceProfileId(step.getEntityId()) : null;
        DeviceProfile current = id != null ? deviceProfileService.findDeviceProfileById(tenantId, id) : null;
        if (step.getAction() == DeviceUpdateRequest.Action.KEEP) {
            existing(tenantId, current, step);
            return kept(id, current.getName());
        }
        DeviceProfile profile = JacksonUtil.treeToValue(entity, DeviceProfile.class);
        profile.setTenantId(tenantId);
        if (step.getAction() == DeviceUpdateRequest.Action.OVERWRITE) {
            existing(tenantId, current, step);
            prepareOverwrite(profile, id, current, step);
            // The tenant's default profile stays the default one
            profile.setDefault(current.isDefault());
        } else {
            profile.setId(null);
            profile.setVersion(null);
        }
        DeviceProfile result = deviceProfileService.saveDeviceProfile(profile);
        return savedResult(tenantId, result.getId(), result, step, output(result.getId(), result.getName()));
    }

    private StepResult saveDevice(TenantId tenantId, DeviceUpdateRequest.Step step, JsonNode entity) {
        DeviceId id = step.getEntityId() != null ? new DeviceId(step.getEntityId()) : null;
        Device device;
        boolean created = false;
        if (step.getAction() == DeviceUpdateRequest.Action.CREATE) {
            Device template = JacksonUtil.treeToValue(entity, Device.class);
            template.setTenantId(tenantId);
            template.setId(null);
            template.setVersion(null);
            device = deviceService.saveDevice(template);
            created = true;
        } else {
            Device current = id != null ? deviceService.findDeviceById(tenantId, id) : null;
            device = existing(tenantId, current, step);
            if (step.getAction() == DeviceUpdateRequest.Action.OVERWRITE) {
                // Another device the user chose to overwrite; the package's own device never changes
                Device template = JacksonUtil.treeToValue(entity, Device.class);
                template.setTenantId(tenantId);
                prepareOverwrite(template, id, current, step);
                device = deviceService.saveDevice(template);
                created = true;
            }
        }
        DeviceCredentials credentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, device.getId());
        if (created && step.getCredentials() != null && step.getCredentials().isObject()) {
            credentials = applyCredentials(tenantId, credentials, step.getCredentials());
        }
        ObjectNode output = output(device.getId(), device.getName());
        if (credentials != null) {
            output.put("token", credentials.getCredentialsId());
        }
        if ("GATEWAY".equals(step.getType())) {
            output.put("dockerComposeUrl", "/api/device-connectivity/gateway-launch/" + device.getId().getId() + "/docker-compose/download");
        }
        SavedEntity saved = created ? new SavedEntity(device.getId(), device, step.getAction() == DeviceUpdateRequest.Action.CREATE) : null;
        return new StepResult(device.getId(), output, null, saved);
    }

    private DeviceCredentials applyCredentials(TenantId tenantId, DeviceCredentials credentials, JsonNode template) {
        JsonNode type = template.get("credentialsType");
        if (type != null && type.isTextual()) {
            credentials.setCredentialsType(DeviceCredentialsType.valueOf(type.asText()));
        }
        JsonNode value = template.get("credentialsValue");
        if (value != null && !value.isNull()) {
            credentials.setCredentialsValue(value.isTextual() ? value.asText() : value.toString());
        }
        JsonNode credentialsId = template.get("credentialsId");
        if (credentialsId != null && credentialsId.isTextual()) {
            credentials.setCredentialsId(credentialsId.asText());
        }
        return deviceCredentialsService.updateDeviceCredentials(tenantId, credentials);
    }

    private StepResult saveDashboard(TenantId tenantId, DeviceUpdateRequest.Step step, JsonNode entity) {
        DashboardId id = step.getEntityId() != null ? new DashboardId(step.getEntityId()) : null;
        Dashboard current = id != null ? dashboardService.findDashboardById(tenantId, id) : null;
        if (step.getAction() == DeviceUpdateRequest.Action.KEEP) {
            existing(tenantId, current, step);
            return kept(id, current.getTitle());
        }
        Dashboard dashboard = JacksonUtil.treeToValue(entity, Dashboard.class);
        dashboard.setTenantId(tenantId);
        if (step.getAction() == DeviceUpdateRequest.Action.OVERWRITE) {
            existing(tenantId, current, step);
            prepareOverwrite(dashboard, id, current, step);
            // The dashboard keeps its owner and the customers it is assigned to
            if (dashboard.getCustomerId() == null) {
                dashboard.setCustomerId(current.getCustomerId());
            }
            dashboard.setAssignedCustomers(current.getAssignedCustomers());
        } else {
            dashboard.setId(null);
            dashboard.setVersion(null);
        }
        Dashboard result = dashboardService.saveDashboard(dashboard);
        return savedResult(tenantId, result.getId(), result, step, output(result.getId(), result.getTitle()));
    }

    private StepResult saveRuleChain(TenantId tenantId, DeviceUpdateRequest.Step step, JsonNode entity) throws ThingsboardException {
        RuleChainId id = step.getEntityId() != null ? new RuleChainId(step.getEntityId()) : null;
        RuleChain current = id != null ? ruleChainService.findRuleChainById(tenantId, id) : null;
        if (step.getAction() == DeviceUpdateRequest.Action.KEEP) {
            existing(tenantId, current, step);
            return kept(id, current.getName());
        }
        JsonNode ruleChainJson = entity.has("ruleChain") ? entity.get("ruleChain") : entity;
        RuleChain ruleChain = JacksonUtil.treeToValue(ruleChainJson, RuleChain.class);
        ruleChain.setTenantId(tenantId);
        if (step.getAction() == DeviceUpdateRequest.Action.OVERWRITE) {
            existing(tenantId, current, step);
            prepareOverwrite(ruleChain, id, current, step);
            // A rule chain the tenant made its root one stays the root
            ruleChain.setRoot(current.isRoot());
        } else {
            ruleChain.setId(null);
            ruleChain.setVersion(null);
            ruleChain.setRoot(false);
        }
        RuleChain result = ruleChainService.saveRuleChain(ruleChain);
        if (entity.has("metadata") && entity.get("metadata").isObject()) {
            RuleChainMetaData metadata = JacksonUtil.treeToValue(entity.get("metadata"), RuleChainMetaData.class);
            metadata.setRuleChainId(result.getId());
            metadata.setVersion(result.getVersion());
            if (!ruleChainService.saveRuleChainMetaData(tenantId, metadata, tbRuleChainService::updateRuleNodeConfiguration).isSuccess()) {
                throw new IllegalStateException("Failed to save the rule chain metadata");
            }
            result = ruleChainService.findRuleChainById(tenantId, result.getId());
        }
        return savedResult(tenantId, result.getId(), result, step, output(result.getId(), result.getName()));
    }

    private StepResult saveConverter(TenantId tenantId, DeviceUpdateRequest.Step step, JsonNode entity) {
        ConverterId id = step.getEntityId() != null ? new ConverterId(step.getEntityId()) : null;
        Converter current = id != null ? converterService.findConverterById(tenantId, id) : null;
        if (step.getAction() == DeviceUpdateRequest.Action.KEEP) {
            existing(tenantId, current, step);
            return kept(id, current.getName());
        }
        Converter converter = JacksonUtil.treeToValue(entity, Converter.class);
        converter.setTenantId(tenantId);
        converter.setType("UPLINK_CONVERTER".equals(step.getType()) ? ConverterType.UPLINK : ConverterType.DOWNLINK);
        if (step.getAction() == DeviceUpdateRequest.Action.OVERWRITE) {
            existing(tenantId, current, step);
            prepareOverwrite(converter, id, current, step);
        } else {
            converter.setId(null);
            converter.setVersion(null);
        }
        Converter result = converterService.saveConverter(converter);
        return savedResult(tenantId, result.getId(), result, step, output(result.getId(), result.getName()));
    }

    private StepResult saveIntegration(TenantId tenantId, DeviceUpdateRequest.Step step, JsonNode entity) {
        IntegrationId id = step.getEntityId() != null ? new IntegrationId(step.getEntityId()) : null;
        Integration current = id != null ? integrationService.findIntegrationById(tenantId, id) : null;
        if (step.getAction() == DeviceUpdateRequest.Action.KEEP) {
            existing(tenantId, current, step);
            return new StepResult(id, integrationOutput(current), null, null);
        }
        Integration integration = JacksonUtil.treeToValue(entity, Integration.class);
        integration.setTenantId(tenantId);
        if (step.getAction() == DeviceUpdateRequest.Action.OVERWRITE) {
            existing(tenantId, current, step);
            prepareOverwrite(integration, id, current, step);
            // The devices keep sending to the same endpoint
            integration.setRoutingKey(current.getRoutingKey());
        } else {
            integration.setId(null);
            integration.setVersion(null);
            if (integration.getRoutingKey() == null) {
                integration.setRoutingKey(UUID.randomUUID().toString());
            }
        }
        Integration result = integrationService.saveIntegration(integration);
        return savedResult(tenantId, result.getId(), result, step, integrationOutput(result));
    }

    /**
     * The overwritten entity keeps its id and its current name (the user may have renamed it). The version the
     * user reviewed makes the save fail if someone changed the entity since.
     */
    private void prepareOverwrite(Object entity, EntityId id, Object current, DeviceUpdateRequest.Step step) {
        switch (entity) {
            case DeviceProfile p -> { p.setId((DeviceProfileId) id); p.setName(((DeviceProfile) current).getName()); }
            case Device d -> { d.setId((DeviceId) id); d.setName(((Device) current).getName()); }
            case Dashboard d -> { d.setId((DashboardId) id); d.setTitle(((Dashboard) current).getTitle()); }
            case RuleChain r -> { r.setId((RuleChainId) id); r.setName(((RuleChain) current).getName()); }
            case Converter c -> { c.setId((ConverterId) id); c.setName(((Converter) current).getName()); }
            case Integration i -> { i.setId((IntegrationId) id); i.setName(((Integration) current).getName()); }
            default -> throw new IllegalArgumentException("Unsupported entity: " + entity.getClass().getSimpleName());
        }
        Long currentVersion = ((HasVersion) current).getVersion();
        if (step.getExpectedVersion() != null && !step.getExpectedVersion().equals(currentVersion)) {
            throw new EntityVersionMismatchException(id.getEntityType(), null);
        }
        ((HasVersion) entity).setVersion(currentVersion);
    }

    private <T> T existing(TenantId tenantId, T current, DeviceUpdateRequest.Step step) {
        if (current == null || !tenantId.equals(((HasTenantId) current).getTenantId())) {
            throw new IllegalArgumentException(step.getName() + " was deleted. Check the update again.");
        }
        return current;
    }

    private StepResult kept(EntityId id, String name) {
        return new StepResult(id, output(id, name), null, null);
    }

    private static String previousChecksum(DeviceInstalledItemDescriptor previous, String stepName) {
        JsonNode checksum = previous.getInstallState() != null && previous.getInstallState().get(stepName) != null ?
                previous.getInstallState().get(stepName).get("checksum") : null;
        return checksum != null && checksum.isTextual() ? checksum.asText() : null;
    }

    /**
     * The checksum is of the entity the save returned: a profile or an integration read back by id inside the
     * transaction may come from the cache, which is evicted only after commit. A rule chain is read back with
     * its metadata, which isn't cached.
     */
    private StepResult savedResult(TenantId tenantId, EntityId id, Object entity, DeviceUpdateRequest.Step step, ObjectNode output) {
        String checksum = entity instanceof RuleChain ? packageEntities.checksum(tenantId, id) : EntityChecksum.of(entity);
        return new StepResult(id, output, checksum, new SavedEntity(id, entity, step.getAction() == DeviceUpdateRequest.Action.CREATE));
    }

    private static ObjectNode output(EntityId id, String name) {
        ObjectNode output = JacksonUtil.newObjectNode();
        output.put("id", id.getId().toString());
        output.put("name", name);
        return output;
    }

    /** With what the install instructions resolve: ${integration.routingKey}, ${integration.httpEndpoint}, ${integration.baseUrl}. */
    private static ObjectNode integrationOutput(Integration integration) {
        ObjectNode output = output(integration.getId(), integration.getName());
        output.put("routingKey", integration.getRoutingKey());
        JsonNode configuration = integration.getConfiguration() != null ? integration.getConfiguration() : JacksonUtil.newObjectNode();
        JsonNode client = configuration.path("clientConfiguration");
        putText(output, "httpEndpoint", client.path("httpEndpoint"), configuration.path("httpEndpoint"));
        putText(output, "baseUrl", client.path("baseUrl"), configuration.path("baseUrl"));
        return output;
    }

    private static void putText(ObjectNode output, String key, JsonNode first, JsonNode second) {
        JsonNode value = first.isTextual() ? first : second;
        if (value.isTextual()) {
            output.put(key, value.asText());
        }
    }

    /** Replaces ${<alias>.<property>} of entities this update has saved, e.g. the id of a converter it created. */
    static JsonNode resolvePlaceholders(JsonNode node, Map<String, ObjectNode> aliasOutputs) {
        if (node == null) {
            return null;
        }
        if (node.isTextual()) {
            String text = node.asText();
            Matcher matcher = ENTITY_PLACEHOLDER.matcher(text);
            StringBuilder resolved = new StringBuilder();
            boolean changed = false;
            while (matcher.find()) {
                ObjectNode output = aliasOutputs.get(matcher.group(1));
                JsonNode value = output != null ? output.get(matcher.group(2)) : null;
                String replacement = value != null && !value.isNull() ? value.asText() : matcher.group();
                changed |= value != null && !value.isNull();
                matcher.appendReplacement(resolved, Matcher.quoteReplacement(replacement));
            }
            matcher.appendTail(resolved);
            return changed ? TextNode.valueOf(resolved.toString()) : node;
        }
        if (node.isObject()) {
            ObjectNode copy = JacksonUtil.newObjectNode();
            for (Iterator<Map.Entry<String, JsonNode>> it = node.fields(); it.hasNext(); ) {
                Map.Entry<String, JsonNode> field = it.next();
                copy.set(field.getKey(), resolvePlaceholders(field.getValue(), aliasOutputs));
            }
            return copy;
        }
        if (node.isArray()) {
            ArrayNode copy = JacksonUtil.newArrayNode();
            node.forEach(item -> copy.add(resolvePlaceholders(item, aliasOutputs)));
            return copy;
        }
        return node;
    }

    /**
     * The ids the package tracks: the entities of this version's steps, and those of the installed version this one
     * no longer uses, so deleting the package still deletes them. Entities deleted since are dropped.
     */
    private List<EntityId> createdEntityIds(TenantId tenantId, DeviceInstalledItemDescriptor previous, List<EntityId> stepEntityIds) {
        List<EntityId> ids = new ArrayList<>(stepEntityIds);
        Set<UUID> known = new HashSet<>();
        ids.forEach(id -> known.add(id.getId()));
        if (previous.getCreatedEntityIds() != null) {
            for (EntityId id : previous.getCreatedEntityIds()) {
                if (known.add(id.getId()) && entityService.fetchEntity(tenantId, id).isPresent()) {
                    ids.add(id);
                }
            }
        }
        return ids;
    }

    private DashboardId existingDashboard(TenantId tenantId, DeviceInstalledItemDescriptor previous) {
        DashboardId id = previous.getDashboardId();
        return id != null && dashboardService.findDashboardById(tenantId, id) != null ? id : null;
    }

    /** What isn't transactional: the audit log, version control and attributes. A failure here doesn't undo the update. */
    private void afterCommit(SecurityUser user, TenantId tenantId, Applied applied) {
        for (SavedEntity saved : applied.saved()) {
            try {
                logEntityActionService.logEntityAction(tenantId, saved.id(), (HasName) saved.entity(),
                        saved.created() ? ActionType.ADDED : ActionType.UPDATED, user);
                if (saved.id().getEntityType() != EntityType.DEVICE) {
                    autoCommit(user, saved.id());
                }
            } catch (Exception e) {
                log.warn("[{}] Failed to log the device package update of {}", tenantId, saved.id(), e);
            }
        }
        for (DeviceUpdateRequest.Step step : applied.afterCommitSteps()) {
            try {
                if ("GATEWAY_CONNECTOR".equals(step.getType())) {
                    saveConnector(tenantId, applied.aliasOutputs().get("gateway"), step.getEntity());
                } else {
                    DeviceId deviceId = new DeviceId(step.getEntityId());
                    saveAttributes(tenantId, deviceId, AttributeScope.SERVER_SCOPE, step.getServerAttributes());
                    saveAttributes(tenantId, deviceId, AttributeScope.SHARED_SCOPE, step.getSharedAttributes());
                }
            } catch (Exception e) {
                log.warn("[{}] Failed to save the attributes of device package step [{}]", tenantId, step.getName(), e);
            }
        }
    }

    private void saveConnector(TenantId tenantId, ObjectNode gateway, JsonNode connector) throws Exception {
        if (gateway == null || connector == null || !connector.has("name")) {
            return;
        }
        DeviceId gatewayId = new DeviceId(UUID.fromString(gateway.get("id").asText()));
        String name = connector.get("name").asText();
        List<String> active = new ArrayList<>();
        for (AttributeKvEntry entry : attributesService.find(tenantId, gatewayId, AttributeScope.SHARED_SCOPE, List.of("active_connectors")).get()) {
            JsonNode value = JacksonUtil.toJsonNode(entry.getValueAsString());
            if (value != null && value.isArray()) {
                value.forEach(v -> active.add(v.asText()));
            }
        }
        if (!active.contains(name)) {
            active.add(name);
        }
        ObjectNode attributes = JacksonUtil.newObjectNode();
        attributes.set("active_connectors", JacksonUtil.valueToTree(active));
        attributes.set(name, connector);
        saveAttributes(tenantId, gatewayId, AttributeScope.SHARED_SCOPE, attributes);
    }

    private void saveAttributes(TenantId tenantId, EntityId entityId, AttributeScope scope, JsonNode attributes) {
        if (attributes == null || !attributes.isObject() || attributes.isEmpty()) {
            return;
        }
        List<AttributeKvEntry> entries = JsonConverter.convertToAttributes(JsonParser.parseString(attributes.toString()));
        telemetryService.saveAttributes(AttributesSaveRequest.builder()
                .tenantId(tenantId)
                .entityId(entityId)
                .scope(scope)
                .entries(entries)
                .build());
    }

    /**
     * The message the wizard shows: the ones validation and limits give, as they are; anything else, e.g. a database
     * error, as a common message, with the details in the log.
     */
    static String userMessage(Throwable e) {
        if (e instanceof EntityVersionMismatchException) {
            return "Someone changed it while you were reviewing the update. Check the update again.";
        }
        if (e instanceof DataValidationException || e instanceof EntitiesLimitExceededException || e instanceof IllegalArgumentException) {
            return e.getMessage();
        }
        if (e instanceof ThingsboardException te && te.getErrorCode() != ThingsboardErrorCode.GENERAL) {
            return te.getMessage();
        }
        return UNEXPECTED_ERROR;
    }

    private static boolean isKnownType(String type) {
        return type != null && (STEP_ALIASES.containsKey(type) || "GATEWAY_CONNECTOR".equals(type));
    }

    private static Map<UUID, EntityId> createdIds(DeviceInstalledItemDescriptor descriptor) {
        Map<UUID, EntityId> ids = new HashMap<>();
        if (descriptor.getCreatedEntityIds() != null) {
            descriptor.getCreatedEntityIds().forEach(id -> ids.put(id.getId(), id));
        }
        return ids;
    }

    private static DeviceUpdateRequest.Step copyWithEntity(DeviceUpdateRequest.Step step, JsonNode entity) {
        DeviceUpdateRequest.Step copy = JacksonUtil.treeToValue(JacksonUtil.valueToTree(step), DeviceUpdateRequest.Step.class);
        if (entity != null) {
            copy.setEntity(entity);
        }
        return copy;
    }

    private record DevicePackage(IotHubInstalledItem installedItem, DeviceInstalledItemDescriptor descriptor) {
    }

    private record StepResult(EntityId entityId, ObjectNode output, String checksum, SavedEntity saved) {
    }

    private record SavedEntity(EntityId id, Object entity, boolean created) {
    }

    private record Applied(DeviceInstalledItemDescriptor descriptor, Map<String, ObjectNode> outputs, List<SavedEntity> saved,
                           List<DeviceUpdateRequest.Step> afterCommitSteps, Map<String, ObjectNode> aliasOutputs) {
    }

    /** A step that failed: the transaction rolls back, and the wizard shows which step it was. */
    private static class StepFailure extends RuntimeException {

        private final String stepName;

        StepFailure(String stepName, Throwable cause) {
            super(cause.getMessage(), cause);
            this.stepName = stepName;
        }

    }

}
