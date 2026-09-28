// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.sql.alarm;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.dao.model.sql.EntityAlarmCompositeKey;
import org.thingsboard.server.dao.model.sql.EntityAlarmEntity;

import java.util.List;
import java.util.UUID;

public interface EntityAlarmRepository extends JpaRepository<EntityAlarmEntity, EntityAlarmCompositeKey> {

    // No conflict target so the clause matches both primary key shapes: (entity_id, alarm_id) on plain PostgreSQL
    // and (originator_id, entity_id, alarm_id) on Citus. DO NOTHING is safe because entity alarm records are
    // immutable for a given (entity_id, alarm_id): re-saves on propagation changes always carry identical values.
    @Transactional
    @Modifying
    @Query(value = "INSERT INTO entity_alarm (tenant_id, entity_type, entity_id, originator_id, created_time, alarm_type, customer_id, alarm_id) " +
            "VALUES (cast(:tenantId as uuid), :entityType, cast(:entityId as uuid), cast(:originatorId as uuid), :createdTime, :alarmType, cast(:customerId as uuid), cast(:alarmId as uuid)) " +
            "ON CONFLICT DO NOTHING", nativeQuery = true)
    void insert(@Param("tenantId") UUID tenantId,
                @Param("entityType") String entityType,
                @Param("entityId") UUID entityId,
                @Param("originatorId") UUID originatorId,
                @Param("createdTime") long createdTime,
                @Param("alarmType") String alarmType,
                @Param("customerId") UUID customerId,
                @Param("alarmId") UUID alarmId);

    // entity_alarm is distributed on originator_id under Citus, so the originator predicate prunes the read to the
    // single shard co-located with the alarm; filtering on alarm_id alone would fan out to every shard.
    List<EntityAlarmEntity> findAllByOriginatorIdAndAlarmId(UUID originatorId, UUID alarmId);

    List<EntityAlarmEntity> findAllByOriginatorIdAndAlarmIdAndEntityTypeIn(UUID originatorId, UUID alarmId, List<String> entityTypes);

    @Transactional
    @Modifying
    @Query("DELETE FROM EntityAlarmEntity e where e.entityId = :entityId")
    int deleteByEntityId(@Param("entityId") UUID entityId);

    @Transactional
    @Modifying
    @Query("DELETE FROM EntityAlarmEntity a WHERE a.tenantId = :tenantId")
    void deleteByTenantId(@Param("tenantId") UUID tenantId);

    List<EntityAlarmEntity> findAllByEntityId(UUID entityId);

    Page<EntityAlarmEntity> findByTenantId(UUID tenantId, Pageable pageable);

}
