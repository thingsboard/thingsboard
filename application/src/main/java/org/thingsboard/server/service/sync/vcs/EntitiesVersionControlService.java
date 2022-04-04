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

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.service.sync.exporting.data.EntityExportData;
import org.thingsboard.server.service.sync.importing.EntityImportResult;
import org.thingsboard.server.service.sync.vcs.data.EntitiesVersionControlSettings;
import org.thingsboard.server.service.sync.vcs.data.EntityVersion;

import java.util.List;

public interface EntitiesVersionControlService {

    EntityVersion saveEntityVersion(TenantId tenantId, EntityId entityId, String branch, String versionName) throws Exception;

    EntityVersion saveEntitiesVersion(TenantId tenantId, List<EntityId> entitiesIds, String branch, String versionName) throws Exception;


    List<EntityVersion> listEntityVersions(TenantId tenantId, EntityId entityId, String branch, int limit) throws Exception;

    List<EntityVersion> listEntityTypeVersions(TenantId tenantId, EntityType entityType, String branch, int limit) throws Exception;

    List<EntityVersion> listVersions(TenantId tenantId, String branch, int limit) throws Exception;


    List<String> listFilesAtVersion(TenantId tenantId, String branch, String versionId) throws Exception;


    <E extends ExportableEntity<I>, I extends EntityId> EntityExportData<E> getEntityAtVersion(TenantId tenantId, I entityId, String branch, String versionId) throws Exception;

    <E extends ExportableEntity<I>, I extends EntityId> EntityImportResult<E> loadEntityVersion(TenantId tenantId, I entityId, String branch, String versionId) throws Exception;

    List<EntityImportResult<ExportableEntity<EntityId>>> loadAllAtVersion(TenantId tenantId, String branch, String versionId) throws Exception;


    void saveSettings(EntitiesVersionControlSettings settings) throws Exception;

    EntitiesVersionControlSettings getSettings();

}
