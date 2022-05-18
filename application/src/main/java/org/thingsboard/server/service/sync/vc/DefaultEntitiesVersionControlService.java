/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.service.sync.vc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityDataSortOrder;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.EntityTypeFilter;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.tenant.TenantDao;
import org.thingsboard.server.queue.util.AfterStartUp;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.sync.ie.EntitiesExportImportService;
import org.thingsboard.server.service.sync.ie.exporting.ExportableEntitiesService;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.common.data.sync.ie.EntityExportSettings;
import org.thingsboard.server.common.data.sync.ie.EntityImportResult;
import org.thingsboard.server.common.data.sync.ie.EntityImportSettings;
import org.thingsboard.server.common.data.sync.vc.EntitiesVersionControlSettings;
import org.thingsboard.server.common.data.sync.vc.EntityVersion;
import org.thingsboard.server.common.data.sync.vc.VersionCreationResult;
import org.thingsboard.server.common.data.sync.vc.VersionLoadResult;
import org.thingsboard.server.common.data.sync.vc.VersionedEntityInfo;
import org.thingsboard.server.common.data.sync.vc.request.create.ComplexVersionCreateRequest;
import org.thingsboard.server.common.data.sync.vc.request.create.SingleEntityVersionCreateRequest;
import org.thingsboard.server.common.data.sync.vc.request.create.SyncStrategy;
import org.thingsboard.server.common.data.sync.vc.request.create.VersionCreateConfig;
import org.thingsboard.server.common.data.sync.vc.request.create.VersionCreateRequest;
import org.thingsboard.server.common.data.sync.vc.request.load.EntityTypeVersionLoadRequest;
import org.thingsboard.server.common.data.sync.vc.request.load.SingleEntityVersionLoadRequest;
import org.thingsboard.server.common.data.sync.vc.request.load.VersionLoadConfig;
import org.thingsboard.server.common.data.sync.vc.request.load.VersionLoadRequest;
import org.thingsboard.server.common.data.sync.ThrowingRunnable;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.thingsboard.server.dao.sql.query.EntityKeyMapping.CREATED_TIME;

@Service
@TbCoreComponent
@RequiredArgsConstructor
@Slf4j
public class DefaultEntitiesVersionControlService implements EntitiesVersionControlService {

    private final GitVersionControlService gitService;
    private final EntitiesExportImportService exportImportService;
    private final ExportableEntitiesService exportableEntitiesService;
    private final AttributesService attributesService;
    private final EntityService entityService;
    private final TransactionTemplate transactionTemplate;

    public static final String SETTINGS_KEY = "vc";

    @Override
    public VersionCreationResult saveEntitiesVersion(SecurityUser user, VersionCreateRequest request) throws Exception {

        var commit = gitService.prepareCommit(user.getTenantId(), request);

        switch (request.getType()) {
            case SINGLE_ENTITY: {
                SingleEntityVersionCreateRequest versionCreateRequest = (SingleEntityVersionCreateRequest) request;
                saveEntityData(user, commit, versionCreateRequest.getEntityId(), versionCreateRequest.getConfig());
                break;
            }
            case COMPLEX: {
                ComplexVersionCreateRequest versionCreateRequest = (ComplexVersionCreateRequest) request;
                versionCreateRequest.getEntityTypes().forEach((entityType, config) -> {
                    if (ObjectUtils.defaultIfNull(config.getSyncStrategy(), versionCreateRequest.getSyncStrategy()) == SyncStrategy.OVERWRITE) {
                        gitService.deleteAll(commit, entityType);
                    }

                    if (config.isAllEntities()) {
                        EntityTypeFilter entityTypeFilter = new EntityTypeFilter();
                        entityTypeFilter.setEntityType(entityType);
                        EntityDataPageLink entityDataPageLink = new EntityDataPageLink();
                        entityDataPageLink.setPage(-1);
                        entityDataPageLink.setPageSize(-1);
                        EntityKey sortProperty = new EntityKey(EntityKeyType.ENTITY_FIELD, CREATED_TIME);
                        entityDataPageLink.setSortOrder(new EntityDataSortOrder(sortProperty, EntityDataSortOrder.Direction.DESC));
                        EntityDataQuery query = new EntityDataQuery(entityTypeFilter, entityDataPageLink, List.of(sortProperty), Collections.emptyList(), Collections.emptyList());

                        DaoUtil.processInBatches(pageLink -> {
                            entityDataPageLink.setPage(pageLink.getPage());
                            entityDataPageLink.setPageSize(pageLink.getPageSize());
                            return entityService.findEntityDataByQuery(user.getTenantId(), new CustomerId(EntityId.NULL_UUID), query);
                        }, 100, data -> {
                            EntityId entityId = data.getEntityId();
                            try {
                                saveEntityData(user, commit, entityId, config);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });
                    } else {
                        for (UUID entityId : config.getEntityIds()) {
                            try {
                                saveEntityData(user, commit, EntityIdFactory.getByTypeAndUuid(entityType, entityId), config);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }

                });
                break;
            }
        }

        return gitService.push(commit);
    }

    private void saveEntityData(SecurityUser user, PendingCommit commit, EntityId entityId, VersionCreateConfig config) throws Exception {
        EntityExportData<ExportableEntity<EntityId>> entityData = exportImportService.exportEntity(user, entityId, EntityExportSettings.builder()
                .exportRelations(config.isSaveRelations())
                .build());
        gitService.addToCommit(commit, entityData);
    }


    @Override
    public List<EntityVersion> listEntityVersions(TenantId tenantId, String branch, EntityId externalId) throws Exception {
        return gitService.listVersions(tenantId, branch, externalId);
    }

    @Override
    public List<EntityVersion> listEntityTypeVersions(TenantId tenantId, String branch, EntityType entityType) throws Exception {
        return gitService.listVersions(tenantId, branch, entityType);
    }

    @Override
    public List<EntityVersion> listVersions(TenantId tenantId, String branch) throws Exception {
        return gitService.listVersions(tenantId, branch);
    }

    @Override
    public List<VersionedEntityInfo> listEntitiesAtVersion(TenantId tenantId, String branch, String versionId, EntityType entityType) throws Exception {
        return gitService.listEntitiesAtVersion(tenantId, branch, versionId, entityType);
    }

    @Override
    public List<VersionedEntityInfo> listAllEntitiesAtVersion(TenantId tenantId, String branch, String versionId) throws Exception {
        return gitService.listEntitiesAtVersion(tenantId, branch, versionId);
    }

    @Override
    public List<VersionLoadResult> loadEntitiesVersion(SecurityUser user, VersionLoadRequest request) throws Exception {
        switch (request.getType()) {
            case SINGLE_ENTITY: {
                SingleEntityVersionLoadRequest versionLoadRequest = (SingleEntityVersionLoadRequest) request;
                EntityImportResult<?> importResult = transactionTemplate.execute(status -> {
                    try {
                        EntityImportResult<?> result = loadEntity(user, request, versionLoadRequest.getConfig(), versionLoadRequest.getExternalEntityId());
                        result.getSaveReferencesCallback().run();
                        return result;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
                try {
                    importResult.getSendEventsCallback().run();
                } catch (Exception e) {
                    log.error("Failed to send events for entity", e);
                }

                return List.of(VersionLoadResult.builder()
                        .created(importResult.getOldEntity() == null ? 1 : 0)
                        .updated(importResult.getOldEntity() != null ? 1 : 0)
                        .deleted(0)
                        .build());
            }
            case ENTITY_TYPE: {
                EntityTypeVersionLoadRequest versionLoadRequest = (EntityTypeVersionLoadRequest) request;
                return transactionTemplate.execute(status -> {
                    Map<EntityType, VersionLoadResult> results = new HashMap<>();
                    List<ThrowingRunnable> saveReferencesCallbacks = new ArrayList<>();
                    List<ThrowingRunnable> sendEventsCallbacks = new ArrayList<>();
                    // order entity types..
                    // or what
                    versionLoadRequest.getEntityTypes().forEach((entityType, config) -> {
                        AtomicInteger created = new AtomicInteger();
                        AtomicInteger updated = new AtomicInteger();
                        AtomicInteger deleted = new AtomicInteger();

                        Set<EntityId> remoteEntities;
                        try {
                            remoteEntities = listEntitiesAtVersion(user.getTenantId(), request.getBranch(), request.getVersionId(), entityType).stream()
                                    .map(VersionedEntityInfo::getExternalId)
                                    .collect(Collectors.toSet());
                            for (EntityId externalEntityId : remoteEntities) {
                                EntityImportResult<?> importResult = loadEntity(user, request, config, externalEntityId);

                                if (importResult.getOldEntity() == null) created.incrementAndGet();
                                else updated.incrementAndGet();

                                saveReferencesCallbacks.add(importResult.getSaveReferencesCallback());
                                sendEventsCallbacks.add(importResult.getSendEventsCallback());
                            }
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }

                        if (config.isRemoveOtherEntities()) {
                            DaoUtil.processInBatches(pageLink -> {
                                return exportableEntitiesService.findEntitiesByTenantId(user.getTenantId(), entityType, pageLink);
                            }, 100, entity -> {
                                if (entity.getExternalId() == null || !remoteEntities.contains(entity.getExternalId())) {
                                    try {
                                        exportableEntitiesService.checkPermission(user, entity, entityType, Operation.DELETE);
                                    } catch (ThingsboardException e) {
                                        throw new RuntimeException(e);
                                    }
                                    // need to delete entity types in a specific order?
                                    exportableEntitiesService.deleteByTenantIdAndId(user.getTenantId(), entity.getId());
                                    deleted.getAndIncrement();
                                }
                            });
                        }

                        results.put(entityType, VersionLoadResult.builder()
                                .created(created.get())
                                .updated(updated.get())
                                .deleted(deleted.get())
                                .build());
                    });

                    for (ThrowingRunnable saveReferencesCallback : saveReferencesCallbacks) {
                        try {
                            saveReferencesCallback.run();
                        } catch (ThingsboardException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    for (ThrowingRunnable sendEventsCallback : sendEventsCallbacks) {
                        try {
                            sendEventsCallback.run();
                        } catch (Exception e) {
                            log.error("Failed to send events for entity", e);
                        }
                    }
                    return new ArrayList<>(results.values());
                });
            }
            default:
                throw new IllegalArgumentException("Unsupported version load request");
        }
    }

    private EntityImportResult<?> loadEntity(SecurityUser user, VersionLoadRequest request, VersionLoadConfig config, EntityId entityId) throws Exception {
        EntityExportData entityData = gitService.getEntity(user.getTenantId(), request.getVersionId(), entityId);
        return exportImportService.importEntity(user, entityData, EntityImportSettings.builder()
                .updateRelations(config.isLoadRelations())
                .findExistingByName(config.isFindExistingEntityByName())
                .build(), false, false);
    }


    @Override
    public List<String> listBranches(TenantId tenantId) throws Exception {
        return gitService.listBranches(tenantId);
    }

    @SneakyThrows
    @Override
    public void saveSettings(TenantId tenantId, EntitiesVersionControlSettings settings) {
        attributesService.save(tenantId, tenantId, DataConstants.SERVER_SCOPE, List.of(
                new BaseAttributeKvEntry(System.currentTimeMillis(), new JsonDataEntry(SETTINGS_KEY, JacksonUtil.toString(settings)))
        )).get();

        gitService.initRepository(tenantId, settings);
    }

    @Override
    public EntitiesVersionControlSettings getSettings(TenantId tenantId) {
        return gitService.getSettings(tenantId);
    }
}
