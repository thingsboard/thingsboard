// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.sync.solution;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.HasCustomerId;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.subscription.SubscriptionException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.common.data.sync.ie.EntityExportSettings;
import org.thingsboard.server.common.data.sync.ie.EntityGroupExportData;
import org.thingsboard.server.common.data.sync.ie.EntityImportSettings;
import org.thingsboard.server.common.data.sync.ie.RuleChainExportData;
import org.thingsboard.server.common.data.sync.solution.SolutionData;
import org.thingsboard.server.common.data.sync.solution.SolutionExportRequest;
import org.thingsboard.server.common.data.sync.solution.SolutionExportResponse;
import org.thingsboard.server.common.data.sync.solution.SolutionImportResult;
import org.thingsboard.server.common.data.sync.solution.SolutionValidationResult;
import org.thingsboard.server.common.data.util.ThrowingRunnable;
import org.thingsboard.server.dao.service.ConstraintValidator;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.sync.ie.DefaultEntitiesExportImportService;
import org.thingsboard.server.service.sync.ie.EntitiesExportImportService;
import org.thingsboard.server.service.sync.ie.exporting.ExportableEntitiesService;
import org.thingsboard.server.service.sync.vc.data.EntitiesImportCtx;
import org.thingsboard.server.service.sync.vc.data.SimpleEntitiesExportCtx;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@TbCoreComponent
@RequiredArgsConstructor
@Slf4j
public class DefaultSolutionExportImportService implements SolutionExportImportService {

    private static final List<EntityType> SUPPORTED_ENTITY_TYPES = DefaultEntitiesExportImportService.SUPPORTED_ENTITY_TYPES;
    private static final EntityExportSettings DEFAULT_EXPORT_SETTINGS = EntityExportSettings.builder()
            .exportRelations(true)
            .exportAttributes(true)
            .exportCredentials(false)
            .exportCalculatedFields(true)
            .exportPermissions(true)
            .exportGroupEntities(true)
            .embedGroupMembers(true)
            .build();
    public static final EntityImportSettings DEFAULT_IMPORT_SETTINGS = EntityImportSettings.builder()
            .findExistingByName(true)
            .updateRelations(true)
            .saveAttributes(true)
            .saveCredentials(true)
            .saveCalculatedFields(true)
            .saveUserGroupPermissions(true)
            .autoGenerateIntegrationKey(true)
            .build();

    private final EntitiesExportImportService exportImportService;
    private final ExportableEntitiesService exportableEntitiesService;
    private final TransactionTemplate transactionTemplate;
    private final UserService userService;

    @Override
    public SolutionExportResponse exportSolution(SecurityUser user, SolutionExportRequest request) throws ThingsboardException {
        log.debug("[{}] Exporting solution", user.getTenantId());

        Set<EntityId> internalIds = request.getInternalIds();
        Set<EntityId> externalIds = request.getExternalIds();

        Map<EntityId, ExportableEntity<?>> entities = new HashMap<>();
        Set<EntityId> missingInternal = new HashSet<>();
        Set<EntityId> missingExternal = new HashSet<>();

        if (internalIds != null) {
            for (EntityId entityId : internalIds) {
                ExportableEntity<?> entity = (ExportableEntity<?>) exportableEntitiesService.findEntityByTenantIdAndId(user.getTenantId(), entityId);
                if (entity == null) {
                    missingInternal.add(entityId);
                    continue;
                }
                if (!missingInternal.isEmpty()) {
                    continue;
                }
                entities.put(entityId, entity);
            }
        }

        if (externalIds != null) {
            for (EntityId externalId : externalIds) {
                ExportableEntity<?> entity = exportableEntitiesService.findEntityByTenantIdAndExternalId(user.getTenantId(), externalId);
                if (entity == null) {
                    missingExternal.add(externalId);
                    continue;
                }
                if (!missingInternal.isEmpty() || !missingExternal.isEmpty() || entities.containsKey(entity.getId())) {
                    continue;
                }
                entities.put(entity.getId(), entity);
            }
        }

        if (!missingInternal.isEmpty() || !missingExternal.isEmpty()) {
            List<String> parts = new ArrayList<>();
            if (!missingInternal.isEmpty()) {
                parts.add("Internal ids not found: " + formatIds(missingInternal));
            }
            if (!missingExternal.isEmpty()) {
                parts.add("External ids not found: " + formatIds(missingExternal));
            }
            throw new ThingsboardException(String.join("; ", parts), ThingsboardErrorCode.ITEM_NOT_FOUND);
        }

        EntityExportSettings exportSettings = request.getSettings() != null ? request.getSettings() : DEFAULT_EXPORT_SETTINGS;
        SimpleEntitiesExportCtx ctx = new SimpleEntitiesExportCtx(user, null, null, exportSettings);

        Map<EntityType, List<EntityExportData<?>>> exportedEntities = new EnumMap<>(EntityType.class);
        for (ExportableEntity<?> entity : entities.values()) {
            EntityExportData<?> exportData = exportImportService.exportEntity(ctx, entity);
            exportedEntities.computeIfAbsent(exportData.getEntityType(), __ -> new ArrayList<>()).add(exportData);
        }

        List<String> warnings = checkDependencies(flatten(exportedEntities), "export selection");

        SolutionData solutionData = new SolutionData();
        solutionData.setEntities(exportedEntities);

        SolutionExportResponse response = new SolutionExportResponse();
        response.setSolution(solutionData);
        response.setWarnings(warnings);

        log.debug("[{}] Solution export completed: {} entity types, {} warnings",
                user.getTenantId(), exportedEntities.size(), warnings.size());
        return response;
    }

    @Override
    public SolutionImportResult importSolution(SecurityUser user, SolutionData solutionData) throws ThingsboardException {
        log.debug("[{}] Importing solution", user.getTenantId());

        validateStructure(solutionData);

        TenantId tenantId = user.getTenantId();
        List<EntityExportData<?>> allEntities = flatten(solutionData.getEntities());
        List<String> preImportConflicts = collectImportConflicts(tenantId, allEntities);
        if (!preImportConflicts.isEmpty()) {
            throw new ThingsboardException(String.join("; ", preImportConflicts), ThingsboardErrorCode.VERSION_CONFLICT);
        }

        EntitiesImportCtx ctx;
        try {
            ctx = transactionTemplate.execute(__ -> {
                try {
                    EntitiesImportCtx importCtx = new EntitiesImportCtx(UUID.randomUUID(), user, null, DEFAULT_IMPORT_SETTINGS);
                    importCtx.setRollbackOnError(true);

                    exportImportService.importEntities(importCtx, solutionData.getEntities());

                    return importCtx;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (RuntimeException e) {
            SubscriptionException subscriptionException = ExceptionUtils.throwableOfType(e, SubscriptionException.class);
            if (subscriptionException != null) {
                // A licence refusal is an expected 403, not a fault: unwrapped so it stays a refusal rather
                // than degrading into a generic server error.
                log.debug("Solution import refused by the license", e);
                throw subscriptionException;
            }
            log.error("Solution import failed", e);
            ThingsboardException te = ExceptionUtils.throwableOfType(e, ThingsboardException.class);
            if (te != null) {
                throw te;
            }
            throw new ThingsboardException("Solution import failed: " + ExceptionUtils.getRootCauseMessage(e),
                    ThingsboardErrorCode.GENERAL);
        }

        if (ctx != null) {
            for (ThrowingRunnable callback : ctx.getEventCallbacks()) {
                try {
                    callback.run();
                } catch (Exception e) {
                    log.warn("[{}] Post-import event callback failed", tenantId, e);
                }
            }
        }

        Map<EntityType, Integer> createdCounts = new HashMap<>();
        if (ctx != null) {
            ctx.getResults().forEach((entityType, loadResult) -> {
                int created = loadResult.getCreated();
                if (created > 0) {
                    createdCounts.put(entityType, created);
                }
                // Entity groups are bucketed under ENTITY_GROUP regardless of their inner type
                // (DEVICE group, USER group, ...) — registerResult stores their counts in
                // groupsCreated against the inner type, not against `created`.
                int groupsCreated = loadResult.getGroupsCreated();
                if (groupsCreated > 0) {
                    createdCounts.merge(EntityType.ENTITY_GROUP, groupsCreated, Integer::sum);
                }
            });
        }

        log.debug("[{}] Solution import completed: {}", tenantId, createdCounts);
        SolutionImportResult result = new SolutionImportResult();
        result.setSuccess(true);
        result.setCreated(createdCounts);
        Map<UUID, UUID> idMapping = new HashMap<>();
        if (ctx != null) {
            ctx.getExternalToInternalIdMap().forEach((externalId, internalId) -> {
                idMapping.put(externalId.getId(), internalId.getId());
            });
        }
        result.setIdMapping(idMapping);
        return result;
    }

    @Override
    public SolutionValidationResult validateSolution(SecurityUser user, SolutionData solutionData) {
        log.debug("[{}] Validating solution", user.getTenantId());

        SolutionValidationResult result = new SolutionValidationResult();

        try {
            validateStructure(solutionData);
        } catch (ThingsboardException e) {
            result.setValid(false);
            result.setConflicts(List.of(e.getMessage()));
            result.setWarnings(List.of());
            return result;
        }

        Map<EntityType, Integer> entitySummary = new LinkedHashMap<>();
        solutionData.getEntities().forEach((type, entities) -> entitySummary.put(type, entities.size()));
        result.setEntitySummary(entitySummary);

        List<EntityExportData<?>> allEntities = flatten(solutionData.getEntities());

        List<String> conflicts = validateEntitiesConstraints(allEntities);
        conflicts.addAll(collectImportConflicts(user.getTenantId(), allEntities));
        if (!conflicts.isEmpty()) {
            result.setValid(false);
            result.setConflicts(conflicts);
            result.setWarnings(List.of());
            return result;
        }

        List<String> warnings = checkDependencies(allEntities, "solution file");

        result.setValid(true);
        result.setConflicts(List.of());
        result.setWarnings(warnings);

        log.debug("[{}] Solution validation completed: valid={}, warnings={}",
                user.getTenantId(), result.isValid(), warnings.size());
        return result;
    }

    private List<String> validateEntitiesConstraints(List<EntityExportData<?>> allEntities) {
        List<String> conflicts = new ArrayList<>();
        for (EntityExportData<?> exportData : allEntities) {
            ExportableEntity<?> entity = exportData.getEntity();
            String entityName = entity.getName() != null ? entity.getName() : String.valueOf(entity.getId());
            String entityLabel = exportData.getEntityType() + " '" + entityName + "'";
            try {
                ConstraintValidator.validateFields(entity);
            } catch (DataValidationException e) {
                conflicts.add(entityLabel + ": " + e.getMessage());
            }
            if (exportData.hasRelations()) {
                List<EntityRelation> relations = exportData.getRelations();
                for (int i = 0; i < relations.size(); i++) {
                    EntityRelation relation = relations.get(i);
                    String relationLabel = relation.getType() != null ? relation.getType() : "#" + i;
                    try {
                        ConstraintValidator.validateFields(relation);
                    } catch (DataValidationException e) {
                        conflicts.add(entityLabel + ", relation '" + relationLabel + "': " + e.getMessage());
                    }
                }
            }
            if (exportData.hasCalculatedFields()) {
                List<CalculatedField> calculatedFields = exportData.getCalculatedFields();
                for (int i = 0; i < calculatedFields.size(); i++) {
                    CalculatedField cf = calculatedFields.get(i);
                    String cfLabel = cf.getName() != null ? cf.getName() : "#" + i;
                    try {
                        ConstraintValidator.validateFields(cf);
                    } catch (DataValidationException e) {
                        conflicts.add(entityLabel + ", calculated field '" + cfLabel + "': " + e.getMessage());
                    }
                }
            }
        }
        return conflicts;
    }

    private List<String> checkDependencies(List<EntityExportData<?>> exportedEntities, String context) {
        Set<EntityId> exportedIds = new HashSet<>();
        for (EntityExportData<?> data : exportedEntities) {
            exportedIds.add(data.getExternalId());
        }

        List<String> warnings = new ArrayList<>();

        for (EntityExportData<?> data : exportedEntities) {
            ExportableEntity<?> entity = data.getEntity();
            String entityName = entity.getName();
            EntityType entityType = data.getEntityType();

            switch (entityType) {
                case DEVICE_PROFILE -> {
                    DeviceProfile dp = (DeviceProfile) entity;
                    checkReference(warnings, entityType, entityName, "rule chain", dp.getDefaultRuleChainId(), exportedIds, context);
                    checkReference(warnings, entityType, entityName, "dashboard", dp.getDefaultDashboardId(), exportedIds, context);
                    checkReference(warnings, entityType, entityName, "edge rule chain", dp.getDefaultEdgeRuleChainId(), exportedIds, context);
                    checkReference(warnings, entityType, entityName, "firmware", dp.getFirmwareId(), exportedIds, context);
                    checkReference(warnings, entityType, entityName, "software", dp.getSoftwareId(), exportedIds, context);
                }
                case ASSET_PROFILE -> {
                    AssetProfile ap = (AssetProfile) entity;
                    checkReference(warnings, entityType, entityName, "rule chain", ap.getDefaultRuleChainId(), exportedIds, context);
                    checkReference(warnings, entityType, entityName, "dashboard", ap.getDefaultDashboardId(), exportedIds, context);
                    checkReference(warnings, entityType, entityName, "edge rule chain", ap.getDefaultEdgeRuleChainId(), exportedIds, context);
                }
                case DEVICE -> {
                    Device device = (Device) entity;
                    checkReference(warnings, entityType, entityName, "device profile", device.getDeviceProfileId(), exportedIds, context);
                    checkReference(warnings, entityType, entityName, "firmware", device.getFirmwareId(), exportedIds, context);
                    checkReference(warnings, entityType, entityName, "software", device.getSoftwareId(), exportedIds, context);
                    checkReference(warnings, entityType, entityName, "customer", device.getCustomerId(), exportedIds, context);
                }
                case ASSET -> {
                    Asset asset = (Asset) entity;
                    checkReference(warnings, entityType, entityName, "asset profile", asset.getAssetProfileId(), exportedIds, context);
                    checkReference(warnings, entityType, entityName, "customer", asset.getCustomerId(), exportedIds, context);
                }
                case RULE_CHAIN -> {
                    if (data instanceof RuleChainExportData rcData && rcData.getMetaData() != null
                        && rcData.getMetaData().getRuleChainConnections() != null) {
                        for (var conn : rcData.getMetaData().getRuleChainConnections()) {
                            checkReference(warnings, entityType, entityName, "rule chain", conn.getTargetRuleChainId(), exportedIds, context);
                        }
                    }
                }
                case DASHBOARD -> {
                    if (entity instanceof HasCustomerId hasCustomer) {
                        checkReference(warnings, entityType, entityName, "customer", hasCustomer.getCustomerId(), exportedIds, context);
                    }
                }
                case USER -> {
                    User u = (User) entity;
                    if (u.getCustomerId() != null && !u.getCustomerId().isNullUid()) {
                        checkReference(warnings, entityType, entityName, "customer", u.getCustomerId(), exportedIds, context);
                    }
                    checkUserDashboardRefWarning(warnings, entityType, entityName, u.getAdditionalInfo(), "defaultDashboardId", exportedIds, context);
                    checkUserDashboardRefWarning(warnings, entityType, entityName, u.getAdditionalInfo(), "homeDashboardId", exportedIds, context);
                }
                default -> { /* No specific reference checks for other types */ }
            }
        }
        return warnings;
    }

    private void checkReference(List<String> warnings, EntityType sourceType, String sourceName,
                                String refTypeName, EntityId refId, Set<EntityId> exportedIds, String context) {
        if (refId != null && !refId.isNullUid() && !exportedIds.contains(refId)) {
            warnings.add(sourceType + " '" + sourceName + "' references " + refTypeName
                         + " (" + refId.getId() + ") which is not included in the " + context);
        }
    }

    private void checkUserDashboardRefWarning(List<String> warnings, EntityType sourceType, String sourceName,
                                              JsonNode additionalInfo, String key, Set<EntityId> exportedIds, String context) {
        if (additionalInfo == null || !additionalInfo.isObject()) {
            return;
        }
        JsonNode node = additionalInfo.get(key);
        if (node == null || !node.isTextual()) {
            return;
        }
        UUID uuid;
        try {
            uuid = UUID.fromString(node.asText());
        } catch (IllegalArgumentException e) {
            return;
        }
        EntityId refId = EntityIdFactory.getByTypeAndUuid(EntityType.DASHBOARD, uuid);
        if (!exportedIds.contains(refId)) {
            warnings.add(sourceType + " '" + sourceName + "' references dashboard ("
                         + uuid + ") via " + key + " which is not included in the " + context);
        }
    }

    private List<EntityExportData<?>> flatten(Map<EntityType, List<EntityExportData<?>>> entities) {
        return entities.values().stream()
                .flatMap(List::stream)
                .toList();
    }

    private List<String> collectImportConflicts(TenantId tenantId, List<EntityExportData<?>> allEntities) {
        List<String> conflicts = new ArrayList<>();
        conflicts.addAll(validateUsers(tenantId, allEntities));
        conflicts.addAll(validateGroupMembers(tenantId, allEntities));
        return conflicts;
    }

    private List<String> validateUsers(TenantId tenantId, List<EntityExportData<?>> allEntities) {
        List<String> conflicts = new ArrayList<>();
        Set<String> seenEmails = new HashSet<>();
        for (EntityExportData<?> exportData : allEntities) {
            if (exportData.getEntityType() != EntityType.USER) {
                continue;
            }
            findUserConflict(tenantId, exportData, seenEmails).ifPresent(conflicts::add);
        }
        return conflicts;
    }

    private Optional<String> findUserConflict(TenantId tenantId, EntityExportData<?> exportData, Set<String> seenEmails) {
        User user = (User) exportData.getEntity();
        if (Authority.SYS_ADMIN.equals(user.getAuthority())) {
            return Optional.of("USER: SYS_ADMIN users cannot be imported");
        }
        String email = user.getEmail();
        if (email == null) {
            return Optional.empty();
        }
        // Mirror UserServiceImpl's default (userLoginCaseSensitive=false) lookup, where emails are
        // compared case-insensitively. Without this, 'Foo@x' and 'foo@x' would slip past dedup and
        // collide on the DB unique constraint at save time.
        if (!seenEmails.add(email.toLowerCase())) {
            return Optional.of("USER: duplicate email '" + email + "'");
        }
        User existing = userService.findUserByEmail(tenantId, email);
        if (existing == null) {
            return Optional.empty();
        }
        if (!tenantId.equals(existing.getTenantId())) {
            return Optional.of("USER: email '" + email + "' already exists in another tenant");
        }
        UserId externalId = (UserId) exportData.getExternalId();
        boolean linkedByExternalId = existing.getExternalId() != null && existing.getExternalId().equals(externalId);
        // Same-tenant idempotency: user.getId() here is actually the externalId, because
        // DefaultEntityExportService.getExportData sets entity.setId(externalId) before export.
        // For the original tenant on first export, externalId == source internal id, so this branch
        // matches the existing local user.
        boolean linkedById = existing.getId().equals(user.getId());
        if (!linkedByExternalId && !linkedById) {
            return Optional.of("USER: email '" + email + "' already exists in this tenant");
        }
        if (!Objects.equals(user.getAuthority(), existing.getAuthority())) {
            return Optional.of("USER: authority change is not allowed (existing: " + existing.getAuthority()
                               + ", incoming: " + user.getAuthority() + ")");
        }
        return Optional.empty();
    }

    private List<String> validateGroupMembers(TenantId tenantId, List<EntityExportData<?>> allEntities) {
        Set<EntityId> inSolution = allEntities.stream()
                .map(EntityExportData::getExternalId)
                .collect(Collectors.toSet());
        List<String> conflicts = new ArrayList<>();
        for (EntityExportData<?> exportData : allEntities) {
            if (!(exportData instanceof EntityGroupExportData groupData) || groupData.getMemberIds() == null) {
                continue;
            }
            EntityGroup group = groupData.getEntity();
            String groupLabel = group.getType() + " entity group '" + group.getName() + "'";
            for (UUID memberUuid : groupData.getMemberIds()) {
                EntityId memberId = EntityIdFactory.getByTypeAndUuid(group.getType(), memberUuid);
                if (isMemberAvailable(tenantId, inSolution, memberId)) {
                    continue;
                }
                conflicts.add(groupLabel + " references member entity that is not included in the solution and does not exist in this tenant: " + memberUuid);
            }
        }
        return conflicts;
    }

    private boolean isMemberAvailable(TenantId tenantId, Set<EntityId> inSolution, EntityId memberId) {
        return inSolution.contains(memberId)
               || exportableEntitiesService.findEntityByTenantIdAndExternalId(tenantId, memberId) != null
               || exportableEntitiesService.findEntityByTenantIdAndId(tenantId, memberId) != null;
    }

    private void validateStructure(SolutionData solutionData) throws ThingsboardException {
        if (solutionData.getEntities() == null || solutionData.getEntities().isEmpty()) {
            throw new ThingsboardException("Solution contains no entities", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }

        List<String> unsupportedTypes = new ArrayList<>();
        for (Map.Entry<EntityType, List<EntityExportData<?>>> entry : solutionData.getEntities().entrySet()) {
            EntityType entityType = entry.getKey();
            if (!SUPPORTED_ENTITY_TYPES.contains(entityType)) {
                unsupportedTypes.add(entityType.name());
            }
            List<EntityExportData<?>> entities = entry.getValue();
            if (entities == null) {
                throw new ThingsboardException("Null entity list for type " + entityType, ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            }
            for (EntityExportData<?> entityData : entities) {
                if (entityData == null || entityData.getEntity() == null || entityData.getEntity().getName() == null
                    || entityData.getEntity().getId() == null) {
                    throw new ThingsboardException("Invalid entity data for type " + entityType, ThingsboardErrorCode.BAD_REQUEST_PARAMS);
                }
                if (entityData.getEntityType() == null || !entityType.equals(entityData.getEntityType())) {
                    throw new ThingsboardException("Mismatched entity type for entry " + entityType, ThingsboardErrorCode.BAD_REQUEST_PARAMS);
                }
            }
        }
        if (!unsupportedTypes.isEmpty()) {
            throw new ThingsboardException("Unsupported entity types: " + String.join(", ", unsupportedTypes),
                    ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
    }

    private static String formatIds(Set<EntityId> missing) {
        return missing.stream()
                .collect(Collectors.groupingBy(EntityId::getEntityType,
                        Collectors.mapping(Object::toString, Collectors.joining(", ", "[", "]"))))
                .entrySet().stream()
                .map(e -> e.getKey().name() + " " + e.getValue())
                .collect(Collectors.joining(", "));
    }

}
