/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.dao.sql.entityprofile;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.dao.model.sql.EntityProfileEntity;
import org.thingsboard.server.dao.util.SqlDao;

@SqlDao
public interface EntityProfileRepository extends PagingAndSortingRepository<EntityProfileEntity, String> {

    @Query("SELECT e FROM EntityProfileEntity e WHERE e.tenantId = :tenantId " +
            "AND LOWER(e.name) LIKE LOWER(CONCAT(:textSearch, '%'))")
    Page<EntityProfileEntity> findByTenantId(@Param("tenantId") String tenantId,
                                             @Param("textSearch") String textSearch,
                                             Pageable pageable);


    @Query("SELECT e FROM EntityProfileEntity e WHERE e.tenantId = :tenantId " +
            "AND e.entityType = :type " +
            "AND LOWER(e.name) LIKE LOWER(CONCAT(:textSearch, '%'))")
    Page<EntityProfileEntity> findByTenantIdAndEntityType(@Param("tenantId") String tenantId,
                                                          @Param("type") EntityType type,
                                                          @Param("textSearch") String textSearch,
                                                          Pageable pageable);
}
