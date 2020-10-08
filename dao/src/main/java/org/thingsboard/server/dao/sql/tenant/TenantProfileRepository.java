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
package org.thingsboard.server.dao.sql.tenant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.dao.model.sql.TenantProfileEntity;

import java.util.UUID;

public interface TenantProfileRepository extends PagingAndSortingRepository<TenantProfileEntity, UUID> {

    @Query("SELECT new org.thingsboard.server.common.data.EntityInfo(t.id, 'TENANT_PROFILE', t.name) " +
            "FROM TenantProfileEntity t " +
            "WHERE t.id = :tenantProfileId")
    EntityInfo findTenantProfileInfoById(@Param("tenantProfileId") UUID tenantProfileId);

    @Query("SELECT t FROM TenantProfileEntity t WHERE " +
            "LOWER(t.searchText) LIKE LOWER(CONCAT(:textSearch, '%'))")
    Page<TenantProfileEntity> findTenantProfiles(@Param("textSearch") String textSearch,
                                                 Pageable pageable);

    @Query("SELECT new org.thingsboard.server.common.data.EntityInfo(t.id, 'TENANT_PROFILE', t.name) " +
            "FROM TenantProfileEntity t " +
            "WHERE LOWER(t.searchText) LIKE LOWER(CONCAT(:textSearch, '%'))")
    Page<EntityInfo> findTenantProfileInfos(@Param("textSearch") String textSearch,
                                            Pageable pageable);

    @Query("SELECT t FROM TenantProfileEntity t " +
            "WHERE t.isDefault = true")
    TenantProfileEntity findByDefaultTrue();

    @Query("SELECT new org.thingsboard.server.common.data.EntityInfo(t.id, 'TENANT_PROFILE', t.name) " +
            "FROM TenantProfileEntity t " +
            "WHERE t.isDefault = true")
    EntityInfo findDefaultTenantProfileInfo();
}
