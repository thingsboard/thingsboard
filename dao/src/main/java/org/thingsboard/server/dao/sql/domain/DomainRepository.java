/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.dao.sql.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.dao.model.sql.DomainEntity;

import java.util.UUID;

public interface DomainRepository extends JpaRepository<DomainEntity, UUID> {

    @Query("SELECT d FROM DomainEntity d WHERE d.tenantId = :tenantId AND " +
            "(:searchText is NULL OR ilike(d.name, concat('%', :searchText, '%')) = true)")
    Page<DomainEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                      @Param("searchText") String searchText,
                                      Pageable pageable);

    @Transactional
    @Modifying
    @Query("DELETE FROM DomainEntity r WHERE r.tenantId = :tenantId")
    void deleteByTenantId(@Param("tenantId") UUID tenantId);

    int countByTenantIdAndOauth2Enabled(UUID tenantId, boolean oauth2Enabled);

}
