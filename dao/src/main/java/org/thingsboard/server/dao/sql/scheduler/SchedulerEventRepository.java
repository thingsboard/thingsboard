// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.scheduler;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.edqs.fields.SchedulerEventFields;
import org.thingsboard.server.dao.ExportableEntityRepository;
import org.thingsboard.server.dao.model.sql.SchedulerEventEntity;

import java.util.List;
import java.util.UUID;

public interface SchedulerEventRepository extends JpaRepository<SchedulerEventEntity, UUID>, ExportableEntityRepository<SchedulerEventEntity> {

    Long countByTenantId(UUID tenantId);

    @Query("SELECT se FROM SchedulerEventEntity se, RelationEntity re WHERE se.tenantId = :tenantId " +
            "AND se.id = re.toId AND re.toType = 'SCHEDULER_EVENT' AND re.relationTypeGroup = 'EDGE' " +
            "AND re.relationType = 'Contains' AND re.fromId = :edgeId AND re.fromType = 'EDGE' " +
            "AND (:searchText IS NULL OR ilike(se.name, CONCAT('%', :searchText, '%')) = true)")
    Page<SchedulerEventEntity> findByTenantIdAndEdgeId(@Param("tenantId") UUID tenantId,
                                                       @Param("edgeId") UUID edgeId,
                                                       @Param("searchText") String searchText,
                                                       Pageable pageable);

    Page<SchedulerEventEntity> findByTenantId(UUID tenantId, Pageable pageable);

    @Query("SELECT se.id FROM SchedulerEventEntity se WHERE se.tenantId = :tenantId")
    Page<UUID> findIdsByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT new org.thingsboard.server.common.data.edqs.fields.SchedulerEventFields(e.id, e.createdTime, " +
            "e.tenantId, e.customerId, e.name, e.version, e.type, e.schedule, e.configuration, e.additionalInfo, e.originatorId, e.originatorType) " +
            "FROM SchedulerEventEntity e WHERE e.id > :id ORDER BY e.id")
    List<SchedulerEventFields> findNextBatch(@Param("id") UUID id, Limit limit);

    @Query("SELECT externalId FROM SchedulerEventEntity WHERE id = :id")
    UUID getExternalIdById(@Param("id") UUID id);

}
