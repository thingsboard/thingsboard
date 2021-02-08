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
package org.thingsboard.server.dao.sql.usagerecord;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.dao.model.sql.ApiUsageStateEntity;

import java.util.UUID;

/**
 * @author Valerii Sosliuk
 */
public interface ApiUsageStateRepository extends CrudRepository<ApiUsageStateEntity, UUID> {

    @Query("SELECT ur FROM ApiUsageStateEntity ur WHERE ur.tenantId = :tenantId " +
            "AND ur.entityId = :tenantId AND ur.entityType = 'TENANT' ")
    ApiUsageStateEntity findByTenantId(@Param("tenantId") UUID tenantId);

    @Transactional
    @Modifying
    @Query("DELETE FROM ApiUsageStateEntity ur WHERE ur.tenantId = :tenantId")
    void deleteApiUsageStateByTenantId(@Param("tenantId") UUID tenantId);
}
