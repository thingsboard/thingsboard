/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.dao.sql.resource;

import org.springframework.data.repository.CrudRepository;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.sql.ResourceCompositeKey;
import org.thingsboard.server.dao.model.sql.ResourceEntity;

import java.util.List;
import java.util.UUID;

public interface ResourceRepository extends CrudRepository<ResourceEntity, ResourceCompositeKey> {

    List<ResourceEntity> findAllByTenantId(UUID tenantId);

    void removeAllByTenantId(UUID tenantId);
}
