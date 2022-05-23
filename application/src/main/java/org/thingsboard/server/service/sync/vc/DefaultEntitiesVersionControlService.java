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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.sync.ThrowingRunnable;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.common.data.sync.ie.EntityExportSettings;
import org.thingsboard.server.common.data.sync.ie.EntityImportResult;
import org.thingsboard.server.common.data.sync.ie.EntityImportSettings;
import org.thingsboard.server.common.data.sync.vc.EntitiesVersionControlSettings;
import org.thingsboard.server.common.data.sync.vc.EntityVersion;
import org.thingsboard.server.common.data.sync.vc.VersionControlAuthMethod;
import org.thingsboard.server.common.data.sync.vc.VersionCreationResult;
import org.thingsboard.server.common.data.sync.vc.VersionLoadResult;
import org.thingsboard.server.common.data.sync.vc.VersionedEntityInfo;
import org.thingsboard.server.common.data.sync.vc.request.create.ComplexVersionCreateRequest;
import org.thingsboard.server.common.data.sync.vc.request.create.SingleEntityVersionCreateRequest;
import org.thingsboard.server.common.data.sync.vc.request.create.SyncStrategy;
import org.thingsboard.server.common.data.sync.vc.request.create.VersionCreateConfig;
import org.thingsboard.server.common.data.sync.vc.request.create.VersionCreateRequest;
import org.thingsboard.server.common.data.sync.vc.request.load.EntityTypeVersionLoadConfig;
import org.thingsboard.server.common.data.sync.vc.request.load.EntityTypeVersionLoadRequest;
import org.thingsboard.server.common.data.sync.vc.request.load.SingleEntityVersionLoadRequest;
import org.thingsboard.server.common.data.sync.vc.request.load.VersionLoadConfig;
import org.thingsboard.server.common.data.sync.vc.request.load.VersionLoadRequest;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.sync.ie.EntitiesExportImportService;
import org.thingsboard.server.service.sync.ie.exporting.ExportableEntitiesService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@TbCoreComponent
@RequiredArgsConstructor
@Slf4j
public class DefaultEntitiesVersionControlService implements EntitiesVersionControlService {

    private final GitVersionControlService gitService;
    private final EntitiesExportImportService exportImportService;
    private final ExportableEntitiesService exportableEntitiesService;
    private final AdminSettingsService adminSettingsService;
    private final TransactionTemplate transactionTemplate;

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
                        DaoUtil.processInBatches(pageLink -> {
                            return exportableEntitiesService.findEntitiesByTenantId(user.getTenantId(), entityType, pageLink);
                        }, 100, entity -> {
                            try {
                                saveEntityData(user, commit, entity.getId(), config);
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
    public PageData<EntityVersion> listEntityVersions(TenantId tenantId, String branch, EntityId externalId, PageLink pageLink) throws Exception {
        return gitService.listVersions(tenantId, branch, externalId, pageLink);
    }

    @Override
    public PageData<EntityVersion> listEntityTypeVersions(TenantId tenantId, String branch, EntityType entityType, PageLink pageLink) throws Exception {
        return gitService.listVersions(tenantId, branch, entityType, pageLink);
    }

    @Override
    public PageData<EntityVersion> listVersions(TenantId tenantId, String branch, PageLink pageLink) throws Exception {
        return gitService.listVersions(tenantId, branch, pageLink);
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
                VersionLoadConfig config = versionLoadRequest.getConfig();
                EntityImportResult<?> importResult = transactionTemplate.execute(status -> {
                    try {
                        EntityExportData entityData = gitService.getEntity(user.getTenantId(), request.getVersionId(), versionLoadRequest.getExternalEntityId());
                        return exportImportService.importEntity(user, entityData, EntityImportSettings.builder()
                                .updateRelations(config.isLoadRelations())
                                .findExistingByName(config.isFindExistingEntityByName())
                                .build(), true, true);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
                return List.of(VersionLoadResult.builder()
                        .entityType(importResult.getEntityType())
                        .created(importResult.getOldEntity() == null ? 1 : 0)
                        .updated(importResult.getOldEntity() != null ? 1 : 0)
                        .deleted(0)
                        .build());
            }
            case ENTITY_TYPE: {
                EntityTypeVersionLoadRequest versionLoadRequest = (EntityTypeVersionLoadRequest) request;
                return transactionTemplate.execute(status -> {
                    Map<EntityType, VersionLoadResult> results = new HashMap<>();
                    Map<EntityType, Set<EntityId>> importedEntities = new HashMap<>();
                    List<ThrowingRunnable> saveReferencesCallbacks = new ArrayList<>();
                    List<ThrowingRunnable> sendEventsCallbacks = new ArrayList<>();

                    versionLoadRequest.getEntityTypes().keySet().stream()
                            .sorted(exportImportService.getEntityTypeComparatorForImport())
                            .forEach(entityType -> {
                                EntityTypeVersionLoadConfig config = versionLoadRequest.getEntityTypes().get(entityType);
                                AtomicInteger created = new AtomicInteger();
                                AtomicInteger updated = new AtomicInteger();

                                try {
                                    int limit = 100;
                                    int offset = 0;
                                    List<EntityExportData<?>> entityDataList;
                                    do {
                                        entityDataList = gitService.getEntities(user.getTenantId(), request.getBranch(), request.getVersionId(), entityType, offset, limit);
                                        for (EntityExportData entityData : entityDataList) {
                                            EntityImportResult<?> importResult = exportImportService.importEntity(user, entityData, EntityImportSettings.builder()
                                                    .updateRelations(config.isLoadRelations())
                                                    .findExistingByName(config.isFindExistingEntityByName())
                                                    .build(), false, false);

                                            if (importResult.getOldEntity() == null) created.incrementAndGet();
                                            else updated.incrementAndGet();
                                            saveReferencesCallbacks.add(importResult.getSaveReferencesCallback());
                                            sendEventsCallbacks.add(importResult.getSendEventsCallback());
                                        }
                                        offset += limit;
                                        importedEntities.computeIfAbsent(entityType, t -> new HashSet<>())
                                                .addAll(entityDataList.stream().map(entityData -> entityData.getEntity().getId()).collect(Collectors.toSet()));
                                    } while (entityDataList.size() == limit);
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                                results.put(entityType, VersionLoadResult.builder()
                                        .entityType(entityType)
                                        .created(created.get())
                                        .updated(updated.get())
                                        .build());
                            });

                    versionLoadRequest.getEntityTypes().keySet().stream()
                            .filter(entityType -> versionLoadRequest.getEntityTypes().get(entityType).isRemoveOtherEntities())
                            .sorted(exportImportService.getEntityTypeComparatorForImport().reversed())
                            .forEach(entityType -> {
                                DaoUtil.processInBatches(pageLink -> {
                                    return exportableEntitiesService.findEntitiesByTenantId(user.getTenantId(), entityType, pageLink);
                                }, 100, entity -> {
                                    if (entity.getExternalId() == null || !importedEntities.get(entityType).contains(entity.getExternalId())) {
                                        try {
                                            exportableEntitiesService.checkPermission(user, entity, entityType, Operation.DELETE);
                                        } catch (ThingsboardException e) {
                                            throw new RuntimeException(e);
                                        }
                                        exportableEntitiesService.deleteByTenantIdAndId(user.getTenantId(), entity.getId());

                                        VersionLoadResult result = results.get(entityType);
                                        result.setDeleted(result.getDeleted() + 1);
                                    }
                                });
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


    @Override
    public List<String> listBranches(TenantId tenantId) throws Exception {
        return gitService.listBranches(tenantId);
    }

    @Override
    public EntitiesVersionControlSettings getVersionControlSettings(TenantId tenantId) {
        AdminSettings adminSettings = adminSettingsService.findAdminSettingsByKey(tenantId, SETTINGS_KEY);
        if (adminSettings != null) {
            try {
                return JacksonUtil.convertValue(adminSettings.getJsonValue(), EntitiesVersionControlSettings.class);
            } catch (Exception e) {
                throw new RuntimeException("Failed to load version control settings!", e);
            }
        }
        return null;
    }

    @Override
    public EntitiesVersionControlSettings saveVersionControlSettings(TenantId tenantId, EntitiesVersionControlSettings versionControlSettings) {
        AdminSettings adminSettings = adminSettingsService.findAdminSettingsByKey(tenantId, SETTINGS_KEY);
        EntitiesVersionControlSettings storedSettings = null;
        if (adminSettings != null) {
            try {
                storedSettings = JacksonUtil.convertValue(adminSettings.getJsonValue(), EntitiesVersionControlSettings.class);
            } catch (Exception e) {
                throw new RuntimeException("Failed to load version control settings!", e);
            }
        }
        versionControlSettings = this.restoreCredentials(versionControlSettings, storedSettings);
        if (adminSettings == null) {
            adminSettings = new AdminSettings();
            adminSettings.setKey(SETTINGS_KEY);
            adminSettings.setTenantId(tenantId);
        }
        try {
            gitService.clearRepository(tenantId);
            gitService.initRepository(tenantId, versionControlSettings);
        } catch (Exception e) {
            throw new RuntimeException("Failed to init repository!", e);
        }
        adminSettings.setJsonValue(JacksonUtil.valueToTree(versionControlSettings));
        AdminSettings savedAdminSettings = adminSettingsService.saveAdminSettings(tenantId, adminSettings);
        EntitiesVersionControlSettings savedVersionControlSettings;
        try {
            savedVersionControlSettings = JacksonUtil.convertValue(savedAdminSettings.getJsonValue(), EntitiesVersionControlSettings.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load version control settings!", e);
        }
        return savedVersionControlSettings;
    }

    @Override
    public void deleteVersionControlSettings(TenantId tenantId) {
        if (adminSettingsService.deleteAdminSettings(tenantId, SETTINGS_KEY)) {
            gitService.clearRepository(tenantId);
        }
    }

    @Override
    public void checkVersionControlAccess(TenantId tenantId, EntitiesVersionControlSettings settings) throws ThingsboardException {
        EntitiesVersionControlSettings storedSettings = getVersionControlSettings(tenantId);
        settings = this.restoreCredentials(settings, storedSettings);
        try {
            gitService.testRepository(tenantId, settings);
        } catch (Exception e) {
            throw new ThingsboardException(String.format("Unable to access repository: %s", e.getMessage()),
                    ThingsboardErrorCode.GENERAL);
        }
    }

    private EntitiesVersionControlSettings restoreCredentials(EntitiesVersionControlSettings settings, EntitiesVersionControlSettings storedSettings) {
        VersionControlAuthMethod authMethod = settings.getAuthMethod();
        if (VersionControlAuthMethod.USERNAME_PASSWORD.equals(authMethod) && settings.getPassword() == null) {
            if (storedSettings != null) {
                settings.setPassword(storedSettings.getPassword());
            }
        } else if (VersionControlAuthMethod.PRIVATE_KEY.equals(authMethod) && settings.getPrivateKey() == null) {
            if (storedSettings != null) {
                settings.setPrivateKey(storedSettings.getPrivateKey());
                if (settings.getPrivateKeyPassword() == null) {
                    settings.setPrivateKeyPassword(storedSettings.getPrivateKeyPassword());
                }
            }
        }
        return settings;
    }
}
