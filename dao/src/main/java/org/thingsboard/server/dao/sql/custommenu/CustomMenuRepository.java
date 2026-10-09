// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.custommenu;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.menu.CMScope;
import org.thingsboard.server.dao.model.sql.CustomMenuEntity;

import java.util.Optional;
import java.util.UUID;


public interface CustomMenuRepository extends JpaRepository<CustomMenuEntity, UUID> {

    @Query("SELECT m FROM CustomMenuEntity m " +
            "WHERE m.tenantId = :tenantId AND m.customerId = :customerId AND m.scope = :scope AND m.assigneeType = 'ALL'")
    CustomMenuEntity findDefaultByTenantIdAndCustomerIdAndScope(@Param("tenantId") UUID tenantId,
                                                                @Param("customerId") UUID customerId,
                                                                @Param("scope") CMScope scope);

    @Transactional
    @Modifying
    @Query("DELETE FROM CustomMenuEntity r WHERE r.tenantId = :tenantId")
    void deleteByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT m FROM CustomMenuEntity m WHERE m.tenantId = :tenantId")
    Page<CustomMenuEntity> findByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query(value = "SELECT m.* FROM custom_menu m WHERE m.tenant_id   = :tenantId " +
            "AND m.customer_id = :customerId " +
            "AND m.scope = :scope " +
            "AND m.assignee_type = 'USER_GROUPS' " +
            "AND m.user_group_names && CAST(:userGroupNames AS text[]) LIMIT 1", nativeQuery = true)
    Optional<CustomMenuEntity> findFirstByScopeAndUserGroupNames(@Param("tenantId") UUID tenantId,
                                                                 @Param("customerId") UUID customerId,
                                                                 @Param("scope") String scope,
                                                                 @Param("userGroupNames") String[] userGroupNames);
}