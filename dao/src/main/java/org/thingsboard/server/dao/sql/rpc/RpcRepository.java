// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.sql.rpc;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.rpc.RpcStatus;
import org.thingsboard.server.dao.model.sql.RpcEntity;

import java.util.UUID;

public interface RpcRepository extends JpaRepository<RpcEntity, UUID> {

    Page<RpcEntity> findAllByTenantIdAndDeviceId(UUID tenantId, UUID deviceId, Pageable pageable);

    Page<RpcEntity> findAllByTenantIdAndDeviceIdAndStatus(UUID tenantId, UUID deviceId, RpcStatus status, Pageable pageable);

    Page<RpcEntity> findAllByTenantId(UUID tenantId, Pageable pageable);

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM rpc WHERE id IN " +
            "(SELECT id FROM rpc WHERE tenant_id = :tenantId AND created_time < :expirationTime LIMIT :batchSize)",
            nativeQuery = true)
    int deleteOutdatedRpcByTenantIdBatch(@Param("tenantId") UUID tenantId,
                                         @Param("expirationTime") Long expirationTime,
                                         @Param("batchSize") int batchSize);

}
