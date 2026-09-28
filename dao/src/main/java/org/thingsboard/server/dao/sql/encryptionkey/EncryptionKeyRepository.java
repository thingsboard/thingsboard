// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.encryptionkey;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.dao.model.sql.EncryptionKeyEntity;

import java.util.UUID;

public interface EncryptionKeyRepository extends JpaRepository<EncryptionKeyEntity, UUID> {

    EncryptionKeyEntity findByTenantId(UUID tenantId);

    @Transactional
    @Modifying
    @Query("DELETE FROM EncryptionKeyEntity r WHERE r.tenantId = :tenantId")
    void deleteByTenantId(@Param("tenantId") UUID tenantId);

}
