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
package org.thingsboard.server.service.sync.vc.data;

import lombok.Data;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.sync.ie.EntityImportSettings;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.HashMap;
import java.util.Map;

@Data
public class EntitiesImportCtx {

    private final SecurityUser user;
    private EntityImportSettings settings;

    private final Map<EntityId, EntityId> externalToInternalIdMap = new HashMap<>();

    public EntitiesImportCtx(SecurityUser user) {
        this(user, null);
    }

    public EntitiesImportCtx(SecurityUser user, EntityImportSettings settings) {
        this.user = user;
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

    public boolean isResetExternalIdsOfAnotherTenant() {
        return getSettings().isResetExternalIdsOfAnotherTenant();
    }

    public EntityId getInternalId(EntityId externalId) {
        return externalToInternalIdMap.get(externalId);
    }

    public void putInternalId(EntityId externalId, EntityId internalId) {
        externalToInternalIdMap.put(externalId, internalId);
    }
}
