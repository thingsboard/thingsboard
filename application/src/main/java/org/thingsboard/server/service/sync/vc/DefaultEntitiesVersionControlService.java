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
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.queue.util.AfterStartUp;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.sync.EntitiesExportImportService;
import org.thingsboard.server.service.sync.exporting.data.EntityExportData;
import org.thingsboard.server.service.sync.exporting.data.request.EntityExportSettings;
import org.thingsboard.server.service.sync.importing.data.EntityImportResult;
import org.thingsboard.server.service.sync.importing.data.EntityImportSettings;
import org.thingsboard.server.service.sync.vc.data.EntitiesVersionControlSettings;
import org.thingsboard.server.service.sync.vc.data.VersionedEntityInfo;
import org.thingsboard.server.service.sync.vc.data.EntityVersion;
import org.thingsboard.server.service.sync.vc.data.EntityVersionLoadResult;
import org.thingsboard.server.service.sync.vc.data.EntityVersionLoadSettings;
import org.thingsboard.server.service.sync.vc.data.EntityVersionSaveSettings;
import org.thingsboard.server.utils.GitRepository;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

@Service
@TbCoreComponent
@RequiredArgsConstructor
@Slf4j
public class DefaultEntitiesVersionControlService implements EntitiesVersionControlService {

    private final EntitiesExportImportService exportImportService;

    private GitRepository repository;
    private final ReadWriteLock repositoryLock = new ReentrantReadWriteLock();

    private ScheduledExecutorService fetchExecutor;
    private ScheduledFuture<?> fetchTask;

    private final AdminSettingsService adminSettingsService;
    private static final String SETTINGS_KEY = "vc";
    private final ObjectWriter jsonWriter = new ObjectMapper().writer(SerializationFeature.INDENT_OUTPUT);


    @AfterStartUp
    public void init() {
        EntitiesVersionControlSettings settings = getSettings();
        if (settings != null) {
            try {
                initRepository(settings);
            } catch (Exception e) {
                log.debug("Failed to init repository", e);
            }
        }

        int fetchPeriod = settings == null || settings.getFetchPeriod() == 0 ? 10 : settings.getFetchPeriod();
        fetchExecutor = Executors.newSingleThreadScheduledExecutor();
        fetchTask = scheduleFetch(fetchPeriod);
    }


    @Override
    public EntityVersion saveEntityVersion(SecurityUser user, EntityId entityId, String branch, String versionName, EntityVersionSaveSettings settings) throws Exception {
        return saveEntitiesVersion(user, List.of(entityId), branch, versionName, settings);
    }

    @Override
    public EntityVersion saveEntitiesVersion(SecurityUser user, List<EntityId> entitiesIds, String branch, String versionName, EntityVersionSaveSettings settings) throws Exception {
        repositoryLock.writeLock().lock();
        try {
            checkRepository();
            checkBranch(user.getTenantId(), branch);

            List<EntityExportData<?>> entityDataList = new ArrayList<>();
            EntityExportSettings exportSettings = EntityExportSettings.builder()
                    .exportRelations(settings.isSaveRelations())
                    .build();
            for (EntityId entityId : entitiesIds) {
                EntityExportData<ExportableEntity<EntityId>> entityData = exportImportService.exportEntity(user, entityId, exportSettings);
                entityDataList.add(entityData);
            }

            fetch();
            if (repository.listBranches().contains(branch)) {
                repository.checkout(branch);
                repository.merge(branch);
            } else {
                repository.createAndCheckoutOrphanBranch(branch);
            }

            for (EntityExportData<?> entityData : entityDataList) {
                String entityDataJson = jsonWriter.writeValueAsString(entityData);
                FileUtils.write(new File(repository.getDirectory() + "/" + getRelativePath(entityData.getEntityType(),
                        entityData.getEntity().getId().toString())), entityDataJson, StandardCharsets.UTF_8);
            }

            GitRepository.Commit commit = repository.commit(versionName, ".");
            repository.push();
            return toVersion(commit);
        } finally {
            repositoryLock.writeLock().unlock();
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
        repositoryLock.readLock().lock();
        try {
            checkRepository();
            checkBranch(tenantId, branch);

            return repository.listCommits(branch, path, Integer.MAX_VALUE).stream()
                    .map(this::toVersion)
                    .collect(Collectors.toList());
        } finally {
            repositoryLock.readLock().unlock();
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
        repositoryLock.readLock().lock();
        try {
            checkRepository();
            checkBranch(tenantId, branch);
            checkVersion(tenantId, branch, versionId, path);

            return repository.listFilesAtCommit(versionId, path).stream()
                    .map(filePath -> {
                        EntityId entityId = fromRelativePath(filePath);
                        EntityExportData<?> entityData = getEntityDataAtVersion(entityId, versionId);

                        VersionedEntityInfo info = new VersionedEntityInfo();
                        info.setExternalId(entityId);
                        info.setEntityName(entityData.getEntity().getName());
                        return info;
                    })
                    .collect(Collectors.toList());
        } finally {
            repositoryLock.readLock().unlock();
        }
    }


    @Override
    public EntityVersionLoadResult loadEntityVersion(SecurityUser user, EntityId externalId, String branch, String versionId, EntityVersionLoadSettings settings) throws Exception {
        return loadAtVersion(user, branch, versionId, getRelativePath(externalId.getEntityType(), externalId.getId().toString()), settings).get(0);
    }

    @Override
    public List<EntityVersionLoadResult> loadEntityTypeVersion(SecurityUser user, EntityType entityType, String branch, String versionId, EntityVersionLoadSettings settings) throws Exception {
        return loadAtVersion(user, branch, versionId, getRelativePath(entityType, null), settings);
    }

    @Override
    public List<EntityVersionLoadResult> loadAllAtVersion(SecurityUser user, String branch, String versionId, EntityVersionLoadSettings settings) throws Exception {
        return loadAtVersion(user, branch, versionId, null, settings);
    }

    private List<EntityVersionLoadResult> loadAtVersion(SecurityUser user, String branch, String versionId, String path, EntityVersionLoadSettings settings) throws Exception {
        List<EntityExportData<?>> entityDataList = new ArrayList<>();
        repositoryLock.readLock().lock();
        try {
            for (VersionedEntityInfo info : listEntitiesAtVersion(user.getTenantId(), branch, versionId, path)) {
                EntityExportData<?> entityData = getEntityDataAtVersion(info.getExternalId(), versionId);
                entityDataList.add(entityData);
            }
        } finally {
            repositoryLock.readLock().unlock();
        }

        EntityImportSettings importSettings = EntityImportSettings.builder()
                .updateRelations(settings.isLoadRelations())
                .findExistingByName(settings.isFindExistingEntityByName())
                .build();
        List<EntityImportResult<?>> importResults = exportImportService.importEntities(user, entityDataList, importSettings);

        return importResults.stream()
                .map(importResult -> EntityVersionLoadResult.builder()
                        .previousEntityVersion(importResult.getOldEntity())
                        .newEntityVersion(importResult.getSavedEntity())
                        .entityType(importResult.getEntityType())
                        .build())
                .collect(Collectors.toList());
    }

    @SneakyThrows
    private EntityExportData<?> getEntityDataAtVersion(EntityId externalId, String versionId) {
        repositoryLock.readLock().lock();
        try {
            String entityDataJson = repository.getFileContentAtCommit(getRelativePath(externalId.getEntityType(), externalId.getId().toString()), versionId);
            return JacksonUtil.fromString(entityDataJson, EntityExportData.class);
        } finally {
            repositoryLock.readLock().unlock();
        }
    }


    private void fetch() throws GitAPIException {
        repositoryLock.writeLock().lock();
        try {
            repository.fetch();
        } finally {
            repositoryLock.writeLock().unlock();
        }
    }

    private ScheduledFuture<?> scheduleFetch(int fetchPeriod) {
        return fetchExecutor.scheduleWithFixedDelay(() -> {
            if (repository == null) return;
            try {
                fetch();
            } catch (Exception e) {
                log.error("Failed to fetch remote repository", e);
            }
        }, fetchPeriod, fetchPeriod, TimeUnit.SECONDS);
    }


    private void checkVersion(TenantId tenantId, String branch, String versionId, String path) throws Exception {
        if (listVersions(tenantId, branch, path).stream().noneMatch(version -> version.getId().equals(versionId))) {
            throw new IllegalArgumentException("Version not found");
        }
    }

    @Override
    public List<String> listAllowedBranches(TenantId tenantId) {
        return Optional.ofNullable(getSettings())
                .flatMap(settings -> Optional.ofNullable(settings.getTenantsAllowedBranches()))
                .flatMap(tenantsAllowedBranches -> Optional.ofNullable(tenantsAllowedBranches.get(tenantId.getId())))
                .orElse(Collections.emptyList());
    }

    private void checkBranch(TenantId tenantId, String branch) {
        if (!listAllowedBranches(tenantId).contains(branch)) {
            throw new IllegalArgumentException("Tenant does not have access to the branch");
        }
    }


    private void checkRepository() {
        if (repository == null) {
            throw new IllegalStateException("Repository is not initialized");
        }
    }

    private void initRepository(EntitiesVersionControlSettings settings) throws Exception {
        repositoryLock.writeLock().lock();
        try {
            if (Files.exists(Path.of(settings.getRepositoryDirectory()))) {
                this.repository = GitRepository.open(settings.getRepositoryDirectory(), settings.getUsername(), settings.getPassword());
            } else {
                Files.createDirectories(Path.of(settings.getRepositoryDirectory()));
                this.repository = GitRepository.clone(settings.getRepositoryUri(), settings.getRepositoryDirectory(),
                        settings.getUsername(), settings.getPassword());
            }
        } finally {
            repositoryLock.writeLock().unlock();
        }
    }

    private void clearRepository() throws IOException {
        repositoryLock.writeLock().lock();
        try {
            if (repository != null) {
                FileUtils.deleteDirectory(new File(repository.getDirectory()));
                repository = null;
            }
        } finally {
            repositoryLock.writeLock().unlock();
        }
    }


    @SneakyThrows
    @Override
    public void saveSettings(EntitiesVersionControlSettings settings) {
        AdminSettings adminSettings = Optional.ofNullable(adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, "vc"))
                .orElseGet(() -> {
                    AdminSettings newAdminSettings = new AdminSettings();
                    newAdminSettings.setKey(SETTINGS_KEY);
                    return newAdminSettings;
                });
        adminSettings.setJsonValue(JacksonUtil.valueToTree(settings));
        adminSettingsService.saveAdminSettings(TenantId.SYS_TENANT_ID, adminSettings);

        repositoryLock.writeLock().lock();
        try {
            clearRepository();
            initRepository(settings);
        } finally {
            repositoryLock.writeLock().unlock();
        }

        if (settings.getFetchPeriod() != 0) {
            fetchTask.cancel(true);
            fetchTask = scheduleFetch(settings.getFetchPeriod());
        }
    }

    @Override
    public EntitiesVersionControlSettings getSettings() {
        return Optional.ofNullable(adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, "vc"))
                .map(adminSettings -> JacksonUtil.treeToValue(adminSettings.getJsonValue(), EntitiesVersionControlSettings.class))
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
        EntityType entityType = EntityType.valueOf(StringUtils.substringBefore(path, "/"));
        String entityId = StringUtils.substringBetween(path, "/", ".json");
        return EntityIdFactory.getByTypeAndUuid(entityType, entityId);
    }

}
