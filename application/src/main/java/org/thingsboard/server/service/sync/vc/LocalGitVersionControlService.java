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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.common.data.sync.vc.EntitiesVersionControlSettings;
import org.thingsboard.server.common.data.sync.vc.EntityVersion;
import org.thingsboard.server.common.data.sync.vc.VersionCreationResult;
import org.thingsboard.server.common.data.sync.vc.VersionedEntityInfo;
import org.thingsboard.server.common.data.sync.vc.request.create.VersionCreateRequest;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.dao.tenant.TenantDao;
import org.thingsboard.server.queue.util.AfterStartUp;

import java.io.IOException;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
@ConditionalOnProperty(prefix = "vc", value = "git.service", havingValue = "local", matchIfMissing = true)
public class LocalGitVersionControlService implements GitVersionControlService {

    private final ObjectWriter jsonWriter = new ObjectMapper().writer(SerializationFeature.INDENT_OUTPUT);
    private final GitRepositoryService gitRepositoryService;
    private final TenantDao tenantDao;
    private final AdminSettingsService adminSettingsService;
    private final ConcurrentMap<TenantId, Lock> tenantRepoLocks = new ConcurrentHashMap<>();
    private final Map<TenantId, PendingCommit> pendingCommitMap = new HashMap<>();

    @AfterStartUp
    public void init() {
        DaoUtil.processInBatches(tenantDao::findTenantsIds, 100, tenantId -> {
            EntitiesVersionControlSettings settings = getVersionControlSettings(tenantId);
            if (settings != null) {
                try {
                    gitRepositoryService.initRepository(tenantId, settings);
                } catch (Exception e) {
                    log.warn("Failed to init repository for tenant {}", tenantId, e);
                }
            }
        });
    }

    @Override
    public void testRepository(TenantId tenantId, EntitiesVersionControlSettings settings) {
        var lock = getRepoLock(tenantId);
        lock.lock();
        try {
            gitRepositoryService.testRepository(tenantId, settings);
        } catch (Exception e) {
            //TODO: analyze and return meaningful exceptions that we can show to the client;
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void initRepository(TenantId tenantId, EntitiesVersionControlSettings settings) {
        var lock = getRepoLock(tenantId);
        lock.lock();
        try {
            gitRepositoryService.initRepository(tenantId, settings);
        } catch (Exception e) {
            //TODO: analyze and return meaningful exceptions that we can show to the client;
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public PendingCommit prepareCommit(TenantId tenantId, VersionCreateRequest request) {
        var lock = getRepoLock(tenantId);
        lock.lock();
        try {
            var pendingCommit = new PendingCommit(tenantId, request);
            PendingCommit old = pendingCommitMap.put(tenantId, pendingCommit);
            if (old != null) {
                gitRepositoryService.abort(old);
            }
            gitRepositoryService.prepareCommit(pendingCommit);
            return pendingCommit;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void deleteAll(PendingCommit commit, EntityType entityType) {
        doInsideLock(commit, c -> {
            try {
                gitRepositoryService.deleteFolderContent(commit, getRelativePath(entityType, null));
            } catch (IOException e) {
                //TODO: analyze and return meaningful exceptions that we can show to the client;
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void addToCommit(PendingCommit commit, EntityExportData<ExportableEntity<EntityId>> entityData) {
        doInsideLock(commit, c -> {
            String entityDataJson;
            try {
                entityDataJson = jsonWriter.writeValueAsString(entityData);
                gitRepositoryService.add(c, getRelativePath(entityData.getEntityType(),
                        entityData.getEntity().getId().toString()), entityDataJson);
            } catch (IOException e) {
                //TODO: analyze and return meaningful exceptions that we can show to the client;
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public VersionCreationResult push(PendingCommit commit) {
        return executeInsideLock(commit, gitRepositoryService::push);
    }

    @Override
    public List<EntityVersion> listVersions(TenantId tenantId, String branch) {
        return listVersions(tenantId, branch, (String) null);
    }

    @Override
    public List<EntityVersion> listVersions(TenantId tenantId, String branch, EntityType entityType) {
        return listVersions(tenantId, branch, getRelativePath(entityType, null));
    }

    @Override
    public List<EntityVersion> listVersions(TenantId tenantId, String branch, EntityId entityId) {
        return listVersions(tenantId, branch, getRelativePath(entityId.getEntityType(), entityId.getId().toString()));
    }

    @Override
    public List<VersionedEntityInfo> listEntitiesAtVersion(TenantId tenantId, String branch, String versionId, EntityType entityType) {
        try {
            return gitRepositoryService.listEntitiesAtVersion(tenantId, branch, versionId, entityType != null ? getRelativePath(entityType, null) : null);
        } catch (Exception e) {
            //TODO: analyze and return meaningful exceptions that we can show to the client;
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<VersionedEntityInfo> listEntitiesAtVersion(TenantId tenantId, String branch, String versionId) {
        return listEntitiesAtVersion(tenantId, branch, versionId, null);
    }

    @Override
    public List<String> listBranches(TenantId tenantId) {
        return gitRepositoryService.listBranches(tenantId);
    }

    @Override
    public List<EntityExportData<?>> getEntities(TenantId tenantId, String branch, String versionId, EntityType entityType, int offset, int limit) {
        return listEntitiesAtVersion(tenantId, branch, versionId, entityType).stream()
                .skip(offset).limit(limit)
                .map(entityInfo -> getEntity(tenantId, versionId, entityInfo.getExternalId()))
                .collect(Collectors.toList());
    }

    @Override
    public EntityExportData<?> getEntity(TenantId tenantId, String versionId, EntityId entityId) {
        try {
            String entityDataJson = gitRepositoryService.getFileContentAtCommit(tenantId,
                    getRelativePath(entityId.getEntityType(), entityId.getId().toString()), versionId);
            return JacksonUtil.fromString(entityDataJson, EntityExportData.class);
        } catch (Exception e) {
            //TODO: analyze and return meaningful exceptions that we can show to the client;
            throw new RuntimeException(e);
        }
    }

    private EntitiesVersionControlSettings getVersionControlSettings(TenantId tenantId) {
        AdminSettings adminSettings = adminSettingsService.findAdminSettingsByKey(tenantId, EntitiesVersionControlService.SETTINGS_KEY);
        if (adminSettings != null) {
            try {
                return JacksonUtil.convertValue(adminSettings.getJsonValue(), EntitiesVersionControlSettings.class);
            } catch (Exception e) {
                throw new RuntimeException("Failed to load version control settings!", e);
            }
        }
        return null;
    }

    private List<EntityVersion> listVersions(TenantId tenantId, String branch, String path) {
        try {
            return gitRepositoryService.listVersions(tenantId, branch, path);
        } catch (Exception e) {
            //TODO: analyze and return meaningful exceptions that we can show to the client;
            throw new RuntimeException(e);
        }
    }

    private void doInsideLock(PendingCommit commit, Consumer<PendingCommit> r) {
        var lock = getRepoLock(commit.getTenantId());
        lock.lock();
        try {
            checkCommit(commit);
            r.accept(commit);
        } finally {
            lock.unlock();
        }
    }

    private <T> T executeInsideLock(PendingCommit commit, Function<PendingCommit, T> c) {
        var lock = getRepoLock(commit.getTenantId());
        lock.lock();
        try {
            checkCommit(commit);
            return c.apply(commit);
        } finally {
            lock.unlock();
        }
    }

    private void checkCommit(PendingCommit commit) {
        PendingCommit existing = pendingCommitMap.get(commit.getTenantId());
        if (existing == null || !existing.getTxId().equals(commit.getTxId())) {
            throw new ConcurrentModificationException();
        }
    }

    private String getRelativePath(EntityType entityType, String entityId) {
        String path = entityType.name().toLowerCase();
        if (entityId != null) {
            path += "/" + entityId + ".json";
        }
        return path;
    }

    private Lock getRepoLock(TenantId tenantId) {
        return tenantRepoLocks.computeIfAbsent(tenantId, t -> new ReentrantLock());
    }

}
