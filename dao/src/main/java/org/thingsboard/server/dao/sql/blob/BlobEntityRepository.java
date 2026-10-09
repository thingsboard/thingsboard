// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.blob;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.edqs.fields.BlobEntityFields;
import org.thingsboard.server.dao.model.sql.BlobEntityEntity;

import java.util.List;
import java.util.UUID;

public interface BlobEntityRepository extends JpaRepository<BlobEntityEntity, UUID> {

    Page<BlobEntityEntity> findByTenantId(UUID tenantId, Pageable pageable);

    @Query("SELECT new org.thingsboard.server.common.data.edqs.fields.BlobEntityFields(b.id, b.createdTime, b.tenantId, " +
            "b.customerId, b.name, b.type, b.additionalInfo) FROM BlobEntityEntity b WHERE b.id > :id ORDER BY b.id")
    List<BlobEntityFields> findNextBatch(@Param("id") UUID id, Limit limit);

}
