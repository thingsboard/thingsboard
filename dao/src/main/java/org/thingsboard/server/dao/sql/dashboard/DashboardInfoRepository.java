// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.sql.dashboard;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.DashboardFullInfoEntity;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.dao.model.sql.DashboardInfoEntity;

import java.util.List;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.SUB_CUSTOMERS_QUERY;

/**
 * Created by Valerii Sosliuk on 5/6/2017.
 */
public interface DashboardInfoRepository extends JpaRepository<DashboardInfoEntity, UUID> {

    @Query("SELECT di FROM DashboardFullInfoEntity di " +
            "WHERE di.id = :dashboardId")
    DashboardFullInfoEntity findFullInfoById(@Param("dashboardId") UUID dashboardId);

    DashboardInfoEntity findFirstByTenantIdAndTitle(UUID tenantId, String title);

    @Query("SELECT di FROM DashboardInfoEntity di " +
            "WHERE di.tenantId = :tenantId " +
            "AND (:searchText IS NULL OR ilike(di.title, CONCAT('%', :searchText, '%')) = true " +
            "OR ilike(di.ownerName, CONCAT('%', :searchText, '%')) = true)")
    Page<DashboardInfoEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                             @Param("searchText") String searchText,
                                             Pageable pageable);

    @Query("SELECT di FROM DashboardInfoEntity di " +
            "WHERE di.tenantId = :tenantId AND (di.customerId IS NULL OR di.customerId = org.thingsboard.server.common.data.id.EntityId.NULL_UUID) " +
            "AND (:searchText IS NULL OR ilike(di.title, CONCAT('%', :searchText, '%')) = true)")
    Page<DashboardInfoEntity> findTenantDashboardsByTenantId(@Param("tenantId") UUID tenantId,
                                                             @Param("searchText") String searchText,
                                                             Pageable pageable);

    @Query("SELECT di FROM DashboardInfoEntity di WHERE di.tenantId = :tenantId " +
            "AND di.mobileHide = false " +
            "AND (:searchText IS NULL OR ilike(di.title, CONCAT('%', :searchText, '%')) = true)")
    Page<DashboardInfoEntity> findMobileByTenantId(@Param("tenantId") UUID tenantId,
                                                   @Param("searchText") String searchText,
                                                   Pageable pageable);

    @Query("SELECT di FROM DashboardInfoEntity di WHERE di.tenantId = :tenantId AND di.customerId = :customerId " +
            "AND (:searchText IS NULL OR ilike(di.title, CONCAT('%', :searchText, '%')) = true)")
    Page<DashboardInfoEntity> findByTenantIdAndCustomerId(@Param("tenantId") UUID tenantId,
                                                          @Param("customerId") UUID customerId,
                                                          @Param("searchText") String searchText,
                                                          Pageable pageable);

    @Query(value = "SELECT e.*, e.owner_name as ownername, e.created_time as createdtime " +
            "FROM (select d.id, d.created_time, d.assigned_customers, d.tenant_id, " +
            "d.customer_id, d.title, d.mobile_hide, d.mobile_order, d.image, d.groups, d.external_id, d.version, " +
            "c.title as owner_name FROM dashboard_info_view d " +
            "LEFT JOIN customer c on c.id = d.customer_id AND c.id != :customerId) e " +
            "WHERE" + SUB_CUSTOMERS_QUERY +
            "AND (:searchText IS NULL OR e.title ILIKE CONCAT('%', :searchText, '%') " +
            "OR e.owner_name ILIKE CONCAT('%', :searchText, '%'))",
            countQuery = "SELECT count(e.id) FROM dashboard e " +
                    "LEFT JOIN customer c on c.id = e.customer_id AND c.id != :customerId " +
                    "WHERE" + SUB_CUSTOMERS_QUERY +
                    "AND (:searchText IS NULL OR e.title ILIKE CONCAT('%', :searchText, '%') " +
                    "OR c.title ILIKE CONCAT('%', :searchText, '%'))",
            nativeQuery = true)
    Page<DashboardInfoEntity> findByTenantIdAndCustomerIdIncludingSubCustomers(@Param("tenantId") UUID tenantId,
                                                                               @Param("customerId") UUID customerId,
                                                                               @Param("searchText") String searchText,
                                                                               Pageable pageable);

    @Query("SELECT di FROM DashboardInfoEntity di, RelationEntity re WHERE di.tenantId = :tenantId " +
            "AND di.mobileHide = false " +
            "AND di.id = re.toId AND re.toType = 'DASHBOARD' AND re.relationTypeGroup = 'DASHBOARD' " +
            "AND re.relationType = 'Contains' AND re.fromId = :customerId AND re.fromType = 'CUSTOMER' " +
            "AND (:searchText IS NULL OR ilike(di.title, CONCAT('%', :searchText, '%')) = true)")
    Page<DashboardInfoEntity> findMobileByTenantIdAndCustomerId(@Param("tenantId") UUID tenantId,
                                                                @Param("customerId") UUID customerId,
                                                                @Param("searchText") String searchText,
                                                                Pageable pageable);

    @Query("SELECT di FROM DashboardInfoEntity di, " +
            "RelationEntity re " +
            "WHERE di.id = re.toId AND re.toType = 'DASHBOARD' " +
            "AND re.relationTypeGroup = 'FROM_ENTITY_GROUP' " +
            "AND re.relationType = 'Contains' " +
            "AND re.fromId = :groupId AND re.fromType = 'ENTITY_GROUP' " +
            "AND (:searchText IS NULL OR ilike(di.title, CONCAT('%', :searchText, '%')) = true)")
    Page<DashboardInfoEntity> findByEntityGroupId(@Param("groupId") UUID groupId,
                                                  @Param("searchText") String searchText,
                                                  Pageable pageable);

    @Query("SELECT di FROM DashboardInfoEntity di, " +
            "RelationEntity re " +
            "WHERE di.id = re.toId AND re.toType = 'DASHBOARD' " +
            "AND re.relationTypeGroup = 'FROM_ENTITY_GROUP' " +
            "AND re.relationType = 'Contains' " +
            "AND re.fromId in :groupIds AND re.fromType = 'ENTITY_GROUP' " +
            "AND (:searchText IS NULL OR ilike(di.title, CONCAT('%', :searchText, '%')) = true)")
    Page<DashboardInfoEntity> findByEntityGroupIds(@Param("groupIds") List<UUID> groupIds,
                                                   @Param("searchText") String searchText,
                                                   Pageable pageable);

    @Query("SELECT di FROM DashboardInfoEntity di, " +
            "RelationEntity re " +
            "WHERE di.id = re.toId AND di.mobileHide = false AND re.toType = 'DASHBOARD' " +
            "AND re.relationTypeGroup = 'FROM_ENTITY_GROUP' " +
            "AND re.relationType = 'Contains' " +
            "AND re.fromId in :groupIds AND re.fromType = 'ENTITY_GROUP' " +
            "AND (:searchText IS NULL OR ilike(di.title, CONCAT('%', :searchText, '%')) = true)")
    Page<DashboardInfoEntity> findMobileByEntityGroupIds(@Param("groupIds") List<UUID> groupIds,
                                                         @Param("searchText") String searchText,
                                                         Pageable pageable);

    List<DashboardInfoEntity> findByIdIn(List<UUID> dashboardIds);

    @Query("SELECT di FROM DashboardInfoEntity di, RelationEntity re WHERE di.tenantId = :tenantId " +
            "AND di.id = re.toId AND re.toType = 'DASHBOARD' AND re.relationTypeGroup = 'EDGE' " +
            "AND re.relationType = 'Contains' AND re.fromId = :edgeId AND re.fromType = 'EDGE' " +
            "AND (:searchText IS NULL OR ilike(di.title, CONCAT('%', :searchText, '%')) = true)")
    Page<DashboardInfoEntity> findByTenantIdAndEdgeId(@Param("tenantId") UUID tenantId,
                                                      @Param("edgeId") UUID edgeId,
                                                      @Param("searchText") String searchText,
                                                      Pageable pageable);

    @Query("SELECT di.title FROM DashboardInfoEntity di WHERE di.tenantId = :tenantId AND di.id = :dashboardId")
    String findTitleByTenantIdAndId(@Param("tenantId") UUID tenantId, @Param("dashboardId") UUID dashboardId);

    @Query(nativeQuery = true,
            value = "SELECT * FROM dashboard_info_view d WHERE d.tenant_id = :tenantId " +
                    "and (d.image = :imageLink or d.configuration ILIKE CONCAT('%\"', :imageLink, '\"%')) limit :limit"
    )
    List<DashboardInfoEntity> findByTenantAndImageLink(@Param("tenantId") UUID tenantId, @Param("imageLink") String imageLink, @Param("limit") int limit);

    @Query(nativeQuery = true,
            value = "SELECT * FROM dashboard_info_view d WHERE d.image = :imageLink or d.configuration ILIKE CONCAT('%\"', :imageLink, '\"%') limit :limit"
    )
    List<DashboardInfoEntity> findByImageLink(@Param("imageLink") String imageLink, @Param("limit") int limit);

    @Query("SELECT new org.thingsboard.server.common.data.EntityInfo(d.id, 'DASHBOARD', d.title) " +
            "FROM DashboardEntity d WHERE d.tenantId = :tenantId AND ilike(cast(d.configuration as string), CONCAT('%', :link, '%')) = true")
    List<EntityInfo> findDashboardInfosByTenantIdAndResourceLink(@Param("tenantId") UUID tenantId,
                                                                 @Param("link") String link,
                                                                 Pageable pageable);

    @Query("SELECT new org.thingsboard.server.common.data.EntityInfo(d.id, 'DASHBOARD', d.title) " +
            "FROM DashboardEntity d WHERE ilike(cast(d.configuration as string), CONCAT('%', :link, '%')) = true")
    List<EntityInfo> findDashboardInfosByResourceLink(@Param("link") String link,
                                                      Pageable pageable);

}
