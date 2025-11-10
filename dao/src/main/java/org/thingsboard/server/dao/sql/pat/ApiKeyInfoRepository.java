/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.dao.sql.pat;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.ApiKeyInfoEntity;

import java.util.UUID;

public interface ApiKeyInfoRepository extends JpaRepository<ApiKeyInfoEntity, UUID> {

    @Query("SELECT ak FROM ApiKeyInfoEntity ak WHERE ak.tenantId = :tenantId AND ak.userId = :userId AND " +
            "(:searchText is NULL OR ilike(ak.description, concat('%', :searchText, '%')) = true)")
    Page<ApiKeyInfoEntity> findByUserId(@Param("tenantId") UUID tenantId,
                                        @Param("userId") UUID userId,
                                        @Param("searchText") String searchText,
                                        Pageable pageable);

}
