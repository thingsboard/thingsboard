// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.edge;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.EdgeInfoEntity;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.SUB_CUSTOMERS_QUERY;

public interface EdgeInfoRepository extends JpaRepository<EdgeInfoEntity, UUID> {

    @Query("SELECT ei FROM EdgeInfoEntity ei " +
            "WHERE ei.tenantId = :tenantId " +
            "AND (:searchText IS NULL OR ilike(ei.name, CONCAT('%', :searchText, '%')) = true " +
            "OR ilike(ei.ownerName, CONCAT('%', :searchText, '%')) = true)")
    Page<EdgeInfoEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                        @Param("searchText") String searchText,
                                        Pageable pageable);

    @Query("SELECT ei FROM EdgeInfoEntity ei " +
            "WHERE ei.tenantId = :tenantId " +
            "AND ei.type = :type " +
            "AND (:searchText IS NULL OR ilike(ei.name, CONCAT('%', :searchText, '%')) = true " +
            "OR ilike(ei.ownerName, CONCAT('%', :searchText, '%')) = true)")
    Page<EdgeInfoEntity> findByTenantIdAndType(@Param("tenantId") UUID tenantId,
                                               @Param("type") String type,
                                               @Param("searchText") String searchText,
                                               Pageable pageable);

    @Query("SELECT ei FROM EdgeInfoEntity ei " +
            "WHERE ei.tenantId = :tenantId AND (ei.customerId IS NULL OR ei.customerId = org.thingsboard.server.common.data.id.EntityId.NULL_UUID) " +
            "AND (:searchText IS NULL OR ilike(ei.name, CONCAT('%', :searchText, '%')) = true)")
    Page<EdgeInfoEntity> findTenantEdgesByTenantId(@Param("tenantId") UUID tenantId,
                                                   @Param("searchText") String searchText,
                                                   Pageable pageable);

    @Query("SELECT ei FROM EdgeInfoEntity ei " +
            "WHERE ei.tenantId = :tenantId AND (ei.customerId IS NULL OR ei.customerId = org.thingsboard.server.common.data.id.EntityId.NULL_UUID) " +
            "AND ei.type = :type " +
            "AND (:searchText IS NULL OR ilike(ei.name, CONCAT('%', :searchText, '%')) = true)")
    Page<EdgeInfoEntity> findTenantEdgesByTenantIdAndType(@Param("tenantId") UUID tenantId,
                                                          @Param("type") String type,
                                                          @Param("searchText") String searchText,
                                                          Pageable pageable);

    @Query("SELECT ei FROM EdgeInfoEntity ei WHERE ei.tenantId = :tenantId AND ei.customerId = :customerId " +
            "AND (:searchText IS NULL OR ilike(ei.name, CONCAT('%', :searchText, '%')) = true)")
    Page<EdgeInfoEntity> findByTenantIdAndCustomerId(@Param("tenantId") UUID tenantId,
                                                     @Param("customerId") UUID customerId,
                                                     @Param("searchText") String searchText,
                                                     Pageable pageable);

    @Query("SELECT ei FROM EdgeInfoEntity ei WHERE ei.tenantId = :tenantId AND ei.customerId = :customerId " +
            "AND ei.type = :type " +
            "AND (:searchText IS NULL OR ilike(ei.name, CONCAT('%', :searchText, '%')) = true)")
    Page<EdgeInfoEntity> findByTenantIdAndCustomerIdAndType(@Param("tenantId") UUID tenantId,
                                                            @Param("customerId") UUID customerId,
                                                            @Param("type") String type,
                                                            @Param("searchText") String searchText,
                                                            Pageable pageable);

    @Query(value = "SELECT e.*, e.owner_name as ownername, e.created_time as createdtime " +
            "FROM (select e.id, e.created_time, e.additional_info, e.customer_id, e.root_rule_chain_id, " +
            "e.type, e.name, e.label, e.routing_key, e.secret, e.edge_license_key, e.cloud_endpoint, " +
            "e.tenant_id, e.version, e.groups, " +
            "c.title as owner_name from edge_info_view e " +
            "LEFT JOIN customer c on c.id = e.customer_id AND c.id != :customerId) e " +
            "WHERE" + SUB_CUSTOMERS_QUERY +
            "AND (:searchText IS NULL OR e.name ILIKE CONCAT('%', :searchText, '%') " +
            "OR e.owner_name ILIKE CONCAT('%', :searchText, '%'))",
            countQuery = "SELECT count(e.id) FROM edge e " +
                    "LEFT JOIN customer c on c.id = e.customer_id AND c.id != :customerId " +
                    "WHERE" + SUB_CUSTOMERS_QUERY +
                    "AND (:searchText IS NULL OR e.name ILIKE CONCAT('%', :searchText, '%') " +
                    "OR c.title ILIKE CONCAT('%', :searchText, '%'))",
            nativeQuery = true)
    Page<EdgeInfoEntity> findByTenantIdAndCustomerIdIncludingSubCustomers(@Param("tenantId") UUID tenantId,
                                                                          @Param("customerId") UUID customerId,
                                                                          @Param("searchText") String searchText,
                                                                          Pageable pageable);

    @Query(value = "SELECT e.*, e.owner_name as ownername, e.created_time as createdtime " +
            "FROM (select e.id, e.created_time, e.additional_info, e.customer_id, e.root_rule_chain_id, " +
            "e.type, e.name, e.label, e.routing_key, e.secret, e.edge_license_key, e.cloud_endpoint, " +
            "e.tenant_id, e.version, e.groups, " +
            "c.title as owner_name from edge_info_view e " +
            "LEFT JOIN customer c on c.id = e.customer_id AND c.id != :customerId) e " +
            "WHERE" + SUB_CUSTOMERS_QUERY +
            "AND e.type = :type " +
            "AND (:searchText IS NULL OR e.name ILIKE CONCAT('%', :searchText, '%') " +
            "OR e.owner_name ILIKE CONCAT('%', :searchText, '%'))",
            countQuery = "SELECT count(e.id) FROM edge e " +
                    "LEFT JOIN customer c on c.id = e.customer_id AND c.id != :customerId " +
                    "WHERE" + SUB_CUSTOMERS_QUERY +
                    "AND e.type = :type " +
                    "AND (:searchText IS NULL OR e.name ILIKE CONCAT('%', :searchText, '%')" +
                    "OR c.title ILIKE CONCAT('%', :searchText, '%'))",
            nativeQuery = true)
    Page<EdgeInfoEntity> findByTenantIdAndCustomerIdAndTypeIncludingSubCustomers(@Param("tenantId") UUID tenantId,
                                                                                 @Param("customerId") UUID customerId,
                                                                                 @Param("type") String type,
                                                                                 @Param("searchText") String searchText,
                                                                                 Pageable pageable);
}
