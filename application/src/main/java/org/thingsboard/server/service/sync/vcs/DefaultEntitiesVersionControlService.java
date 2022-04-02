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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
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
    private final ReentrantLock fetchLock = new ReentrantLock();
    private final Lock writeLock = new ReentrantLock();

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
    public void fetch() throws Exception {
        if (repository == null) return;

        if (fetchLock.tryLock()) {
            try {
                log.info("Fetching remote repository");
                repository.fetch();
            } finally {
                fetchLock.unlock();
            }
        }
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
                .map(entityId -> {
                    return exportImportService.exportEntity(tenantId, entityId, exportSettings);
                })
                .collect(Collectors.toList());

        if (fetchLock.tryLock()) {
            try {
                repository.fetch();
            } finally {
                fetchLock.unlock();
            }
        }

        writeLock.lock();
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
            return new EntityVersion(commit.getId(), commit.getMessage(), commit.getAuthorName());
        } finally {
            writeLock.unlock();
        }
    }



    @Override
    public List<EntityVersion> listEntityVersions(TenantId tenantId, EntityId entityId, String branch, int limit) throws Exception {
        checkRepository();
        checkBranch(tenantId, branch);

        return repository.listCommits(branch, getRelativePathForEntity(entityId), limit).stream()
                .map(commit -> new EntityVersion(commit.getId(), commit.getMessage(), commit.getAuthorName()))
                .collect(Collectors.toList());
    }

    @Override
    public List<EntityVersion> listEntityTypeVersions(TenantId tenantId, EntityType entityType, String branch, int limit) throws Exception {
        checkRepository();
        checkBranch(tenantId, branch);

        return repository.listCommits(branch, getRelativePathForEntityType(entityType), limit).stream()
                .map(commit -> new EntityVersion(commit.getId(), commit.getMessage(), commit.getAuthorName()))
                .collect(Collectors.toList());
    }



    @Override
    public <E extends ExportableEntity<I>, I extends EntityId> EntityExportData<E> getEntityAtVersion(TenantId tenantId, I entityId, String versionId) throws Exception {
        checkRepository();
        // FIXME [viacheslav]: validate access

        String entityDataJson = repository.getFileContentAtCommit(getRelativePathForEntity(entityId), versionId);
        return JacksonUtil.fromString(entityDataJson, new TypeReference<EntityExportData<E>>() {});
    }

    @Override
    public <E extends ExportableEntity<I>, I extends EntityId> EntityImportResult<E> loadEntityVersion(TenantId tenantId, I entityId, String versionId) throws Exception {
        EntityExportData<E> entityData = getEntityAtVersion(tenantId, entityId, versionId);
        return exportImportService.importEntity(tenantId, entityData, EntityImportSettings.builder()
                .importInboundRelations(false)
                .importOutboundRelations(false)
                .updateReferencesToOtherEntities(true)
                .build());
    }



    private String getRelativePathForEntity(EntityId entityId) {
        return getRelativePathForEntityType(entityId.getEntityType())
                + "/" + entityId.getId() + ".json";
    }

    private String getRelativePathForEntityType(EntityType entityType) {
        return entityType.name().toLowerCase();
    }


    private void checkBranch(TenantId tenantId, String branch) {
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

    @Override
    public void saveSettings(EntitiesVersionControlSettings settings) throws Exception {
        this.repository = initRepository(settings.getGitSettings());

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
        if (this.repository != null) {
            FileUtils.deleteDirectory(new File(repository.getDirectory()));
            this.repository = null;
        }
        EntitiesVersionControlSettings settings = getSettings();
        this.repository = initRepository(settings.getGitSettings());
    }

}
