/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.service.sync.ie.importing.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.api.client.util.Objects;
import com.google.common.util.concurrent.FutureCallback;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.AttributesSaveRequest;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.HasDefaultOption;
import org.thingsboard.server.common.data.HasVersion;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.configuration.ArgumentsBasedCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.geofencing.GeofencingCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.sync.ie.AttributeExportData;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.common.data.sync.ie.EntityImportResult;
import org.thingsboard.server.dao.cf.CalculatedFieldService;
import org.thingsboard.server.dao.relation.RelationDao;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.service.action.EntityActionService;
import org.thingsboard.server.service.entitiy.TbLogEntityActionService;
import org.thingsboard.server.service.sync.ie.exporting.ExportableEntitiesService;
import org.thingsboard.server.service.sync.ie.importing.EntityImportService;
import org.thingsboard.server.service.sync.vc.data.EntitiesImportCtx;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public abstract class BaseEntityImportService<I extends EntityId, E extends ExportableEntity<I>, D extends EntityExportData<E>> implements EntityImportService<I, E, D> {

    @Autowired
    @Lazy
    private ExportableEntitiesService entitiesService;
    @Autowired
    private CalculatedFieldService calculatedFieldService;
    @Autowired
    private RelationService relationService;
    @Autowired
    private RelationDao relationDao;
    @Autowired
    private TelemetrySubscriptionService tsSubService;
    @Autowired
    protected EntityActionService entityActionService;
    @Autowired
    protected TbClusterService clusterService;
    @Autowired
    protected TbLogEntityActionService logEntityActionService;

    @Override
    public EntityImportResult<E> importEntity(EntitiesImportCtx ctx, D exportData) throws ThingsboardException {
        EntityImportResult<E> importResult = new EntityImportResult<>();
        ctx.setCurrentImportResult(importResult);
        importResult.setEntityType(getEntityType());
        IdProvider idProvider = new IdProvider(ctx, importResult);

        E entity = exportData.getEntity();
        entity.setExternalId(entity.getId());

        E existingEntity = findExistingEntity(ctx, entity, idProvider);
        importResult.setOldEntity(existingEntity);

        setOwner(ctx.getTenantId(), entity, idProvider);
        if (existingEntity == null) {
            entity.setId(null);
        } else {
            entity.setId(existingEntity.getId());
            entity.setCreatedTime(existingEntity.getCreatedTime());
        }

        E prepared = prepare(ctx, entity, existingEntity, exportData, idProvider);

        CompareResult compareResult = compare(ctx, exportData, prepared, existingEntity);

        if (compareResult.isUpdateNeeded()) {
            E savedEntity = saveOrUpdate(ctx, prepared, exportData, idProvider, compareResult);
            boolean created = existingEntity == null;
            importResult.setCreated(created);
            importResult.setUpdated(!created);
            importResult.setSavedEntity(savedEntity);
            ctx.putInternalId(exportData.getExternalId(), savedEntity.getId());
        } else {
            importResult.setSavedEntity(existingEntity);
            ctx.putInternalId(exportData.getExternalId(), existingEntity.getId());
            importResult.setUpdatedRelatedEntities(updateRelatedEntitiesIfUnmodified(ctx, prepared, exportData, idProvider));
        }

        processAfterSaved(ctx, importResult, exportData, idProvider);

        return importResult;
    }

    @Data
    @AllArgsConstructor
    static class CompareResult {
        private boolean updateNeeded;
        private boolean externalIdChangedOnly;

        public CompareResult(boolean updateNeeded) {
            this.updateNeeded = updateNeeded;
        }

    }

    protected boolean updateRelatedEntitiesIfUnmodified(EntitiesImportCtx ctx, E prepared, D exportData, IdProvider idProvider) {
        return importCalculatedFields(ctx, prepared, exportData, idProvider);
    }

    @Override
    public abstract EntityType getEntityType();

    protected abstract void setOwner(TenantId tenantId, E entity, IdProvider idProvider);

    protected abstract E prepare(EntitiesImportCtx ctx, E entity, E oldEntity, D exportData, IdProvider idProvider);

    protected CompareResult compare(EntitiesImportCtx ctx, D exportData, E prepared, E existing) {
        if (existing == null) {
            log.debug("[{}] Found new entity.", prepared.getId());
            return new CompareResult(true);
        }
        var newCopy = deepCopy(prepared);
        var existingCopy = deepCopy(existing);
        cleanupForComparison(newCopy);
        cleanupForComparison(existingCopy);
        var updateNeeded = isUpdateNeeded(ctx, exportData, newCopy, existingCopy);
        boolean externalIdChangedOnly = false;
        if (updateNeeded) {
            log.debug("[{}] Found update.", prepared.getId());
            log.debug("[{}] From: {}", prepared.getId(), newCopy);
            log.debug("[{}] To: {}", prepared.getId(), existingCopy);
            cleanupExternalId(newCopy);
            cleanupExternalId(existingCopy);
            externalIdChangedOnly = newCopy.equals(existingCopy);
        }
        return new CompareResult(updateNeeded, externalIdChangedOnly);
    }

    protected boolean isUpdateNeeded(EntitiesImportCtx ctx, D exportData, E prepared, E existing) {
        return !prepared.equals(existing);
    }

    protected abstract E deepCopy(E e);

    protected void cleanupForComparison(E e) {
        e.setTenantId(null);
        e.setCreatedTime(0);
        if (e instanceof HasVersion hasVersion) {
            hasVersion.setVersion(null);
        }
    }

    protected void cleanupExternalId(E e) {
        e.setExternalId(null);
    }

    protected abstract E saveOrUpdate(EntitiesImportCtx ctx, E entity, D exportData, IdProvider idProvider, CompareResult compareResult);

    protected void processAfterSaved(EntitiesImportCtx ctx, EntityImportResult<E> importResult, D exportData, IdProvider idProvider) throws ThingsboardException {
        E savedEntity = importResult.getSavedEntity();
        E oldEntity = importResult.getOldEntity();

        if (importResult.isCreated() || importResult.isUpdated()) {
            importResult.addSendEventsCallback(() -> onEntitySaved(ctx.getUser(), savedEntity, oldEntity));
        }

        if (ctx.isUpdateRelations() && exportData.getRelations() != null) {
            importRelations(ctx, exportData.getRelations(), importResult, idProvider);
        }
        if (ctx.isSaveAttributes() && exportData.getAttributes() != null) {
            if (exportData.getAttributes().values().stream().anyMatch(d -> !d.isEmpty())) {
                importResult.setUpdatedRelatedEntities(true);
            }
            importAttributes(ctx.getUser(), exportData.getAttributes(), importResult);
        }
    }

    private void importRelations(EntitiesImportCtx ctx, List<EntityRelation> relations, EntityImportResult<E> importResult, IdProvider idProvider) {
        var tenantId = ctx.getTenantId();
        E entity = importResult.getSavedEntity();
        importResult.addSaveReferencesCallback(() -> {
            for (EntityRelation relation : relations) {
                if (!relation.getTo().equals(entity.getId())) {
                    relation.setTo(idProvider.getInternalId(relation.getTo()));
                }
                if (!relation.getFrom().equals(entity.getId())) {
                    relation.setFrom(idProvider.getInternalId(relation.getFrom()));
                }
            }

            Map<EntityRelation, EntityRelation> relationsMap = new LinkedHashMap<>();
            relations.forEach(r -> relationsMap.put(r, r));

            if (importResult.getOldEntity() != null) {
                List<EntityRelation> existingRelations = new ArrayList<>();
                existingRelations.addAll(relationDao.findAllByTo(tenantId, entity.getId(), RelationTypeGroup.COMMON));
                existingRelations.addAll(relationDao.findAllByFrom(tenantId, entity.getId(), RelationTypeGroup.COMMON));
                // dao is used here instead of service to avoid getting cached values, because relationService.deleteRelation will evict value from cache only after transaction is committed

                for (EntityRelation existingRelation : existingRelations) {
                    EntityRelation relation = relationsMap.get(existingRelation);
                    if (relation == null) {
                        importResult.setUpdatedRelatedEntities(true);
                        relationService.deleteRelation(ctx.getTenantId(), existingRelation.getFrom(), existingRelation.getTo(), existingRelation.getType(), existingRelation.getTypeGroup());
                        importResult.addSendEventsCallback(() -> {
                            logEntityActionService.logEntityRelationAction(tenantId, null,
                                    existingRelation, ctx.getUser(), ActionType.RELATION_DELETED, null, existingRelation);
                        });
                    } else if (Objects.equal(relation.getAdditionalInfo(), existingRelation.getAdditionalInfo())) {
                        relationsMap.remove(relation);
                    }
                }
            }
            if (!relationsMap.isEmpty()) {
                importResult.setUpdatedRelatedEntities(true);
                ctx.addRelations(relationsMap.values());
            }
        });
    }

    private void importAttributes(User user, Map<String, List<AttributeExportData>> attributes, EntityImportResult<E> importResult) {
        E entity = importResult.getSavedEntity();
        importResult.addSaveReferencesCallback(() -> {
            attributes.forEach((scope, attributesExportData) -> {
                List<AttributeKvEntry> attributeKvEntries = attributesExportData.stream()
                        .map(attributeExportData -> {
                            KvEntry kvEntry;
                            String key = attributeExportData.getKey();
                            if (attributeExportData.getStrValue() != null) {
                                kvEntry = new StringDataEntry(key, attributeExportData.getStrValue());
                            } else if (attributeExportData.getBooleanValue() != null) {
                                kvEntry = new BooleanDataEntry(key, attributeExportData.getBooleanValue());
                            } else if (attributeExportData.getDoubleValue() != null) {
                                kvEntry = new DoubleDataEntry(key, attributeExportData.getDoubleValue());
                            } else if (attributeExportData.getLongValue() != null) {
                                kvEntry = new LongDataEntry(key, attributeExportData.getLongValue());
                            } else if (attributeExportData.getJsonValue() != null) {
                                kvEntry = new JsonDataEntry(key, attributeExportData.getJsonValue());
                            } else {
                                throw new IllegalArgumentException("Invalid attribute export data");
                            }
                            return new BaseAttributeKvEntry(kvEntry, attributeExportData.getLastUpdateTs());
                        })
                        .collect(Collectors.toList());
                // fixme: attributes are saved outside the transaction
                tsSubService.saveAttributes(AttributesSaveRequest.builder()
                        .tenantId(user.getTenantId())
                        .entityId(entity.getId())
                        .scope(scope)
                        .entries(attributeKvEntries)
                        .callback(new FutureCallback<>() {
                            @Override
                            public void onSuccess(@Nullable Void unused) {
                            }

                            @Override
                            public void onFailure(Throwable thr) {
                                log.error("Failed to import attributes for {} {}", entity.getId().getEntityType(), entity.getId(), thr);
                            }
                        })
                        .build());
            });
        });
    }

    protected boolean importCalculatedFields(EntitiesImportCtx ctx, E savedEntity, D exportData, IdProvider idProvider) {
        if (exportData.getCalculatedFields() == null || !ctx.isSaveCalculatedFields()) {
            return false;
        }

        boolean updated = false;
        List<CalculatedField> existing = calculatedFieldService.findCalculatedFieldsByEntityId(ctx.getTenantId(), savedEntity.getId());
        List<CalculatedField> fieldsToSave = exportData.getCalculatedFields().stream()
                .peek(calculatedField -> {
                    calculatedField.setTenantId(ctx.getTenantId());
                    calculatedField.setEntityId(savedEntity.getId());
                    if (calculatedField.getConfiguration() instanceof ArgumentsBasedCalculatedFieldConfiguration argBasedConfig) {
                        if (argBasedConfig instanceof GeofencingCalculatedFieldConfiguration geofencingCfg) {
                            geofencingCfg.getZoneGroups().values().forEach(zoneGroupConfiguration -> {
                                if (zoneGroupConfiguration.getRefEntityId() != null) {
                                    zoneGroupConfiguration.setRefEntityId(idProvider.getInternalId(zoneGroupConfiguration.getRefEntityId(), ctx.isFinalImportAttempt()));
                                }
                            });
                        } else {
                            argBasedConfig.getArguments().values().forEach(argument -> {
                                if (argument.getRefEntityId() != null) {
                                    argument.setRefEntityId(idProvider.getInternalId(argument.getRefEntityId(), ctx.isFinalImportAttempt()));
                                }
                            });
                        }
                    }
                }).toList();

        for (CalculatedField existingField : existing) {
            boolean found = fieldsToSave.stream().anyMatch(importedField -> compareCalculatedFields(existingField, importedField));
            if (!found) {
                calculatedFieldService.deleteCalculatedField(ctx.getTenantId(), existingField.getId());
                updated = true;
            }
        }

        for (CalculatedField calculatedField : fieldsToSave) {
            boolean found = existing.stream().anyMatch(existingField -> compareCalculatedFields(existingField, calculatedField));
            if (!found) {
                calculatedFieldService.save(calculatedField);
                updated = true;
            }
        }
        return updated;
    }

    private boolean compareCalculatedFields(CalculatedField existingField, CalculatedField newField) {
        CalculatedField oldCopy = new CalculatedField(existingField);
        CalculatedField newCopy = new CalculatedField(newField);
        oldCopy.setId(null);
        newCopy.setId(null);
        oldCopy.setVersion(null);
        newCopy.setVersion(null);
        oldCopy.setCreatedTime(0);
        newCopy.setCreatedTime(0);
        return oldCopy.equals(newCopy);
    }

    protected void onEntitySaved(User user, E savedEntity, E oldEntity) throws ThingsboardException {
        logEntityActionService.logEntityAction(user.getTenantId(), savedEntity.getId(), savedEntity, null,
                oldEntity == null ? ActionType.ADDED : ActionType.UPDATED, user);
    }

    @SuppressWarnings("unchecked")
    protected E findExistingEntity(EntitiesImportCtx ctx, E entity, IdProvider idProvider) {
        return (E) Optional.ofNullable(entitiesService.findEntityByTenantIdAndExternalId(ctx.getTenantId(), entity.getId()))
                .or(() -> Optional.ofNullable(entitiesService.findEntityByTenantIdAndId(ctx.getTenantId(), entity.getId())))
                .or(() -> {
                    if (ctx.isFindExistingByName()) {
                        return Optional.ofNullable(entitiesService.findEntityByTenantIdAndName(ctx.getTenantId(), getEntityType(), entity.getName()));
                    } else {
                        return Optional.empty();
                    }
                })
                .or(() -> {
                    if (entity instanceof HasDefaultOption hasDefaultOption) {
                        if (hasDefaultOption.isDefault()) {
                            return Optional.ofNullable(entitiesService.findDefaultEntityByTenantId(ctx.getTenantId(), getEntityType()));
                        }
                    }
                    return Optional.empty();
                })
                .orElse(null);
    }

    @SuppressWarnings("unchecked")
    private <ID extends EntityId> HasId<ID> findInternalEntity(TenantId tenantId, ID externalId) {
        return (HasId<ID>) Optional.ofNullable(entitiesService.findEntityByTenantIdAndExternalId(tenantId, externalId))
                .or(() -> Optional.ofNullable(entitiesService.findEntityByTenantIdAndId(tenantId, externalId)))
                .orElseThrow(() -> new MissingEntityException(externalId));
    }

    @SuppressWarnings("unchecked")
    @RequiredArgsConstructor
    protected class IdProvider {

        private final EntitiesImportCtx ctx;
        private final EntityImportResult<E> importResult;

        public <ID extends EntityId> ID getInternalId(ID externalId) {
            return getInternalId(externalId, true);
        }

        public <ID extends EntityId> ID getInternalId(ID externalId, boolean throwExceptionIfNotFound) {
            if (externalId == null || externalId.isNullUid()) {
                return null;
            }

            if (EntityType.TENANT.equals(externalId.getEntityType())) {
                return (ID) ctx.getTenantId();
            }

            EntityId localId = ctx.getInternalId(externalId);
            if (localId != null) {
                return (ID) localId;
            }

            HasId<ID> entity;
            try {
                entity = findInternalEntity(ctx.getTenantId(), externalId);
            } catch (Exception e) {
                if (throwExceptionIfNotFound) {
                    throw e;
                } else {
                    importResult.setUpdatedAllExternalIds(false);
                    return null;
                }
            }
            ctx.putInternalId(externalId, entity.getId());
            return entity.getId();
        }

        public Optional<EntityId> getInternalIdByUuid(UUID externalUuid, boolean fetchAllUUIDs, Set<EntityType> hints) {
            if (externalUuid.equals(EntityId.NULL_UUID)) {
                return Optional.empty();
            }

            for (EntityType entityType : EntityType.values()) {
                Optional<EntityId> externalId = buildEntityId(entityType, externalUuid);
                if (externalId.isEmpty()) {
                    continue;
                }
                EntityId internalId = ctx.getInternalId(externalId.get());
                if (internalId != null) {
                    return Optional.of(internalId);
                }
            }

            if (fetchAllUUIDs) {
                Set<EntityType> processLast = Set.of(EntityType.TENANT);
                List<EntityType> entityTypes = new ArrayList<>(hints);
                for (EntityType entityType : EntityType.values()) {
                    if (!hints.contains(entityType) && !processLast.contains(entityType)) {
                        entityTypes.add(entityType);
                    }
                }
                entityTypes.addAll(processLast);

                for (EntityType entityType : entityTypes) {
                    Optional<EntityId> externalId = buildEntityId(entityType, externalUuid);
                    if (externalId.isEmpty() || ctx.isNotFound(externalId.get())) {
                        continue;
                    }
                    EntityId internalId = getInternalId(externalId.get(), false);
                    if (internalId != null) {
                        return Optional.of(internalId);
                    } else {
                        ctx.registerNotFound(externalId.get());
                    }
                }
            }

            importResult.setUpdatedAllExternalIds(false);
            return Optional.empty();
        }

        private Optional<EntityId> buildEntityId(EntityType entityType, UUID externalUuid) {
            try {
                return Optional.of(EntityIdFactory.getByTypeAndUuid(entityType, externalUuid));
            } catch (Exception e) {
                return Optional.empty();
            }
        }

    }

    protected void replaceIdsRecursively(EntitiesImportCtx ctx, IdProvider idProvider, JsonNode json,
                                         Set<String> skippedRootFields, Pattern includedFieldsPattern,
                                         LinkedHashSet<EntityType> hints) {
        JacksonUtil.replaceUuidsRecursively(json, skippedRootFields, includedFieldsPattern,
                uuid -> idProvider.getInternalIdByUuid(uuid, ctx.isFinalImportAttempt(), hints)
                        .map(EntityId::getId).orElse(uuid), true);
    }

}
