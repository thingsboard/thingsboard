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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.TbResourceEntity;

import java.util.List;
import java.util.UUID;

public interface TbResourceRepository extends CrudRepository<TbResourceEntity, UUID> {

    TbResourceEntity findByTenantIdAndResourceTypeAndResourceKey(UUID tenantId, String resourceType, String resourceKey);

    Page<TbResourceEntity> findAllByTenantId(UUID tenantId, Pageable pageable);

    @Query("SELECT tr FROM TbResourceEntity tr " +
            "WHERE tr.resourceType = :resourceType " +
            "AND LOWER(tr.searchText) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
            "AND (tr.tenantId = :tenantId " +
            "OR (tr.tenantId = :systemAdminId " +
            "AND NOT EXISTS " +
            "(SELECT sr FROM TbResourceEntity sr " +
            "WHERE sr.tenantId = :tenantId " +
            "AND sr.resourceType = :resourceType " +
            "AND tr.resourceKey = sr.resourceKey)))")
    Page<TbResourceEntity> findResourcesPage(
            @Param("tenantId") UUID tenantId,
            @Param("systemAdminId") UUID sysAdminId,
            @Param("resourceType") String resourceType,
            @Param("searchText") String search,
            Pageable pageable);

    void removeAllByTenantId(UUID tenantId);

    @Query("SELECT tr FROM TbResourceEntity tr " +
            "WHERE tr.resourceType = :resourceType " +
            "AND LOWER(tr.searchText) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
            "AND (tr.tenantId = :tenantId " +
            "OR (tr.tenantId = :systemAdminId " +
            "AND NOT EXISTS " +
            "(SELECT sr FROM TbResourceEntity sr " +
            "WHERE sr.tenantId = :tenantId " +
            "AND sr.resourceType = :resourceType " +
            "AND tr.resourceKey = sr.resourceKey)))")
    List<TbResourceEntity> findResources(@Param("tenantId") UUID tenantId,
                                         @Param("systemAdminId") UUID sysAdminId,
                                         @Param("resourceType") String resourceType,
                                         @Param("searchText") String search);

    @Query("SELECT tr FROM TbResourceEntity tr " +
            "WHERE tr.resourceType = :resourceType " +
            "AND tr.resourceKey in (:resourceIds) " +
            "AND (tr.tenantId = :tenantId " +
            "OR (tr.tenantId = :systemAdminId " +
            "AND NOT EXISTS " +
            "(SELECT sr FROM TbResourceEntity sr " +
            "WHERE sr.tenantId = :tenantId " +
            "AND sr.resourceType = :resourceType " +
            "AND tr.resourceKey = sr.resourceKey)))")
    List<TbResourceEntity> findResourcesByIds(@Param("tenantId") UUID tenantId,
                                              @Param("systemAdminId") UUID sysAdminId,
                                              @Param("resourceType") String resourceType,
                                              @Param("resourceIds") String[] objectIds);
}
