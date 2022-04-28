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
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.sync.vc.data.EntitiesVersionControlSettings;
import org.thingsboard.server.service.sync.vc.data.VersionedEntityInfo;
import org.thingsboard.server.service.sync.vc.data.EntityVersion;
import org.thingsboard.server.service.sync.vc.data.EntityVersionLoadResult;
import org.thingsboard.server.service.sync.vc.data.EntityVersionLoadSettings;
import org.thingsboard.server.service.sync.vc.data.EntityVersionSaveSettings;

import java.util.List;

public interface EntitiesVersionControlService {

    EntityVersion saveEntityVersion(SecurityUser user, EntityId entityId, String branch, String versionName, EntityVersionSaveSettings settings) throws Exception;

    EntityVersion saveEntitiesVersion(SecurityUser user, List<EntityId> entitiesIds, String branch, String versionName, EntityVersionSaveSettings settings) throws Exception;


    List<EntityVersion> listEntityVersions(TenantId tenantId, String branch, EntityId externalId) throws Exception;

    List<EntityVersion> listEntityTypeVersions(TenantId tenantId, String branch, EntityType entityType) throws Exception;

    List<EntityVersion> listVersions(TenantId tenantId, String branch) throws Exception;


    List<VersionedEntityInfo> listEntitiesAtVersion(TenantId tenantId, EntityType entityType, String branch, String versionId) throws Exception; // will be good to return entity name also

    List<VersionedEntityInfo> listAllEntitiesAtVersion(TenantId tenantId, String branch, String versionId) throws Exception;


    EntityVersionLoadResult loadEntityVersion(SecurityUser user, EntityId externalId, String branch, String versionId, EntityVersionLoadSettings settings) throws Exception;

    List<EntityVersionLoadResult> loadEntityTypeVersion(SecurityUser user, EntityType entityType, String branch, String versionId, EntityVersionLoadSettings settings) throws Exception;

    List<EntityVersionLoadResult> loadAllAtVersion(SecurityUser user, String branch, String versionId, EntityVersionLoadSettings settings) throws Exception;


    List<String> listAllowedBranches(TenantId tenantId);


    void saveSettings(EntitiesVersionControlSettings settings);

    EntitiesVersionControlSettings getSettings();

}
