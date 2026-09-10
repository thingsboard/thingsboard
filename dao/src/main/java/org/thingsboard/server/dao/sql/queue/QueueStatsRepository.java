// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.sql.queue;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.edqs.fields.QueueStatsFields;
import org.thingsboard.server.dao.model.sql.QueueStatsEntity;

import java.util.List;
import java.util.UUID;

public interface QueueStatsRepository extends JpaRepository<QueueStatsEntity, UUID> {

    QueueStatsEntity findByTenantIdAndQueueNameAndServiceId(UUID tenantId, String queueName, String serviceId);

    @Query("SELECT q FROM QueueStatsEntity q WHERE q.tenantId = :tenantId " +
            "AND (:textSearch IS NULL OR ilike(q.queueName, CONCAT('%', :textSearch, '%')) = true " +
            "OR ilike(q.serviceId, CONCAT('%', :textSearch, '%')) = true)")
    Page<QueueStatsEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                          @Param("textSearch") String textSearch,
                                          Pageable pageable);

    @Transactional
    @Modifying
    @Query("DELETE FROM QueueStatsEntity t WHERE t.tenantId = :tenantId")
    void deleteByTenantId(@Param("tenantId") UUID tenantId);

    List<QueueStatsEntity> findByTenantIdAndIdIn(UUID tenantId, List<UUID> queueStatsIds);

    @Query("SELECT new org.thingsboard.server.common.data.edqs.fields.QueueStatsFields(q.id, q.createdTime," +
            "q.tenantId, q.queueName, q.serviceId) FROM QueueStatsEntity q WHERE q.id > :id ORDER BY q.id")
    List<QueueStatsFields> findNextBatch(@Param("id") UUID id, Limit limit);

}