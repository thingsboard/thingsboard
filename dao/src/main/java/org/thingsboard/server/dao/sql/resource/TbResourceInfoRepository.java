// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.sql.resource;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.TbResourceInfoEntity;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface TbResourceInfoRepository extends JpaRepository<TbResourceInfoEntity, UUID> {

    @Query("SELECT tr FROM TbResourceInfoEntity tr WHERE " +
            "(:searchText IS NULL OR ilike(tr.title, CONCAT('%', :searchText, '%')) = true) " +
            "AND ((tr.tenantId = :tenantId AND (tr.customerId IS NULL OR tr.customerId = org.thingsboard.server.common.data.id.EntityId.NULL_UUID))" +
            "OR (tr.tenantId = :systemTenantId " +
            "AND NOT EXISTS " +
            "(SELECT sr FROM TbResourceEntity sr " +
            "WHERE sr.tenantId = :tenantId " +
            "AND tr.resourceType = sr.resourceType " +
            "AND tr.resourceKey = sr.resourceKey)))" +
            "AND tr.resourceType IN :resourceTypes " +
            "AND (:resourceSubTypes IS NULL OR tr.resourceSubType IN :resourceSubTypes)")
    Page<TbResourceInfoEntity> findAllTenantResourcesByTenantId(@Param("tenantId") UUID tenantId,
                                                                @Param("systemTenantId") UUID systemTenantId,
                                                                @Param("resourceTypes") List<String> resourceTypes,
                                                                @Param("resourceSubTypes") List<String> resourceSubTypes,
                                                                @Param("searchText") String searchText,
                                                                Pageable pageable);

    @Query("SELECT ri FROM TbResourceInfoEntity ri WHERE " +
            "ri.tenantId = :tenantId AND (ri.customerId IS NULL OR ri.customerId = org.thingsboard.server.common.data.id.EntityId.NULL_UUID) " +
            "AND ri.resourceType IN :resourceTypes " +
            "AND (:resourceSubTypes IS NULL OR ri.resourceSubType IN :resourceSubTypes) " +
            "AND (:searchText IS NULL OR ilike(ri.title, CONCAT('%', :searchText, '%')) = true)")
    Page<TbResourceInfoEntity> findTenantResourcesByTenantId(@Param("tenantId") UUID tenantId,
                                                             @Param("resourceTypes") List<String> resourceTypes,
                                                             @Param("resourceSubTypes") List<String> resourceSubTypes,
                                                             @Param("searchText") String searchText,
                                                             Pageable pageable);

    @Query("SELECT ri FROM TbResourceInfoEntity ri WHERE " +
            "ri.tenantId = :tenantId AND ri.customerId = :customerId " +
            "AND ri.resourceType IN :resourceTypes " +
            "AND (:resourceSubTypes IS NULL OR ri.resourceSubType IN :resourceSubTypes) " +
            "AND (:searchText IS NULL OR ilike(ri.title, CONCAT('%', :searchText, '%')) = true)")
    Page<TbResourceInfoEntity> findTenantResourcesByCustomerId(@Param("tenantId") UUID tenantId,
                                                               @Param("customerId") UUID customerId,
                                                               @Param("resourceTypes") List<String> resourceTypes,
                                                               @Param("resourceSubTypes") List<String> resourceSubTypes,
                                                               @Param("searchText") String searchText,
                                                               Pageable pageable);

    TbResourceInfoEntity findByTenantIdAndResourceTypeAndResourceKey(UUID tenantId, String resourceType, String resourceKey);

    TbResourceInfoEntity findByTenantIdAndCustomerIdAndResourceTypeAndResourceKey(UUID tenantId, UUID customerId, String resourceType, String resourceKey);

    boolean existsByTenantIdAndResourceTypeAndResourceKey(UUID tenantId, String resourceType, String resourceKey);

    @Query(value = "SELECT r.resource_key FROM resource r WHERE r.tenant_id = :tenantId AND r.resource_type = :resourceType " +
            "AND starts_with(r.resource_key, :prefix)", nativeQuery = true)
    Set<String> findKeysByTenantIdAndResourceTypeAndResourceKeyStartingWith(@Param("tenantId") UUID tenantId,
                                                                            @Param("resourceType") String resourceType,
                                                                            @Param("prefix") String prefix);

    List<TbResourceInfoEntity> findByTenantIdAndEtagAndResourceKeyStartingWith(UUID tenantId, String etag, String query);

    @Query(value = "SELECT * FROM resource r WHERE (r.tenant_id = '13814000-1dd2-11b2-8080-808080808080' OR r.tenant_id = :tenantId) " +
            "AND r.resource_type = :resourceType AND r.etag = :etag ORDER BY created_time, id LIMIT 1", nativeQuery = true)
    TbResourceInfoEntity findSystemOrTenantResourceByEtag(@Param("tenantId") UUID tenantId,
                                                          @Param("resourceType") String resourceType,
                                                          @Param("etag") String etag);

    @Query(value = "SELECT * FROM resource r WHERE (r.tenant_id = '13814000-1dd2-11b2-8080-808080808080' " +
            "OR (r.tenant_id = :tenantId AND r.customer_id = :customerId)) " +
            "AND r.resource_type = :resourceType AND r.etag = :etag LIMIT 1", nativeQuery = true)
    TbResourceInfoEntity findSystemOrCustomerImageByEtag(@Param("tenantId") UUID tenantId,
                                                         @Param("customerId") UUID customerId,
                                                         @Param("resourceType") String resourceType,
                                                         @Param("etag") String etag);

    boolean existsByResourceTypeAndPublicResourceKey(String resourceType, String publicResourceKey);

    TbResourceInfoEntity findByResourceTypeAndPublicResourceKeyAndIsPublicTrue(String resourceType, String publicResourceKey);

    @Query("SELECT tr FROM TbResourceInfoEntity tr WHERE " +
            "tr.id IN (:resourceIds) AND (tr.tenantId = :tenantId OR tr.tenantId = :systemTenantId)")
    List<TbResourceInfoEntity> findSystemOrTenantResourcesByIdIn(@Param("tenantId") UUID tenantId,
                                                                 @Param("systemTenantId") UUID systemTenantId,
                                                                 @Param("resourceIds") List<UUID> resourceIds);

}
