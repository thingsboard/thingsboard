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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
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
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityDataSortOrder;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
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
import org.thingsboard.server.service.sync.vc.data.request.create.MultipleEntitiesVersionCreateConfig;
import org.thingsboard.server.service.sync.vc.data.request.load.VersionLoadRequest;
import org.thingsboard.server.service.sync.vc.data.request.load.VersionLoadSettings;
import org.thingsboard.server.service.sync.vc.data.request.create.VersionCreateRequest;
import org.thingsboard.server.service.sync.vc.data.request.create.EntitiesByCustomFilterVersionCreateConfig;
import org.thingsboard.server.service.sync.vc.data.request.create.EntitiesByCustomQueryVersionCreateConfig;
import org.thingsboard.server.service.sync.vc.data.request.create.EntityListVersionCreateConfig;
import org.thingsboard.server.service.sync.vc.data.request.create.EntityTypeVersionCreateConfig;
import org.thingsboard.server.service.sync.vc.data.request.create.SingleEntityVersionCreateConfig;
import org.thingsboard.server.service.sync.vc.data.request.create.VersionCreateConfig;
import org.thingsboard.server.utils.GitRepository;

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

    // TODO [viacheslav]: concurrency
    private final Map<TenantId, GitRepository> repositories = new ConcurrentHashMap<>();
    @Value("${java.io.tmpdir}")
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
        repository.getLock().writeLock().lock();

        try {
            repository.fetch();
            if (repository.listBranches().contains(request.getBranch())) {
                repository.checkout(request.getBranch());
                repository.merge(request.getBranch());
            } else { // TODO [viacheslav]: rollback orphan branch on failure
                repository.createAndCheckoutOrphanBranch(request.getBranch());            // FIXME [viacheslav]: Checkout returned unexpected result NO_CHANGE for master branch
            }

            for (VersionCreateConfig config : request.getConfigs()) {
                EntityExportSettings exportSettings = EntityExportSettings.builder()
                        .exportRelations(config.isSaveRelations())
                        .build();

                List<EntityExportData<?>> entityDataList = new ArrayList<>();
                for (EntityId entityId : findEntities(user, config, 0, Integer.MAX_VALUE)) { // TODO [viacheslav]: find with pagination
                    EntityExportData<ExportableEntity<EntityId>> entityData = exportImportService.exportEntity(user, entityId, exportSettings);
                    entityDataList.add(entityData);
                }

                if (config instanceof MultipleEntitiesVersionCreateConfig &&
                        ((MultipleEntitiesVersionCreateConfig) config).isRemoveOtherRemoteEntitiesOfType()) {
                    entityDataList.stream() // FIXME [viacheslav]: in case of an emtpy entity type? none will be deleted on remote?
                            .map(EntityExportData::getEntityType)
                            .distinct()
                            .forEach(entityType -> {
                                try {
                                    FileUtils.deleteDirectory(Path.of(repository.getDirectory(), getRelativePath(entityType, null)).toFile());
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            });
                }

                for (EntityExportData<?> entityData : entityDataList) {
                    String entityDataJson = jsonWriter.writeValueAsString(entityData);
                    FileUtils.write(Path.of(repository.getDirectory(), getRelativePath(entityData.getEntityType(),
                            entityData.getEntity().getId().toString())).toFile(), entityDataJson, StandardCharsets.UTF_8);
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
        } finally {
            repository.getLock().writeLock().unlock();
        }
    }

    private List<EntityId> findEntities(SecurityUser user, VersionCreateConfig config, int page, int pageSize) {
        switch (config.getType()) {
            case SINGLE_ENTITY: {
                SingleEntityVersionCreateConfig filter = (SingleEntityVersionCreateConfig) config;
                return List.of(filter.getEntityId());
            }
            case ENTITY_LIST: {
                EntityListVersionCreateConfig filter = (EntityListVersionCreateConfig) config;
                return filter.getEntitiesIds();
            }
            case ENTITY_TYPE: {
                EntityTypeVersionCreateConfig filter = (EntityTypeVersionCreateConfig) config;
                EntitiesByCustomFilterVersionCreateConfig newFilter = new EntitiesByCustomFilterVersionCreateConfig();

                org.thingsboard.server.common.data.query.EntityTypeFilter entityTypeFilter = new org.thingsboard.server.common.data.query.EntityTypeFilter();
                entityTypeFilter.setEntityType(filter.getEntityType());

                newFilter.setFilter(entityTypeFilter);
                newFilter.setCustomerId(filter.getCustomerId());
                return findEntities(user, newFilter, page, pageSize);
            }
            case CUSTOM_ENTITY_FILTER: {
                EntitiesByCustomFilterVersionCreateConfig filter = (EntitiesByCustomFilterVersionCreateConfig) config;
                EntitiesByCustomQueryVersionCreateConfig newFilter = new EntitiesByCustomQueryVersionCreateConfig();

                EntityDataPageLink pageLink = new EntityDataPageLink();
                pageLink.setPage(page);
                pageLink.setPageSize(pageSize);
                EntityKey sortProperty = new EntityKey(EntityKeyType.ENTITY_FIELD, CREATED_TIME);
                pageLink.setSortOrder(new EntityDataSortOrder(sortProperty, EntityDataSortOrder.Direction.DESC));
                EntityDataQuery query = new EntityDataQuery(filter.getFilter(), pageLink, List.of(sortProperty), Collections.emptyList(), Collections.emptyList());

                newFilter.setQuery(query);
                newFilter.setCustomerId(filter.getCustomerId());
                return findEntities(user, newFilter, page, pageSize);
            }
            case CUSTOM_ENTITY_QUERY: {
                EntitiesByCustomQueryVersionCreateConfig filter = (EntitiesByCustomQueryVersionCreateConfig) config;
                CustomerId customerId = new CustomerId(ObjectUtils.defaultIfNull(filter.getCustomerId(), EntityId.NULL_UUID));
                return entityService.findEntityDataByQuery(user.getTenantId(), customerId, filter.getQuery()).getData()
                        .stream().map(EntityData::getEntityId)
                        .collect(Collectors.toList());
            }
        }
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
        repository.getLock().readLock().lock();
        try {
            return repository.listCommits(branch, path, Integer.MAX_VALUE).stream()
                    .map(this::toVersion)
                    .collect(Collectors.toList());
        } finally {
            repository.getLock().readLock().unlock();
        }
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
        repository.getLock().readLock().lock();
        try {
            checkVersion(tenantId, branch, versionId);
            return repository.listFilesAtCommit(versionId, path).stream()
                    .map(filePath -> {
                        EntityId entityId = fromRelativePath(filePath);
                        EntityExportData<?> entityData = getEntityDataAtVersion(tenantId, entityId, versionId);

                        VersionedEntityInfo info = new VersionedEntityInfo();
                        info.setExternalId(entityId);
                        info.setEntityName(entityData.getEntity().getName());
                        return info;
                    })
                    .collect(Collectors.toList());
        } finally {
            repository.getLock().readLock().unlock();
        }
    }


    @Override
    public List<VersionLoadResult> loadEntitiesVersion(SecurityUser user, VersionLoadRequest request) throws Exception {
        GitRepository repository = checkRepository(user.getTenantId());

        List<EntityExportData<?>> entityDataList = new ArrayList<>();
        EntityVersion version;
        repository.getLock().readLock().lock();
        try {
            version = checkVersion(user.getTenantId(), request.getBranch(), request.getVersionId());
            for (VersionedEntityInfo info : listEntitiesAtVersion(user.getTenantId(), request.getBranch(), request.getVersionId(), path)) {
                EntityExportData<?> entityData = getEntityDataAtVersion(user.getTenantId(), info.getExternalId(), versionId);
                entityDataList.add(entityData);
            }
        } finally {
            repository.getLock().readLock().unlock();
        }

        EntityImportSettings importSettings = EntityImportSettings.builder()
                .updateRelations(settings.isLoadRelations())
                .findExistingByName(settings.isFindExistingEntityByName())
                .build();
        // FIXME [viacheslav]: do evrth in transaction
        List<EntityImportResult<?>> importResults = exportImportService.importEntities(user, entityDataList, importSettings);

        Map<EntityType, VersionLoadResult> results = new HashMap<>();

        boolean removeNonexistentLocalEntities = false;
        if ()
            if (request.isRemoveOtherLocalEntitiesOfType()) {
                importResults.stream()
                        .collect(Collectors.groupingBy(EntityImportResult::getEntityType)) // FIXME [viacheslav]: if no entities of entity type - remove all ?
                        .forEach((entityType, resultsForEntityType) -> {
                            Set<EntityId> modifiedEntities = resultsForEntityType.stream().map(EntityImportResult::getSavedEntity).map(ExportableEntity::getExternalId).collect(Collectors.toSet());
                            AtomicInteger deleted = new AtomicInteger();

                            DaoUtil.processInBatches(pageLink -> {
                                return exportableEntitiesService.findEntitiesByTenantId(user.getTenantId(), entityType, pageLink);
                            }, 100, entity -> {
                                if (entity.getExternalId() == null || !modifiedEntities.contains(entity.getExternalId())) {
                                    try {
                                        exportableEntitiesService.checkPermission(user, entity, entityType, Operation.DELETE);
                                    } catch (ThingsboardException e) {
                                        throw new RuntimeException(e);
                                    }
                                    // need to delete in a specific order?
                                    exportableEntitiesService.deleteByTenantIdAndId(user.getTenantId(), entity.getId());
                                    deleted.getAndIncrement();
                                }
                            });
                            results.put(entityType, VersionLoadResult.builder()
                                    .entityType(entityType)
                                    .created((int) resultsForEntityType.stream().filter(importResult -> importResult.getOldEntity() == null).count())
                                    .updated((int) resultsForEntityType.stream().filter(importResult -> importResult.getOldEntity() != null).count())
                                    .deleted(deleted.get())
                                    .build());
                        });
            }

        return new ArrayList<>(results.values());
    }

    @SneakyThrows
    private EntityExportData<?> getEntityDataAtVersion(TenantId tenantId, EntityId externalId, String versionId) {
        GitRepository repository = checkRepository(tenantId);
        repository.getLock().readLock().lock();
        try {
            String entityDataJson = repository.getFileContentAtCommit(getRelativePath(externalId.getEntityType(), externalId.getId().toString()), versionId);
            return JacksonUtil.fromString(entityDataJson, EntityExportData.class);
        } finally {
            repository.getLock().readLock().unlock();
        }
    }

    private void updateEntityVersionInfo(TenantId tenantId, EntityId entityId, EntityVersion version) {
        ObjectNode versionInfo = JacksonUtil.newObjectNode();
        versionInfo.set("versionName", new TextNode(version.getName()));
        versionInfo.set("versionId", new TextNode(version.getId()));
        attributesService.save(tenantId, entityId, DataConstants.SERVER_SCOPE,
                List.of(new BaseAttributeKvEntry(System.currentTimeMillis(), new JsonDataEntry("entityVersionInfo", versionInfo.toString()))));
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
        if (Files.exists(repositoryDirectory)) {
            repository = GitRepository.open(repositoryDirectory.toFile(), settings.getUsername(), settings.getPassword());
        } else {
            Files.createDirectories(repositoryDirectory);
            repository = GitRepository.clone(settings.getRepositoryUri(), settings.getUsername(), settings.getPassword(), repositoryDirectory.toFile());
        }
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
