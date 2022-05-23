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

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.common.data.sync.vc.EntitiesVersionControlSettings;
import org.thingsboard.server.common.data.sync.vc.EntityVersion;
import org.thingsboard.server.common.data.sync.vc.VersionCreationResult;
import org.thingsboard.server.common.data.sync.vc.VersionedEntityInfo;
import org.thingsboard.server.common.data.sync.vc.request.create.VersionCreateRequest;

import java.util.List;

public interface GitVersionControlService {

    void testRepository(TenantId tenantId, EntitiesVersionControlSettings settings);

    void initRepository(TenantId tenantId, EntitiesVersionControlSettings settings);

    void clearRepository(TenantId tenantId);

    PendingCommit prepareCommit(TenantId tenantId, VersionCreateRequest request);

    void addToCommit(PendingCommit commit, EntityExportData<ExportableEntity<EntityId>> entityData);

    void deleteAll(PendingCommit pendingCommit, EntityType entityType);

    VersionCreationResult push(PendingCommit commit);

    PageData<EntityVersion> listVersions(TenantId tenantId, String branch, PageLink pageLink);

    PageData<EntityVersion> listVersions(TenantId tenantId, String branch, EntityType entityType, PageLink pageLink);

    PageData<EntityVersion> listVersions(TenantId tenantId, String branch, EntityId entityId, PageLink pageLink);

    List<VersionedEntityInfo> listEntitiesAtVersion(TenantId tenantId, String branch, String versionId, EntityType entityType);

    List<VersionedEntityInfo> listEntitiesAtVersion(TenantId tenantId, String branch, String versionId);

    List<String> listBranches(TenantId tenantId);

    EntityExportData<?> getEntity(TenantId tenantId, String versionId, EntityId entityId);

    List<EntityExportData<?>> getEntities(TenantId tenantId, String branch, String versionId, EntityType entityType, int offset, int limit);

}
