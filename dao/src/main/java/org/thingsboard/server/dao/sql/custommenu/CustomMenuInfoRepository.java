// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.custommenu;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.menu.CMAssigneeType;
import org.thingsboard.server.common.data.menu.CMScope;
import org.thingsboard.server.dao.model.sql.CustomMenuInfoEntity;

import java.util.UUID;


public interface CustomMenuInfoRepository extends JpaRepository<CustomMenuInfoEntity, UUID> {

    @Query("SELECT m FROM CustomMenuInfoEntity m WHERE m.tenantId = :tenantId AND m.customerId = :customerId " +
            "AND (:scope IS NULL OR m.scope = :scope) AND (:assigneeType IS NULL OR m.assigneeType = :assigneeType) " +
            "AND (:searchText IS NULL OR ilike(m.name, CONCAT('%', :searchText, '%')) = true )")
    Page<CustomMenuInfoEntity> findByTenantIdAndCustomerIdAndScopeAndAssigneeType(@Param("tenantId") UUID tenantId,
                                                                                  @Param("customerId") UUID customerId,
                                                                                  @Param("scope") CMScope scope,
                                                                                  @Param("assigneeType") CMAssigneeType assigneeType,
                                                                                  @Param("searchText") String searchText,
                                                                                  Pageable pageable);

}
