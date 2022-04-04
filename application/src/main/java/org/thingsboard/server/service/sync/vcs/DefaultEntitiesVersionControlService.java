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
package org.thingsboard.server.service.sync.vcs;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.queue.util.AfterStartUp;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.EntitiesExportImportService;
import org.thingsboard.server.service.sync.exporting.EntityExportSettings;
import org.thingsboard.server.service.sync.exporting.data.EntityExportData;
import org.thingsboard.server.service.sync.importing.EntityImportResult;
import org.thingsboard.server.service.sync.importing.EntityImportSettings;
import org.thingsboard.server.service.sync.vcs.data.EntitiesVersionControlSettings;
import org.thingsboard.server.service.sync.vcs.data.EntityVersion;
import org.thingsboard.server.service.sync.vcs.data.GitSettings;
import org.thingsboard.server.utils.git.Repository;
import org.thingsboard.server.utils.git.data.Commit;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

@Service
@TbCoreComponent
@RequiredArgsConstructor
@Slf4j
public class DefaultEntitiesVersionControlService implements EntitiesVersionControlService {
    // TODO [viacheslav]: start up only on one of the cores

    private final TenantService tenantService;
    private final EntitiesExportImportService exportImportService;
    private final AdminSettingsService adminSettingsService;

    private final ObjectWriter jsonWriter = new ObjectMapper().writer(SerializationFeature.INDENT_OUTPUT);
    private static final String SETTINGS_KEY = "vc";

    private Repository repository;
    private final Lock fetchLock = new ReentrantLock();
    private final ReadWriteLock repositoryLock = new ReentrantReadWriteLock();

    @AfterStartUp
    public void init() throws Exception {
        try {
            EntitiesVersionControlSettings settings = getSettings();
            if (settings != null && settings.getGitSettings() != null) {
                this.repository = initRepository(settings.getGitSettings());
            }
        } catch (Exception e) {
            log.error("Failed to initialize entities version control service", e);
        }
    }


    @Scheduled(initialDelay = 10 * 1000, fixedDelay = 10 * 1000)
    private void fetch() throws Exception {
        if (repository == null) return;
        tryFetch();
    }


    @Override
    public EntityVersion saveEntityVersion(TenantId tenantId, EntityId entityId, String branch, String versionName) throws Exception {
        return saveEntitiesVersion(tenantId, List.of(entityId), branch, versionName);
    }

    @Override
    public EntityVersion saveEntitiesVersion(TenantId tenantId, List<EntityId> entitiesIds, String branch, String versionName) throws Exception {
        checkRepository();
        checkBranch(tenantId, branch);

        EntityExportSettings exportSettings = EntityExportSettings.builder()
                .exportInboundRelations(false)
                .exportOutboundRelations(false)
                .build();
        List<EntityExportData<ExportableEntity<EntityId>>> entityDataList = entitiesIds.stream()
                .map(entityId -> exportImportService.exportEntity(tenantId, entityId, exportSettings))
                .collect(Collectors.toList());

        tryFetch();

        repositoryLock.writeLock().lock();
        try {
            if (repository.listBranches().contains(branch)) {
                repository.checkout(branch);
                repository.merge(branch);
            } else {
                repository.createAndCheckoutOrphanBranch(branch);
            }

            for (EntityExportData<ExportableEntity<EntityId>> entityData : entityDataList) {
                String entityDataJson = jsonWriter.writeValueAsString(entityData);
                FileUtils.write(new File(repository.getDirectory() + "/" + getRelativePathForEntity(entityData.getEntity().getId())),
                        entityDataJson, StandardCharsets.UTF_8);
            }

            Commit commit = repository.commit(versionName, ".", "Tenant " + tenantId);
            repository.push();
            return toVersion(commit);
        } finally {
            repositoryLock.writeLock().unlock();
        }
    }



    @Override
    public List<EntityVersion> listEntityVersions(TenantId tenantId, EntityId entityId, String branch, int limit) throws Exception {
        return listVersions(tenantId, branch, getRelativePathForEntity(entityId), limit);
    }

    @Override
    public List<EntityVersion> listEntityTypeVersions(TenantId tenantId, EntityType entityType, String branch, int limit) throws Exception {
        return listVersions(tenantId, getRelativePathForEntityType(entityType), limit);
    }

    @Override
    public List<EntityVersion> listVersions(TenantId tenantId, String branch, int limit) throws Exception {
        return listVersions(tenantId, branch, null, limit);
    }

    private List<EntityVersion> listVersions(TenantId tenantId, String branch, String path, int limit) throws Exception {
        repositoryLock.readLock().lock();
        try {
            checkRepository();
            checkBranch(tenantId, branch);

            return repository.listCommits(branch, path, limit).stream()
                    .map(this::toVersion)
                    .collect(Collectors.toList());

        } finally {
            repositoryLock.readLock().unlock();
        }
    }



    @Override
    public List<String> listFilesAtVersion(TenantId tenantId, String branch, String versionId) throws Exception {
        repositoryLock.readLock().lock();
        try {
            if (listVersions(tenantId, branch, Integer.MAX_VALUE).stream()
                    .noneMatch(version -> version.getId().equals(versionId))) {
                throw new IllegalArgumentException("Unknown version");
            }
            return repository.listFilesAtCommit(versionId);
        } finally {
            repositoryLock.readLock().unlock();
        }
    }



    @Override
    public <E extends ExportableEntity<I>, I extends EntityId> EntityExportData<E> getEntityAtVersion(TenantId tenantId, I entityId, String branch, String versionId) throws Exception {
        repositoryLock.readLock().lock();
        try {
            if (listEntityVersions(tenantId, entityId, branch, Integer.MAX_VALUE).stream()
                    .noneMatch(version -> version.getId().equals(versionId))) {
                throw new IllegalArgumentException("Unknown version");
            }

            String entityDataJson = repository.getFileContentAtCommit(getRelativePathForEntity(entityId), versionId);
            return parseEntityData(entityDataJson);
        } finally {
            repositoryLock.readLock().unlock();
        }
    }


    @Override
    public <E extends ExportableEntity<I>, I extends EntityId> EntityImportResult<E> loadEntityVersion(TenantId tenantId, I entityId, String branch, String versionId) throws Exception {
        EntityExportData<E> entityData = getEntityAtVersion(tenantId, entityId, branch, versionId);
        return exportImportService.importEntity(tenantId, entityData, EntityImportSettings.builder()
                .importInboundRelations(false)
                .importOutboundRelations(false)
                .updateReferencesToOtherEntities(true)
                .build());
    }

    @Override
    public List<EntityImportResult<ExportableEntity<EntityId>>> loadAllAtVersion(TenantId tenantId, String branch, String versionId) throws Exception {
        repositoryLock.readLock().lock();
        try {
            List<EntityExportData<ExportableEntity<EntityId>>> entityDataList = listFilesAtVersion(tenantId, branch, versionId).stream()
                    .map(entityDataFilePath -> {
                        String entityDataJson;
                        try {
                            entityDataJson = repository.getFileContentAtCommit(entityDataFilePath, versionId);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                        return parseEntityData(entityDataJson);
                    })
                    .collect(Collectors.toList());

            return exportImportService.importEntities(tenantId, entityDataList, EntityImportSettings.builder()
                    .importInboundRelations(false)
                    .importOutboundRelations(false)
                    .updateReferencesToOtherEntities(true)
                    .build());
        } finally {
            repositoryLock.readLock().unlock();
        }
    }

    private void tryFetch() throws GitAPIException {
        repositoryLock.readLock().lock();
        try {
            if (fetchLock.tryLock()) {
                try {
                    log.info("Fetching remote repository");
                    repository.fetch();
                } finally {
                    fetchLock.unlock();
                }
            }
        } finally {
            repositoryLock.readLock().unlock();
        }
    }


    private String getRelativePathForEntity(EntityId entityId) {
        return getRelativePathForEntityType(entityId.getEntityType())
                + "/" + entityId.getId() + ".json";
    }

    private String getRelativePathForEntityType(EntityType entityType) {
        return entityType.name().toLowerCase();
    }


    private void checkBranch(TenantId tenantId, String branch) {
        // TODO [viacheslav]: all branches are available by default?
        if (!getAllowedBranches(tenantId).contains(branch)) {
            throw new IllegalArgumentException("Tenant does not have access to this branch");
        }
    }

    public Set<String> getAllowedBranches(TenantId tenantId) {
        return Optional.ofNullable(getSettings())
                .flatMap(settings -> Optional.ofNullable(settings.getAllowedBranches()))
                .flatMap(tenantsAllowedBranches -> Optional.ofNullable(tenantsAllowedBranches.get(tenantId.getId())))
                .orElse(Collections.emptySet());
    }

    private EntityVersion toVersion(Commit commit) {
        return new EntityVersion(commit.getId(), commit.getMessage(), commit.getAuthorName());
    }

    private <E extends ExportableEntity<I>, I extends EntityId> EntityExportData<E> parseEntityData(String entityDataJson) {
        return JacksonUtil.fromString(entityDataJson, new TypeReference<EntityExportData<E>>() {});
    }



    @Override
    public void saveSettings(EntitiesVersionControlSettings settings) throws Exception {
        repositoryLock.writeLock().lock();
        try {
            this.repository = initRepository(settings.getGitSettings());
        } finally {
            repositoryLock.writeLock().unlock();
        }

        AdminSettings adminSettings = Optional.ofNullable(adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, SETTINGS_KEY))
                .orElseGet(() -> {
                    AdminSettings newSettings = new AdminSettings();
                    newSettings.setKey(SETTINGS_KEY);
                    return newSettings;
                });
        adminSettings.setJsonValue(JacksonUtil.valueToTree(settings));
        adminSettingsService.saveAdminSettings(TenantId.SYS_TENANT_ID, adminSettings);
    }

    @Override
    public EntitiesVersionControlSettings getSettings() {
        return Optional.ofNullable(adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, SETTINGS_KEY))
                .map(adminSettings -> JacksonUtil.treeToValue(adminSettings.getJsonValue(), EntitiesVersionControlSettings.class))
                .orElse(null);
    }


    private void checkRepository() {
        if (repository == null) {
            throw new IllegalStateException("Repository is not initialized");
        }
    }

    private static Repository initRepository(GitSettings gitSettings) throws Exception {
        if (Files.exists(Path.of(gitSettings.getRepositoryDirectory()))) {
            return Repository.open(gitSettings.getRepositoryDirectory(),
                    gitSettings.getUsername(), gitSettings.getPassword());
        } else {
            Files.createDirectories(Path.of(gitSettings.getRepositoryDirectory()));
            return Repository.clone(gitSettings.getRepositoryUri(), gitSettings.getRepositoryDirectory(),
                    gitSettings.getUsername(), gitSettings.getPassword());
        }
    }

    public void resetRepository() throws Exception {
        repositoryLock.writeLock().lock();
        try {
            if (this.repository != null) {
                FileUtils.deleteDirectory(new File(repository.getDirectory()));
                this.repository = null;
            }
            EntitiesVersionControlSettings settings = getSettings();
            this.repository = initRepository(settings.getGitSettings());
        } finally {
            repositoryLock.writeLock().unlock();
        }
    }

}
