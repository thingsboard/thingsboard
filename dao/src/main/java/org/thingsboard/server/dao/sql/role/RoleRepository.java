// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.role;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.edqs.fields.RoleFields;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.dao.ExportableEntityRepository;
import org.thingsboard.server.dao.model.sql.RoleEntity;

import java.util.List;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<RoleEntity, UUID>, ExportableEntityRepository<RoleEntity> {

    RoleEntity findByTenantIdAndCustomerIdAndName(UUID tenantId, UUID customerId, String name);

    @Query("SELECT r FROM RoleEntity r WHERE r.tenantId = :tenantId " +
            "AND r.customerId = :customerId " +
            "AND (:searchText IS NULL OR ilike(r.name, CONCAT('%', :searchText, '%')) = true)")
    Page<RoleEntity> findByTenantIdAndCustomerId(@Param("tenantId") UUID tenantId,
                                                 @Param("customerId") UUID customerId,
                                                 @Param("searchText") String searchText,
                                                 Pageable pageable);

    @Query("SELECT r FROM RoleEntity r WHERE r.tenantId = :tenantId " +
            "AND r.customerId = :customerId AND r.type = :type " +
            "AND (:searchText IS NULL OR ilike(r.name, CONCAT('%', :searchText, '%')) = true)")
    Page<RoleEntity> findByTenantIdAndCustomerIdAndType(@Param("tenantId") UUID tenantId,
                                                        @Param("customerId") UUID customerId,
                                                        @Param("type") RoleType type,
                                                        @Param("searchText") String searchText,
                                                        Pageable pageable);

    List<RoleEntity> findRolesByTenantIdAndIdIn(UUID tenantId, List<UUID> roleIds);

    Page<RoleEntity> findByTenantId(UUID tenantId, Pageable pageable);

    @Query("SELECT externalId FROM RoleEntity WHERE id = :id")
    UUID getExternalIdById(@Param("id") UUID id);

    @Query("SELECT new org.thingsboard.server.common.data.edqs.fields.RoleFields(r.id, r.createdTime, " +
            "r.tenantId, r.customerId, r.name, r.version, r.type, r.additionalInfo) FROM RoleEntity r WHERE r.id > :id ORDER BY r.id")
    List<RoleFields> findNextBatch(@Param("id") UUID id, Limit limit);

}
