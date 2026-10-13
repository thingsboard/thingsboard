// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.UserInfoEntity;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.SUB_CUSTOMERS_QUERY;

public interface UserInfoRepository extends JpaRepository<UserInfoEntity, UUID> {

    @Query("SELECT ui FROM UserInfoEntity ui " +
            "WHERE ui.tenantId = :tenantId " +
            "AND (:searchText IS NULL OR ilike(ui.email, CONCAT('%', :searchText, '%')) = true " +
            "OR ilike(ui.ownerName, CONCAT('%', :searchText, '%')) = true)")
    Page<UserInfoEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                         @Param("searchText") String searchText,
                                         Pageable pageable);

    @Query("SELECT ui FROM UserInfoEntity ui " +
            "WHERE ui.tenantId = :tenantId AND (ui.customerId IS NULL OR ui.customerId = org.thingsboard.server.common.data.id.EntityId.NULL_UUID) " +
            "AND (:searchText IS NULL OR ilike(ui.email, CONCAT('%', :searchText, '%')) = true)")
    Page<UserInfoEntity> findTenantUsersByTenantId(@Param("tenantId") UUID tenantId,
                                                    @Param("searchText") String searchText,
                                                    Pageable pageable);

    @Query("SELECT ui FROM UserInfoEntity ui WHERE ui.tenantId = :tenantId AND ui.customerId = :customerId " +
            "AND (:searchText IS NULL OR ilike(ui.email, CONCAT('%', :searchText, '%')) = true)")
    Page<UserInfoEntity> findByTenantIdAndCustomerId(@Param("tenantId") UUID tenantId,
                                                      @Param("customerId") UUID customerId,
                                                      @Param("searchText") String searchText,
                                                      Pageable pageable);

    @Query(value = "SELECT e.*, e.owner_name as ownername, e.created_time as createdtime , e.first_name as firstname, e.last_name as lastname " +
            "FROM (select u.id, u.created_time, u.additional_info, u.authority, u.customer_id, u.email, " +
            "u.first_name, u.last_name, u.phone, u.tenant_id, u.version, u.custom_menu_id, u.external_id, u.groups, " +
            "c.title as owner_name from user_info_view u " +
            "LEFT JOIN customer c on c.id = u.customer_id AND c.id != :customerId) e " +
            "WHERE" + SUB_CUSTOMERS_QUERY +
            "AND (:searchText IS NULL OR e.email ILIKE CONCAT('%', :searchText, '%') " +
            "OR e.owner_name ILIKE CONCAT('%', :searchText, '%'))",
            countQuery = "SELECT count(e.id) FROM tb_user e " +
                    "LEFT JOIN customer c on c.id = e.customer_id AND c.id != :customerId " +
                    "WHERE" + SUB_CUSTOMERS_QUERY +
                    "AND (:searchText IS NULL OR e.email ILIKE CONCAT('%', :searchText, '%') " +
                    "OR c.title ILIKE CONCAT('%', :searchText, '%'))",
            nativeQuery = true)
    Page<UserInfoEntity> findByTenantIdAndCustomerIdIncludingSubCustomers(@Param("tenantId") UUID tenantId,
                                                                          @Param("customerId") UUID customerId,
                                                                          @Param("searchText") String searchText,
                                                                          Pageable pageable);

}
