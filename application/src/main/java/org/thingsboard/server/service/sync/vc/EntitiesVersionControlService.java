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
import org.thingsboard.server.common.data.sync.vc.EntitiesVersionControlSettings;
import org.thingsboard.server.common.data.sync.vc.EntityVersion;
import org.thingsboard.server.common.data.sync.vc.VersionCreationResult;
import org.thingsboard.server.common.data.sync.vc.VersionLoadResult;
import org.thingsboard.server.common.data.sync.vc.VersionedEntityInfo;
import org.thingsboard.server.common.data.sync.vc.request.load.VersionLoadRequest;
import org.thingsboard.server.common.data.sync.vc.request.create.VersionCreateRequest;

import java.util.List;

public interface EntitiesVersionControlService {

    VersionCreationResult saveEntitiesVersion(SecurityUser user, VersionCreateRequest request) throws Exception;


    List<EntityVersion> listEntityVersions(TenantId tenantId, String branch, EntityId externalId) throws Exception;

    List<EntityVersion> listEntityTypeVersions(TenantId tenantId, String branch, EntityType entityType) throws Exception;

    List<EntityVersion> listVersions(TenantId tenantId, String branch) throws Exception;


    List<VersionedEntityInfo> listEntitiesAtVersion(TenantId tenantId, EntityType entityType, String branch, String versionId) throws Exception;

    List<VersionedEntityInfo> listAllEntitiesAtVersion(TenantId tenantId, String branch, String versionId) throws Exception;


    List<VersionLoadResult> loadEntitiesVersion(SecurityUser user, VersionLoadRequest request) throws Exception;


    List<String> listBranches(TenantId tenantId) throws Exception;


    void saveSettings(TenantId tenantId, EntitiesVersionControlSettings settings);

    EntitiesVersionControlSettings getSettings(TenantId tenantId);

}
