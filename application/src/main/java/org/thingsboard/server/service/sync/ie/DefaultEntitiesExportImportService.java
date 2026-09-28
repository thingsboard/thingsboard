// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.sync.ie;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cache.limits.RateLimitService;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.limit.LimitedApi;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.common.data.sync.ie.EntityImportResult;
import org.thingsboard.server.common.data.util.ThrowingRunnable;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.subscription.PlatformFeature;
import org.thingsboard.server.dao.subscription.SubscriptionService;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.TbLogEntityActionService;
import org.thingsboard.server.service.sync.ie.exporting.EntityExportService;
import org.thingsboard.server.service.sync.ie.exporting.ExportableEntitiesService;
import org.thingsboard.server.service.sync.ie.exporting.impl.BaseEntityExportService;
import org.thingsboard.server.service.sync.ie.exporting.impl.DefaultEntityExportService;
import org.thingsboard.server.service.sync.ie.importing.EntityImportService;
import org.thingsboard.server.service.sync.ie.importing.MissingEntityException;
import org.thingsboard.server.service.sync.vc.LoadEntityException;
import org.thingsboard.server.service.sync.vc.data.EntitiesExportCtx;
import org.thingsboard.server.service.sync.vc.data.EntitiesImportCtx;
import org.thingsboard.server.service.sync.vc.data.ReimportTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@Service
@TbCoreComponent
@RequiredArgsConstructor
@Slf4j
public class DefaultEntitiesExportImportService implements EntitiesExportImportService {

    /**
     * Entity types the licence may withhold: importing one is a management action and is refused when the
     * corresponding platform feature is disabled. The type alone does not always name every feature an import
     * needs - a report-producing scheduler event is a reporting write as well as a scheduler one - so
     * {@link #importEntity} checks that case explicitly on top of this map.
     */
    private static final Map<EntityType, PlatformFeature> LICENSED_ENTITY_TYPES = Map.of(
            EntityType.INTEGRATION, PlatformFeature.INTEGRATIONS,
            EntityType.CONVERTER, PlatformFeature.INTEGRATIONS,
            EntityType.SCHEDULER_EVENT, PlatformFeature.SCHEDULER,
            EntityType.REPORT_TEMPLATE, PlatformFeature.REPORTING);

    private final Map<EntityType, EntityExportService<?, ?, ?>> exportServices = new HashMap<>();
    private final Map<EntityType, EntityImportService<?, ?, ?>> importServices = new HashMap<>();

    private final RelationService relationService;
    private final SubscriptionService subscriptionService;
    private final RateLimitService rateLimitService;
    private final TbLogEntityActionService logEntityActionService;
    private final ExportableEntitiesService exportableEntitiesService;

    public static final List<EntityType> SUPPORTED_ENTITY_TYPES = List.of(
            EntityType.CUSTOMER, EntityType.USER, EntityType.ROLE, EntityType.ENTITY_GROUP, EntityType.RULE_CHAIN,
            EntityType.TB_RESOURCE, EntityType.DASHBOARD, EntityType.ASSET_PROFILE, EntityType.ASSET,
            EntityType.DEVICE_PROFILE, EntityType.OTA_PACKAGE, EntityType.DEVICE, EntityType.ENTITY_VIEW, EntityType.CONVERTER,
            EntityType.INTEGRATION, EntityType.WIDGET_TYPE, EntityType.WIDGETS_BUNDLE, EntityType.REPORT_TEMPLATE,
            EntityType.NOTIFICATION_TEMPLATE, EntityType.NOTIFICATION_TARGET, EntityType.NOTIFICATION_RULE, EntityType.SCHEDULER_EVENT,
            EntityType.AI_MODEL
    );

    @SuppressWarnings("unchecked")
    @Override
    public <E extends ExportableEntity<I>, I extends EntityId> EntityExportData<E> exportEntity(EntitiesExportCtx<?> ctx, I entityId) throws ThingsboardException {
        E entity = (E) exportableEntitiesService.findEntityByTenantIdAndId(ctx.getTenantId(), entityId);
        if (entity == null) {
            throw new IllegalArgumentException(entityId.getEntityType() + " [" + entityId.getId() + "] not found");
        }
        return exportEntity(ctx, entity);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public <E extends ExportableEntity<I>, I extends EntityId> EntityExportData<E> exportEntity(EntitiesExportCtx<?> ctx, E entity) throws ThingsboardException {
        if (!rateLimitService.checkRateLimit(LimitedApi.ENTITY_EXPORT, ctx.getTenantId())) {
            throw new ThingsboardException("Rate limit for entities export is exceeded", ThingsboardErrorCode.TOO_MANY_REQUESTS);
        }
        EntityExportService exportService = getExportService(entity.getId().getEntityType());
        return exportService.getExportData(ctx, entity);
    }

    @Override
    public <E extends ExportableEntity<I>, I extends EntityId> EntityImportResult<E> importEntity(EntitiesImportCtx ctx, EntityExportData<E> exportData) throws Exception {
        if (!rateLimitService.checkRateLimit(LimitedApi.ENTITY_IMPORT, ctx.getTenantId())) {
            throw new ThingsboardException("Rate limit for entities import is exceeded", ThingsboardErrorCode.TOO_MANY_REQUESTS);
        }
        if (exportData.getEntity() == null || exportData.getEntity().getId() == null) {
            throw new DataValidationException("Invalid entity data");
        }

        EntityType entityType = exportData.getEntityType();
        PlatformFeature requiredFeature = LICENSED_ENTITY_TYPES.get(entityType);
        if (requiredFeature != null) {
            subscriptionService.checkFeatureAllowed(ctx.getTenantId(), requiredFeature);
        }
        checkReportingAllowed(ctx, exportData);
        EntityImportService<I, E, EntityExportData<E>> importService = getImportService(entityType);

        EntityImportResult<E> importResult = importService.importEntity(ctx, exportData);
        ctx.putInternalId(exportData.getExternalId(), importResult.getSavedEntity().getId());

        ctx.addReferenceCallback(exportData.getExternalId(), importResult.getSaveReferencesCallback());
        if (ctx.isRollbackOnError()) {
            ctx.addEventCallback(importResult.getSendEventsCallback());
        } else {
            importResult.getSendEventsCallback().run();
        }
        return importResult;
    }

    /**
     * Importing a report-producing scheduler event needs REPORTING on top of the SCHEDULER its entity type asks
     * for: once imported, the event generates reports on its own schedule.
     */
    private void checkReportingAllowed(EntitiesImportCtx ctx, EntityExportData<?> exportData) throws ThingsboardException {
        if (exportData.getEntity() instanceof SchedulerEvent schedulerEvent
                && schedulerEvent.isReportProducing()) {
            subscriptionService.checkFeatureAllowed(ctx.getTenantId(), PlatformFeature.REPORTING);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void importEntities(EntitiesImportCtx ctx, Map<EntityType, List<EntityExportData<?>>> entities) throws ThingsboardException {
        List<EntityType> sortedTypes = entities.keySet().stream()
                .sorted(getEntityTypeComparatorForImport())
                .toList();

        // First pass: import all entities in dependency order
        for (EntityType entityType : sortedTypes) {
            List<EntityExportData<?>> entityList = entities.get(entityType);
            if (entityList == null) {
                continue;
            }
            log.debug("[{}] Importing {} entities", ctx.getTenantId(), entityType);
            for (EntityExportData entityData : entityList) {
                EntityExportData reimportBackup = JacksonUtil.clone(entityData);
                EntityId externalId = entityData.getExternalId();
                try {
                    EntityImportResult<?> importResult = importEntity(ctx, entityData);
                    registerResult(ctx, entityType, importResult, entityData);
                    if (!importResult.isUpdatedAllExternalIds()) {
                        ctx.getToReimport().put(externalId, new ReimportTask(reimportBackup, ctx.getSettings()));
                    } else {
                        ctx.getImportedEntities().computeIfAbsent(entityType, t -> new HashSet<>())
                                .add(importResult.getSavedEntity().getId());
                    }
                } catch (Exception e) {
                    log.error("[{}] First pass failed for entity {} (type={})", ctx.getTenantId(), externalId, entityType, e);
                    throw new LoadEntityException(externalId, e);
                }
            }
        }

        // Second pass: reimport entities with unresolved forward/circular references
        if (!ctx.getToReimport().isEmpty()) {
            log.debug("[{}] Reimporting {} entities with unresolved references", ctx.getTenantId(), ctx.getToReimport().size());
            ctx.setFinalImportAttempt(true);
            for (Map.Entry<EntityId, ReimportTask> entry : ctx.getToReimport().entrySet()) {
                EntityId externalId = entry.getKey();
                ReimportTask task = entry.getValue();
                ctx.setSettings(task.getSettings());
                try {
                    EntityImportResult<?> importResult = importEntity(ctx, task.getData());
                    ctx.getImportedEntities().computeIfAbsent(externalId.getEntityType(), t -> new HashSet<>())
                            .add(importResult.getSavedEntity().getId());
                } catch (Exception e) {
                    log.error("[{}] Reimport failed for entity {} (type={})", ctx.getTenantId(), externalId, externalId.getEntityType(), e);
                    throw new LoadEntityException(externalId, e);
                }
            }
        }

        // Save deferred references and relations
        saveReferencesAndRelations(ctx);
    }

    @Override
    public void saveReferencesAndRelations(EntitiesImportCtx ctx) throws ThingsboardException {
        for (Map.Entry<EntityId, ThrowingRunnable> callbackEntry : ctx.getReferenceCallbacks().entrySet()) {
            EntityId externalId = callbackEntry.getKey();
            ThrowingRunnable saveReferencesCallback = callbackEntry.getValue();
            try {
                saveReferencesCallback.run();
            } catch (MissingEntityException e) {
                throw new LoadEntityException(externalId, e);
            }
        }

        relationService.saveRelations(ctx.getTenantId(), new ArrayList<>(ctx.getRelations()));

        for (EntityRelation relation : ctx.getRelations()) {
            logEntityActionService.logEntityRelationAction(ctx.getTenantId(), null,
                    relation, ctx.getUser(), ActionType.RELATION_ADD_OR_UPDATE, null, relation);
        }
    }

    @Override
    public Comparator<EntityType> getEntityTypeComparatorForImport() {
        return Comparator.comparingInt(type -> {
            int index = SUPPORTED_ENTITY_TYPES.indexOf(type);
            return index >= 0 ? index : Integer.MAX_VALUE;
        });
    }

    @SuppressWarnings("rawtypes")
    private void registerResult(EntitiesImportCtx ctx, EntityType entityType, EntityImportResult<?> importResult, EntityExportData exportData) {
        boolean isGroup = exportData.getEntity() instanceof EntityGroup;
        if (isGroup) {
            entityType = ((EntityGroup) exportData.getEntity()).getType();
        }
        if (importResult.isCreated()) {
            ctx.registerResult(entityType, isGroup, true);
        } else if (importResult.isUpdated() || importResult.isUpdatedRelatedEntities()) {
            ctx.registerResult(entityType, isGroup, false);
        }
    }

    @SuppressWarnings("unchecked")
    private <I extends EntityId, E extends ExportableEntity<I>, D extends EntityExportData<E>> EntityExportService<I, E, D> getExportService(EntityType entityType) {
        EntityExportService<?, ?, ?> exportService = exportServices.get(entityType);
        if (exportService == null) {
            throw new IllegalArgumentException("Export for entity type " + entityType + " is not supported");
        }
        return (EntityExportService<I, E, D>) exportService;
    }

    @SuppressWarnings("unchecked")
    private <I extends EntityId, E extends ExportableEntity<I>, D extends EntityExportData<E>> EntityImportService<I, E, D> getImportService(EntityType entityType) {
        EntityImportService<?, ?, ?> importService = importServices.get(entityType);
        if (importService == null) {
            throw new IllegalArgumentException("Import for entity type " + entityType + " is not supported");
        }
        return (EntityImportService<I, E, D>) importService;
    }

    @Autowired
    private void setExportServices(DefaultEntityExportService<?, ?, ?> defaultExportService,
                                   Collection<BaseEntityExportService<?, ?, ?>> exportServices) {
        exportServices.stream()
                .sorted(Comparator.comparing(exportService -> exportService.getSupportedEntityTypes().size(), Comparator.reverseOrder()))
                .forEach(exportService -> {
                    exportService.getSupportedEntityTypes().forEach(entityType -> {
                        this.exportServices.put(entityType, exportService);
                    });
                });
        SUPPORTED_ENTITY_TYPES.forEach(entityType -> {
            this.exportServices.putIfAbsent(entityType, defaultExportService);
        });
    }

    @Autowired
    private void setImportServices(Collection<EntityImportService<?, ?, ?>> importServices) {
        importServices.forEach(entityImportService -> {
            this.importServices.put(entityImportService.getEntityType(), entityImportService);
        });
    }

}
