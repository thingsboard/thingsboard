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
package org.thingsboard.server.service.sync.exporting;

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;

public interface ExportableEntitiesService {

    <E extends ExportableEntity<I>, I extends EntityId> E findEntityByTenantIdAndExternalId(TenantId tenantId, I externalId);

    <E extends HasId<I>, I extends EntityId> E findEntityByTenantIdAndId(TenantId tenantId, I id);

    <E extends ExportableEntity<I>, I extends EntityId> E findEntityByTenantIdAndName(TenantId tenantId, EntityType entityType, String name);


    void checkPermission(SecurityUser user, HasId<? extends EntityId> entity, EntityType entityType, Operation operation) throws ThingsboardException;

    void checkPermission(SecurityUser user, EntityId entityId, Operation operation) throws ThingsboardException;

}
