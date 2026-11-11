// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.sync.vc.data;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.sync.ie.EntityImportResult;
import org.thingsboard.server.common.data.sync.ie.EntityImportSettings;
import org.thingsboard.server.common.data.sync.vc.EntityTypeLoadResult;
import org.thingsboard.server.common.data.util.ThrowingRunnable;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Data
@Slf4j
public class EntitiesImportCtx {

    private final UUID requestId;
    private final SecurityUser user;
    private final String versionId;
    private final Map<EntityType, EntityTypeLoadResult> results = new HashMap<>();
    private final Map<EntityType, Set<EntityId>> importedEntities = new HashMap<>();
    private final Map<EntityId, ReimportTask> toReimport = new HashMap<>();
    private final Map<EntityId, ThrowingRunnable> referenceCallbacks = new HashMap<>();
    private final List<ThrowingRunnable> eventCallbacks = new ArrayList<>();
    private final Map<EntityId, EntityId> externalToInternalIdMap = new HashMap<>();
    private final Set<EntityId> notFoundIds = new HashSet<>();

    private final Set<EntityRelation> relations = new LinkedHashSet<>();

    private boolean finalImportAttempt = false;
    private EntityImportSettings settings;
    private EntityImportResult<?> currentImportResult;
    private boolean rollbackOnError;

    public EntitiesImportCtx(UUID requestId, SecurityUser user, String versionId) {
        this(requestId, user, versionId, null);
    }

    public EntitiesImportCtx(UUID requestId, SecurityUser user, String versionId, EntityImportSettings settings) {
        this.requestId = requestId;
        this.user = user;
        this.versionId = versionId;
        this.settings = settings;
    }

    public TenantId getTenantId() {
        return user.getTenantId();
    }

    public boolean isFindExistingByName() {
        return getSettings().isFindExistingByName();
    }

    public boolean isUpdateRelations() {
        return getSettings().isUpdateRelations();
    }

    public boolean isSaveAttributes() {
        return getSettings().isSaveAttributes();
    }

    public boolean isSaveCredentials() {
        return getSettings().isSaveCredentials();
    }

    public boolean isSaveCalculatedFields() {
        return getSettings().isSaveCalculatedFields();
    }

    public boolean isSaveUserGroupPermissions() {
        return getSettings().isSaveUserGroupPermissions();
    }

    public boolean isAutoGenerateIntegrationKey() {
        return getSettings().isAutoGenerateIntegrationKey();
    }

    public EntityId getInternalId(EntityId externalId) {
        var result = externalToInternalIdMap.get(externalId);
        log.debug("[{}][{}] Local internal id cache {} for id", externalId.getEntityType(), externalId.getId(), result != null ? "hit" : "miss");
        return result;
    }

    public void putInternalId(EntityId externalId, EntityId internalId) {
        log.debug("[{}][{}] Local cache put: {}", externalId.getEntityType(), externalId.getId(), internalId);
        externalToInternalIdMap.put(externalId, internalId);
    }

    public void registerResult(EntityType entityType, boolean isGroup, boolean created) {
        EntityTypeLoadResult result = results.computeIfAbsent(entityType, EntityTypeLoadResult::new);
        if (isGroup) {
            if (created) {
                result.setGroupsCreated(result.getGroupsCreated() + 1);
            } else {
                result.setGroupsUpdated(result.getGroupsUpdated() + 1);
            }
        } else {
            if (created) {
                result.setCreated(result.getCreated() + 1);
            } else {
                result.setUpdated(result.getUpdated() + 1);
            }
        }
    }

    public void registerDeleted(EntityType entityType, boolean isGroup) {
        EntityTypeLoadResult result = results.computeIfAbsent(entityType, EntityTypeLoadResult::new);
        if (isGroup) {
            result.setGroupsDeleted(result.getDeleted() + 1);
        } else {
            result.setDeleted(result.getDeleted() + 1);
        }
    }

    public void addRelations(Collection<EntityRelation> values) {
        relations.addAll(values);
    }

    public void addReferenceCallback(EntityId externalId, ThrowingRunnable tr) {
        if (tr != null) {
            referenceCallbacks.put(externalId, tr);
        }
    }

    public void addEventCallback(ThrowingRunnable tr) {
        if (tr != null) {
            eventCallbacks.add(tr);
        }
    }

    public void registerNotFound(EntityId externalId) {
        notFoundIds.add(externalId);
    }

    public boolean isNotFound(EntityId externalId) {
        return notFoundIds.contains(externalId);
    }

    public boolean shouldImportEntities(EntityType entityType) {
        return entityType != EntityType.USER;
    }

}
