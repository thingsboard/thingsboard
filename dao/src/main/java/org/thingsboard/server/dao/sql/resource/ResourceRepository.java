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
import org.thingsboard.server.dao.model.sql.ResourceCompositeKey;
import org.thingsboard.server.dao.model.sql.ResourceEntity;

import java.util.List;
import java.util.UUID;

public interface ResourceRepository extends CrudRepository<ResourceEntity, ResourceCompositeKey> {


    Page<ResourceEntity> findAllByTenantId(UUID tenantId, Pageable pageable);

    @Query("SELECT tr FROM ResourceEntity tr " +
            "WHERE tr.resourceType = :resourceType " +
            "AND LOWER(tr.textSearch) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
            "AND (tr.tenantId = :tenantId " +
            "OR (tr.tenantId = :systemAdminId " +
            "AND NOT EXISTS " +
            "(SELECT sr FROM ResourceEntity sr " +
            "WHERE sr.tenantId = :tenantId " +
            "AND sr.resourceType = :resourceType " +
            "AND tr.resourceId = sr.resourceId)))")
    Page<ResourceEntity> findResourcesPage(
            @Param("tenantId") UUID tenantId,
            @Param("systemAdminId") UUID sysAdminId,
            @Param("resourceType") String resourceType,
            @Param("searchText") String search,
            Pageable pageable);

    List<ResourceEntity> findAllByTenantIdAndResourceType(UUID tenantId, String resourceType);

    void removeAllByTenantId(UUID tenantId);

    @Query("SELECT tr FROM ResourceEntity tr " +
            "WHERE tr.resourceType = :resourceType " +
            "AND LOWER(tr.textSearch) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
            "AND (tr.tenantId = :tenantId " +
            "OR (tr.tenantId = :systemAdminId " +
            "AND NOT EXISTS " +
            "(SELECT sr FROM ResourceEntity sr " +
            "WHERE sr.tenantId = :tenantId " +
            "AND sr.resourceType = :resourceType " +
            "AND tr.resourceId = sr.resourceId)))")
    List<ResourceEntity> findResources(@Param("tenantId") UUID tenantId,
                                       @Param("systemAdminId") UUID sysAdminId,
                                       @Param("resourceType") String resourceType,
                                       @Param("searchText") String search);

    @Query("SELECT tr FROM ResourceEntity tr " +
            "WHERE tr.resourceType = :resourceType " +
            "AND tr.resourceId in (:resourceIds) " +
            "AND (tr.tenantId = :tenantId " +
            "OR (tr.tenantId = :systemAdminId " +
            "AND NOT EXISTS " +
            "(SELECT sr FROM ResourceEntity sr " +
            "WHERE sr.tenantId = :tenantId " +
            "AND sr.resourceType = :resourceType " +
            "AND tr.resourceId = sr.resourceId)))")
    List<ResourceEntity> findResourcesByIds(@Param("tenantId") UUID tenantId,
                                            @Param("systemAdminId") UUID sysAdminId,
                                            @Param("resourceType") String resourceType,
                                            @Param("resourceIds") String[] objectIds);
}
