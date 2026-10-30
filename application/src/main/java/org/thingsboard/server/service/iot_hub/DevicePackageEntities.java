// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.iot_hub;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.HasVersion;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.iot_hub.DeviceInstalledItemDescriptor;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.dao.converter.ConverterService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.integration.IntegrationService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The entities of a device package whose content an update overwrites, and their checksums. A device, a gateway
 * and a gateway connector don't have one: an update never changes them.
 */
@Component
@TbCoreComponent
@RequiredArgsConstructor
public class DevicePackageEntities {

    static final Set<EntityType> CHECKSUM_TYPES = Set.of(EntityType.DEVICE_PROFILE, EntityType.DASHBOARD,
            EntityType.RULE_CHAIN, EntityType.CONVERTER, EntityType.INTEGRATION);

    private final DeviceProfileService deviceProfileService;
    private final DashboardService dashboardService;
    private final RuleChainService ruleChainService;
    private final ConverterService converterService;
    private final IntegrationService integrationService;

    /** The entity of the tenant, null when there is none, e.g. it was deleted. A rule chain comes with its metadata. */
    public Snapshot load(TenantId tenantId, EntityId entityId) {
        Object entity = switch (entityId.getEntityType()) {
            case DEVICE_PROFILE -> deviceProfileService.findDeviceProfileById(tenantId, (DeviceProfileId) entityId);
            case DASHBOARD -> dashboardService.findDashboardById(tenantId, (DashboardId) entityId);
            case RULE_CHAIN -> ruleChainService.findRuleChainById(tenantId, (RuleChainId) entityId);
            case CONVERTER -> converterService.findConverterById(tenantId, (ConverterId) entityId);
            case INTEGRATION -> integrationService.findIntegrationById(tenantId, (IntegrationId) entityId);
            default -> throw new IllegalArgumentException("Unsupported entity type: " + entityId.getEntityType());
        };
        if (entity == null || !tenantId.equals(((HasTenantId) entity).getTenantId())) {
            return null;
        }
        Object metadata = entity instanceof RuleChain ruleChain ? ruleChainService.loadRuleChainMetaData(tenantId, ruleChain.getId()) : null;
        return new Snapshot(entity, metadata);
    }

    public String checksum(TenantId tenantId, EntityId entityId) {
        Snapshot snapshot = load(tenantId, entityId);
        return snapshot != null ? snapshot.checksum() : null;
    }

    /**
     * Adds the checksum of each entity the install state names, so that an update can tell whether someone changed
     * it since. Entities the user kept at install are left out: an update never overwrites them.
     */
    public void addChecksums(TenantId tenantId, DeviceInstalledItemDescriptor descriptor) {
        if (descriptor.getInstallState() == null || descriptor.getCreatedEntityIds() == null) {
            return;
        }
        Map<UUID, EntityId> createdIds = new HashMap<>();
        descriptor.getCreatedEntityIds().forEach(id -> createdIds.put(id.getId(), id));
        descriptor.getInstallState().forEach((stepName, state) -> {
            EntityId entityId = entityIdOf(state, createdIds);
            if (entityId != null && CHECKSUM_TYPES.contains(entityId.getEntityType()) && state.isObject()) {
                String checksum = checksum(tenantId, entityId);
                if (checksum != null) {
                    ((ObjectNode) state).put("checksum", checksum);
                }
            }
        });
    }

    /** The package entity an install-state step recorded, with its type from the ids the package created. */
    static EntityId entityIdOf(JsonNode state, Map<UUID, EntityId> createdIds) {
        JsonNode id = state != null ? state.path("entityOutput").path("id") : null;
        if (id == null || !id.isTextual() || "use-existing".equals(state.path("resolution").asText())) {
            return null;
        }
        try {
            return createdIds.get(UUID.fromString(id.asText()));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public record Snapshot(Object entity, Object metadata) {

        public Long version() {
            return ((HasVersion) entity).getVersion();
        }

        public String checksum() {
            return metadata != null ? EntityChecksum.of(entity, metadata) : EntityChecksum.of(entity);
        }

    }

}
