// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.sql.queue;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.QueueEntity;

import java.util.List;
import java.util.UUID;

public interface QueueRepository extends JpaRepository<QueueEntity, UUID> {
    QueueEntity findByTenantIdAndTopic(UUID tenantId, String topic);

    QueueEntity findByTenantIdAndName(UUID tenantId, String name);

    List<QueueEntity> findByTenantId(UUID tenantId);

    @Query("SELECT q FROM QueueEntity q WHERE q.tenantId = :tenantId " +
            "AND (:textSearch IS NULL OR ilike(q.name, CONCAT(:textSearch, '%')) = true)")
    Page<QueueEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                     @Param("textSearch") String textSearch,
                                     Pageable pageable);

    List<QueueEntity> findAllByName(String name);
}