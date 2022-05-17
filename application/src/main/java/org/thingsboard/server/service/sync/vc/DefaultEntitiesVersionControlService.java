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
import org.apache.commons.lang3.StringUtils;
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
import org.thingsboard.server.service.sync.exportimport.EntitiesExportImportService;
import org.thingsboard.server.service.sync.exportimport.exporting.ExportableEntitiesService;
import org.thingsboard.server.service.sync.exportimport.exporting.data.EntityExportData;
import org.thingsboard.server.service.sync.exportimport.exporting.data.EntityExportSettings;
import org.thingsboard.server.service.sync.exportimport.importing.data.EntityImportResult;
import org.thingsboard.server.service.sync.exportimport.importing.data.EntityImportSettings;
import org.thingsboard.server.service.sync.vc.data.EntitiesVersionControlSettings;
import org.thingsboard.server.service.sync.vc.data.EntityVersion;
import org.thingsboard.server.service.sync.vc.data.VersionCreationResult;
import org.thingsboard.server.service.sync.vc.data.VersionLoadResult;
import org.thingsboard.server.service.sync.vc.data.VersionedEntityInfo;
import org.thingsboard.server.service.sync.vc.data.request.create.EntityListVersionCreateRequest;
import org.thingsboard.server.service.sync.vc.data.request.create.ComplexVersionCreateRequest;
import org.thingsboard.server.service.sync.vc.data.request.create.SingleEntityVersionCreateRequest;
import org.thingsboard.server.service.sync.vc.data.request.create.SyncStrategy;
import org.thingsboard.server.service.sync.vc.data.request.create.VersionCreateConfig;
import org.thingsboard.server.service.sync.vc.data.request.create.VersionCreateRequest;
import org.thingsboard.server.service.sync.vc.data.request.load.EntityTypeVersionLoadRequest;
import org.thingsboard.server.service.sync.vc.data.request.load.SingleEntityVersionLoadRequest;
import org.thingsboard.server.service.sync.vc.data.request.load.VersionLoadConfig;
import org.thingsboard.server.service.sync.vc.data.request.load.VersionLoadRequest;
import org.thingsboard.server.utils.GitRepository;
import org.thingsboard.server.utils.ThrowingRunnable;

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

    private final EntitiesExportImportService exportImportService;
    private final ExportableEntitiesService exportableEntitiesService;
    private final AttributesService attributesService;
    private final EntityService entityService;
    private final TenantDao tenantDao;
    private final TransactionTemplate transactionTemplate;

    // TODO [viacheslav]: concurrency
    private final Map<TenantId, GitRepository> repositories = new ConcurrentHashMap<>();
    @Value("${java.io.tmpdir}/repositories")
    private String repositoriesFolder;

    private static final String SETTINGS_KEY = "vc";
    private final ObjectWriter jsonWriter = new ObjectMapper().writer(SerializationFeature.INDENT_OUTPUT);


    @AfterStartUp
    public void init() {
        DaoUtil.processInBatches(tenantDao::findTenantsIds, 100, tenantId -> {
            EntitiesVersionControlSettings settings = getSettings(tenantId);
            if (settings != null) {
                try {
                    initRepository(tenantId, settings);
                } catch (Exception e) {
                    log.warn("Failed to init repository for tenant {}", tenantId, e);
                }
            }
        });
        Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(() -> {
            repositories.forEach((tenantId, repository) -> {
                try {
                    repository.fetch();
                    log.info("Fetching remote repository for tenant {}", tenantId);
                } catch (Exception e) {
                    log.warn("Failed to fetch repository for tenant {}", tenantId, e);
                }
            });
        }, 5, 5, TimeUnit.SECONDS);
    }


    @Override
    public VersionCreationResult saveEntitiesVersion(SecurityUser user, VersionCreateRequest request) throws Exception {
        GitRepository repository = checkRepository(user.getTenantId());

        repository.fetch();
        if (repository.listBranches().contains(request.getBranch())) {
            repository.checkout(request.getBranch());
            repository.merge(request.getBranch());
        } else { // TODO [viacheslav]: rollback orphan branch on failure
            repository.createAndCheckoutOrphanBranch(request.getBranch()); // FIXME [viacheslav]: Checkout returned unexpected result NO_CHANGE for master branch
        }

        switch (request.getType()) {
            case SINGLE_ENTITY: {
                SingleEntityVersionCreateRequest versionCreateRequest = (SingleEntityVersionCreateRequest) request;
                saveEntityData(user, repository, versionCreateRequest.getEntityId(), versionCreateRequest.getConfig());
                break;
            }
            case ENTITY_LIST: {
                EntityListVersionCreateRequest versionCreateRequest = (EntityListVersionCreateRequest) request;
                if (versionCreateRequest.getConfig().getSyncStrategy() == SyncStrategy.OVERWRITE) {
                    versionCreateRequest.getEntitiesIds().stream()
                            .map(EntityId::getEntityType).distinct()
                            .forEach(entityType -> {
                                try {
                                    FileUtils.deleteDirectory(Path.of(repository.getDirectory(), getRelativePath(entityType, null)).toFile());
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            });
                }
                for (EntityId entityId : versionCreateRequest.getEntitiesIds()) {
                    saveEntityData(user, repository, entityId, versionCreateRequest.getConfig());
                }
                break;
            }
            case COMPLEX: {
                ComplexVersionCreateRequest versionCreateRequest = (ComplexVersionCreateRequest) request;
                versionCreateRequest.getEntityTypes().forEach((entityType, config) -> {
                    if (config.getSyncStrategy() == SyncStrategy.OVERWRITE) {
                        try {
                            FileUtils.deleteDirectory(Path.of(repository.getDirectory(), getRelativePath(entityType, null)).toFile());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }

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
                            saveEntityData(user, repository, entityId, config);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });
                });
                break;
            }
        }

        repository.add(".");

        VersionCreationResult result = new VersionCreationResult();
        GitRepository.Status status = repository.status();
        result.setAdded(status.getAdded().size());
        result.setModified(status.getModified().size());
        result.setRemoved(status.getRemoved().size());

        GitRepository.Commit commit = repository.commit(request.getVersionName());
        repository.push();

        result.setVersion(toVersion(commit));
        return result;
    }

    private void saveEntityData(SecurityUser user, GitRepository repository, EntityId entityId, VersionCreateConfig config) throws Exception {
        EntityExportData<ExportableEntity<EntityId>> entityData = exportImportService.exportEntity(user, entityId, EntityExportSettings.builder()
                .exportRelations(config.isSaveRelations())
                .build());
        String entityDataJson = jsonWriter.writeValueAsString(entityData);
        FileUtils.write(Path.of(repository.getDirectory(), getRelativePath(entityData.getEntityType(),
                entityData.getEntity().getId().toString())).toFile(), entityDataJson, StandardCharsets.UTF_8);
    }


    @Override
    public List<EntityVersion> listEntityVersions(TenantId tenantId, String branch, EntityId externalId) throws Exception {
        return listVersions(tenantId, branch, getRelativePath(externalId.getEntityType(), externalId.getId().toString()));
    }

    @Override
    public List<EntityVersion> listEntityTypeVersions(TenantId tenantId, String branch, EntityType entityType) throws Exception {
        return listVersions(tenantId, branch, getRelativePath(entityType, null));
    }

    @Override
    public List<EntityVersion> listVersions(TenantId tenantId, String branch) throws Exception {
        return listVersions(tenantId, branch, null);
    }

    private List<EntityVersion> listVersions(TenantId tenantId, String branch, String path) throws Exception {
        GitRepository repository = checkRepository(tenantId);
        return repository.listCommits(branch, path, Integer.MAX_VALUE).stream()
                .map(this::toVersion)
                .collect(Collectors.toList());
    }


    @Override
    public List<VersionedEntityInfo> listEntitiesAtVersion(TenantId tenantId, EntityType entityType, String branch, String versionId) throws Exception {
        return listEntitiesAtVersion(tenantId, branch, versionId, getRelativePath(entityType, null));
    }

    @Override
    public List<VersionedEntityInfo> listAllEntitiesAtVersion(TenantId tenantId, String branch, String versionId) throws Exception {
        return listEntitiesAtVersion(tenantId, branch, versionId, null);
    }

    private List<VersionedEntityInfo> listEntitiesAtVersion(TenantId tenantId, String branch, String versionId, String path) throws Exception {
        GitRepository repository = checkRepository(tenantId);
        checkVersion(tenantId, branch, versionId);
        return repository.listFilesAtCommit(versionId, path).stream()
                .map(filePath -> {
                    EntityId entityId = fromRelativePath(filePath);
                    VersionedEntityInfo info = new VersionedEntityInfo();
                    info.setExternalId(entityId);
                    return info;
                })
                .collect(Collectors.toList());
    }


    @Override
    public List<VersionLoadResult> loadEntitiesVersion(SecurityUser user, VersionLoadRequest request) throws Exception {
        GitRepository repository = checkRepository(user.getTenantId());

        EntityVersion version = checkVersion(user.getTenantId(), request.getBranch(), request.getVersionId());

        switch (request.getType()) {
            case SINGLE_ENTITY: {
                SingleEntityVersionLoadRequest versionLoadRequest = (SingleEntityVersionLoadRequest) request;
                EntityImportResult<?> importResult = transactionTemplate.execute(status -> {
                    try {
                        EntityImportResult<?> result = loadEntity(user, repository, versionLoadRequest.getExternalEntityId(), version.getId(), versionLoadRequest.getConfig());
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
                            remoteEntities = listEntitiesAtVersion(user.getTenantId(), request.getBranch(), request.getVersionId(), getRelativePath(entityType, null)).stream()
                                    .map(VersionedEntityInfo::getExternalId)
                                    .collect(Collectors.toSet());
                            for (EntityId externalEntityId : remoteEntities) {
                                EntityImportResult<?> importResult = loadEntity(user, repository, externalEntityId, version.getId(), config);

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

    private EntityImportResult<?> loadEntity(SecurityUser user, GitRepository repository, EntityId externalId, String versionId, VersionLoadConfig config) throws Exception {
        String entityDataJson = repository.getFileContentAtCommit(getRelativePath(externalId.getEntityType(), externalId.getId().toString()), versionId);
        EntityExportData entityData = JacksonUtil.fromString(entityDataJson, EntityExportData.class);

        return exportImportService.importEntity(user, entityData, EntityImportSettings.builder()
                .updateRelations(config.isLoadRelations())
                .findExistingByName(config.isFindExistingEntityByName())
                .build(), false, false);
    }


    @Override
    public List<String> listBranches(TenantId tenantId) throws Exception {
        GitRepository repository = checkRepository(tenantId);
        return repository.listBranches();
    }


    private EntityVersion checkVersion(TenantId tenantId, String branch, String versionId) throws Exception {
        return listVersions(tenantId, branch, null).stream()
                .filter(version -> version.getId().equals(versionId))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Version not found"));
    }

    private GitRepository checkRepository(TenantId tenantId) {
        return Optional.ofNullable(repositories.get(tenantId))
                .orElseThrow(() -> new IllegalStateException("Repository is not initialized"));
    }

    private void initRepository(TenantId tenantId, EntitiesVersionControlSettings settings) throws Exception {
        Path repositoryDirectory = Path.of(repositoriesFolder, tenantId.getId().toString());
        GitRepository repository;
        FileUtils.forceDelete(repositoryDirectory.toFile());

        Files.createDirectories(repositoryDirectory);
        repository = GitRepository.clone(settings.getRepositoryUri(), settings.getUsername(), settings.getPassword(), repositoryDirectory.toFile());
        repositories.put(tenantId, repository);
    }

    private void clearRepository(TenantId tenantId) throws IOException {
        GitRepository repository = repositories.get(tenantId);
        if (repository != null) {
            FileUtils.deleteDirectory(new File(repository.getDirectory()));
            repositories.remove(tenantId);
        }
    }


    @SneakyThrows
    @Override
    public void saveSettings(TenantId tenantId, EntitiesVersionControlSettings settings) {
        attributesService.save(tenantId, tenantId, DataConstants.SERVER_SCOPE, List.of(
                new BaseAttributeKvEntry(System.currentTimeMillis(), new JsonDataEntry(SETTINGS_KEY, JacksonUtil.toString(settings)))
        )).get();

        clearRepository(tenantId);
        initRepository(tenantId, settings);
    }

    @SneakyThrows
    @Override
    public EntitiesVersionControlSettings getSettings(TenantId tenantId) {
        return attributesService.find(tenantId, tenantId, DataConstants.SERVER_SCOPE, SETTINGS_KEY).get()
                .flatMap(KvEntry::getJsonValue)
                .map(json -> {
                    try {
                        return JacksonUtil.fromString(json, EntitiesVersionControlSettings.class);
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .orElse(null);
    }


    private EntityVersion toVersion(GitRepository.Commit commit) {
        return new EntityVersion(commit.getId(), commit.getMessage());
    }

    private String getRelativePath(EntityType entityType, String entityId) {
        String path = entityType.name().toLowerCase();
        if (entityId != null) {
            path += "/" + entityId + ".json";
        }
        return path;
    }

    private EntityId fromRelativePath(String path) {
        EntityType entityType = EntityType.valueOf(StringUtils.substringBefore(path, "/").toUpperCase());
        String entityId = StringUtils.substringBetween(path, "/", ".json");
        return EntityIdFactory.getByTypeAndUuid(entityType, entityId);
    }

}
