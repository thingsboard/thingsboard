// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.customer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.CustomerInfoEntity;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.CUSTOMERS_SUB_CUSTOMERS_QUERY;

public interface CustomerInfoRepository extends JpaRepository<CustomerInfoEntity, UUID> {

    @Query("SELECT ci FROM CustomerInfoEntity ci " +
            "WHERE ci.tenantId = :tenantId " +
            "AND (:searchText IS NULL OR ilike(ci.title, CONCAT('%', :searchText, '%')) = true " +
            "OR ilike(ci.ownerName, CONCAT('%', :searchText, '%')) = true)")
    Page<CustomerInfoEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                            @Param("searchText") String searchText,
                                            Pageable pageable);

    @Query("SELECT ci FROM CustomerInfoEntity ci " +
            "WHERE ci.tenantId = :tenantId AND (ci.parentCustomerId IS NULL OR ci.parentCustomerId = org.thingsboard.server.common.data.id.EntityId.NULL_UUID) " +
            "AND (:searchText IS NULL OR ilike(ci.title, CONCAT('%', :searchText, '%')) = true)")
    Page<CustomerInfoEntity> findTenantCustomersByTenantId(@Param("tenantId") UUID tenantId,
                                                           @Param("searchText") String searchText,
                                                           Pageable pageable);

    @Query("SELECT ci FROM CustomerInfoEntity ci WHERE ci.tenantId = :tenantId AND ci.parentCustomerId = :customerId " +
            "AND (:searchText IS NULL OR ilike(ci.title, CONCAT('%', :searchText, '%')) = true)")
    Page<CustomerInfoEntity> findByTenantIdAndCustomerId(@Param("tenantId") UUID tenantId,
                                                         @Param("customerId") UUID customerId,
                                                         @Param("searchText") String searchText,
                                                         Pageable pageable);

    @Query(value = "SELECT e.*, e.owner_name as ownername, e.created_time as createdtime " +
            "FROM (select ce.id, ce.created_time, ce.additional_info, ce.address, ce.address2, ce.city, " +
            "ce.country, ce.email, ce.phone, ce.state, ce.tenant_id, " +
            "ce.parent_customer_id, ce.title, ce.zip, ce.external_id, ce.custom_menu_id, ce.version, ce.groups, ce.is_public, " +
            "c.title as owner_name from customer_info_view ce " +
            "LEFT JOIN customer c on c.id = ce.parent_customer_id AND c.id != :customerId) e " +
            "WHERE" + CUSTOMERS_SUB_CUSTOMERS_QUERY +
            "AND (:searchText IS NULL OR e.title ILIKE CONCAT('%', :searchText, '%') " +
            "OR e.owner_name ILIKE CONCAT('%', :searchText, '%'))",
            countQuery = "SELECT count(e.id) FROM customer e " +
                    "LEFT JOIN customer c on c.id = e.parent_customer_id AND c.id != :customerId " +
                    "WHERE" + CUSTOMERS_SUB_CUSTOMERS_QUERY +
                    "AND (:searchText IS NULL OR e.title ILIKE CONCAT('%', :searchText, '%') " +
                    "OR c.title ILIKE CONCAT('%', :searchText, '%'))",
            nativeQuery = true)
    Page<CustomerInfoEntity> findByTenantIdAndCustomerIdIncludingSubCustomers(@Param("tenantId") UUID tenantId,
                                                                              @Param("customerId") UUID customerId,
                                                                              @Param("searchText") String searchText,
                                                                              Pageable pageable);

}
