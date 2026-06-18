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
package org.thingsboard.server.service.solutions.data;

import lombok.Data;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.service.solutions.data.definition.AssetDefinition;
import org.thingsboard.server.service.solutions.data.definition.AssetProfileDefinition;
import org.thingsboard.server.service.solutions.data.definition.CustomerDefinition;
import org.thingsboard.server.service.solutions.data.definition.DashboardDefinition;
import org.thingsboard.server.service.solutions.data.definition.DeviceDefinition;
import org.thingsboard.server.service.solutions.data.definition.EdgeDefinition;
import org.thingsboard.server.service.solutions.data.definition.DeviceProfileDefinition;
import org.thingsboard.server.service.solutions.data.definition.EntityDefinition;
import org.thingsboard.server.service.solutions.data.definition.EntitySearchKey;
import org.thingsboard.server.service.solutions.data.definition.EmulatorDefinition;
import org.thingsboard.server.service.solutions.data.definition.RelationDefinition;
import org.thingsboard.server.service.solutions.data.solution.TenantSolutionTemplateInstructions;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
public class SolutionInstallContext {

    private final TenantId tenantId;
    private final Path tempDir;
    private final User user;
    private final TenantSolutionTemplateInstructions solutionInstructions;
    private final List<EntityId> createdEntitiesList = new ArrayList<>();
    private final Map<String, String> realIds = new HashMap<>();
    private final Map<EntitySearchKey, EntityId> entityIdMap = new HashMap<>();
    private final Map<EntityId, List<RelationDefinition>> relationDefinitions = new LinkedHashMap<>();
    private final List<DashboardLinkInfo> dashboardLinks = new ArrayList<>();

    // For instructions
    private final Map<String, DeviceCredentialsInfo> createdDevices = new LinkedHashMap<>();
    private final Map<String, UserCredentialsInfo> createdUsers = new LinkedHashMap<>();
    private final Map<String, EdgeLinkInfo> createdEdges = new LinkedHashMap<>();
    private final Map<UUID, CreatedEntityInfo> createdEntities = new LinkedHashMap<>();
    private final Map<UUID, CreatedAlarmRuleInfo> createdAlarmRules = new LinkedHashMap<>();
    private final Map<UUID, CreatedCalculatedFieldInfo> createdCalculatedFields = new LinkedHashMap<>();
    private final String solutionId;
    private final long installTs;
    private long oldestTelemetryTs;
    private Map<String, EmulatorDefinition> deviceEmulators;
    private Map<String, EmulatorDefinition> assetEmulators;

    public SolutionInstallContext(TenantId tenantId, String solutionId, Path tempDir, User user, TenantSolutionTemplateInstructions solutionInstructions) {
        this.tenantId = tenantId;
        this.solutionId = solutionId;
        this.tempDir = tempDir;
        this.user = user;
        this.solutionInstructions = solutionInstructions;
        this.installTs = System.currentTimeMillis();
        put(new EntitySearchKey(tenantId, EntityType.TENANT, null), tenantId);
    }

    public void registerReferenceOnly(String referenceId, EntityId entityId) {
        if (!StringUtils.isEmpty(referenceId)) {
            realIds.put(referenceId, entityId.getId().toString());
        }
    }

    public void register(String referenceId, EntityId entityId) {
        registerReferenceOnly(referenceId, entityId);
        register(entityId);
    }

    public void register(EntityId entityId) {
        createdEntitiesList.add(entityId);
    }

    public void register(String referenceId, RuleChain ruleChain) {
        register(referenceId, ruleChain.getId());
        createdEntities.put(ruleChain.getUuidId(), new CreatedRuleChainInfo(ruleChain.getName(), ruleChain.getType(), "Tenant"));
    }

    public void register(DeviceProfileDefinition definition, DeviceProfile deviceProfile) {
        register(definition.getJsonId(), deviceProfile.getId());
        createdEntities.put(deviceProfile.getUuidId(), new CreatedEntityInfo(deviceProfile.getName(), EntityType.DEVICE_PROFILE, "Tenant"));
    }

    public void register(AssetProfileDefinition definition, AssetProfile assetProfile) {
        register(definition.getJsonId(), assetProfile.getId());
        createdEntities.put(assetProfile.getUuidId(), new CreatedEntityInfo(assetProfile.getName(), EntityType.ASSET_PROFILE, "Tenant"));
    }

    public void register(AssetDefinition definition, Asset asset) {
        register(definition.getJsonId(), asset.getId());
        createdEntities.put(asset.getUuidId(), new CreatedEntityInfo(asset.getName(), EntityType.ASSET,
                StringUtils.isEmpty(definition.getCustomer()) ? "Tenant" : definition.getCustomer()));
    }

    public void register(DeviceDefinition definition, Device device) {
        register(definition.getJsonId(), device.getId());
        createdEntities.put(device.getUuidId(), new CreatedEntityInfo(device.getName(), EntityType.DEVICE,
                StringUtils.isEmpty(definition.getCustomer()) ? "Tenant" : definition.getCustomer()));
    }

    public void register(CustomerDefinition definition, Customer customer) {
        register(definition.getJsonId(), customer.getId());
        createdEntities.put(customer.getUuidId(), new CreatedEntityInfo(customer.getName(), EntityType.CUSTOMER, "Tenant"));
    }

    public void register(CustomerDefinition customerDef, User user) {
        register((String) null, user.getId());
        createdEntities.put(user.getUuidId(), new CreatedEntityInfo(user.getName(), EntityType.USER,
                StringUtils.isEmpty(customerDef.getName()) ? "Tenant" : customerDef.getName()));
    }

    public void register(EdgeDefinition definition, Edge edge) {
        register(definition.getJsonId(), edge.getId());
        createdEntities.put(edge.getUuidId(), new CreatedEntityInfo(edge.getName(), EntityType.EDGE,
                StringUtils.isEmpty(definition.getCustomer()) ? "Tenant" : definition.getCustomer()));
    }

    public void register(DashboardDefinition definition, Dashboard dashboard) {
        register(definition.getJsonId(), dashboard.getId());
        createdEntities.put(dashboard.getUuidId(), new CreatedEntityInfo(dashboard.getTitle(), EntityType.DASHBOARD,
                StringUtils.isEmpty(definition.getCustomer()) ? "Tenant" : definition.getCustomer()));
    }

    public void register(CalculatedField calculatedField) {
        register(calculatedField.getId());
        EntityId entityId = calculatedField.getEntityId();
        CreatedEntityInfo entityInfo = createdEntities.get(entityId.getId());
        boolean alarmRule = calculatedField.getType() == CalculatedFieldType.ALARM;
        if (entityInfo == null) {
            String target = alarmRule ? "Alarm rule" : "Calculated field";
            throw new IllegalStateException("Failed to register " + target + " with name: " +
                    calculatedField.getName() + " for non-existing entity with id: " + entityId);
        }
        if (alarmRule) {
            createdAlarmRules.put(calculatedField.getUuidId(), CreatedAlarmRuleInfo.from(entityId, entityInfo.getName(), calculatedField));
            return;
        }
        createdCalculatedFields.put(calculatedField.getUuidId(), CreatedCalculatedFieldInfo.from(entityId, entityInfo.getName(), calculatedField));
    }

    public void putIdToMap(EntityDefinition entityDefinition, EntityId entityId) {
        putIdToMap(entityDefinition.getEntityType(), entityDefinition.getName(), entityId);
    }

    public void putIdToMap(EntityType entityType, String entityName, EntityId entityId) {
        putIdToMap(tenantId, entityType, entityName, entityId);
    }

    public void putIdToMap(EntityId ownerId, EntityType entityType, String entityName, EntityId entityId) {
        put(new EntitySearchKey(ownerId, entityType, entityName), entityId);
    }

    public void put(EntitySearchKey entitySearchKey, EntityId entityId) {
        entityIdMap.put(entitySearchKey, entityId);
    }

    @SuppressWarnings("unchecked")
    public <T extends EntityId> T getIdFromMap(EntityType entityType, String entityName) {
        return (T) entityIdMap.get(new EntitySearchKey(tenantId, entityType, entityName));
    }

    public void put(EntityId entityId, List<RelationDefinition> relations) {
        relationDefinitions.put(entityId, relations);
    }

    public void addDeviceCredentials(DeviceCredentialsInfo deviceCredentialsInfo) {
        createdDevices.put(deviceCredentialsInfo.getName(), deviceCredentialsInfo);
    }

    public void addUserCredentials(UserCredentialsInfo userCredentialsInfo) {
        createdUsers.put(userCredentialsInfo.getName(), userCredentialsInfo);
    }

    public void addEdgeLinkInfo(String edgeName, EdgeLinkInfo edgeLinkInfo) {
        createdEdges.put(edgeName, edgeLinkInfo);
    }

}
