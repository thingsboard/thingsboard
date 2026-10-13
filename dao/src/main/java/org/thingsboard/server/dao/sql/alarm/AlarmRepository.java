// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.sql.alarm;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.alarm.AlarmRef;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.dao.model.sql.AlarmEntity;
import org.thingsboard.server.dao.model.sql.AlarmInfoEntity;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface AlarmRepository extends JpaRepository<AlarmEntity, UUID> {

    @Query("SELECT a FROM AlarmEntity a WHERE a.originatorId = :originatorId AND a.type = :alarmType ORDER BY a.startTs DESC")
    List<AlarmEntity> findLatestByOriginatorAndType(@Param("originatorId") UUID originatorId,
                                                    @Param("alarmType") String alarmType,
                                                    Pageable pageable);

    @Query("SELECT a FROM AlarmEntity a WHERE a.originatorId = :originatorId AND a.type = :alarmType AND a.cleared = FALSE ORDER BY a.createdTime DESC")
    List<AlarmEntity> findLatestActiveByOriginatorAndType(@Param("originatorId") UUID originatorId,
                                                          @Param("alarmType") String alarmType,
                                                          Pageable pageable);

    @Query(value = "SELECT a " +
            "FROM AlarmInfoEntity a " +
            "LEFT JOIN EntityAlarmEntity ea ON a.id = ea.alarmId AND a.originatorId = ea.originatorId " +
            "WHERE a.tenantId = :tenantId " +
            "AND ea.tenantId = :tenantId " +
            "AND ea.entityId = :affectedEntityId " +
            "AND ea.entityType = :affectedEntityType " +
            "AND (:startTime IS NULL OR (a.createdTime >= :startTime AND ea.createdTime >= :startTime)) " +
            "AND (:endTime IS NULL OR (a.createdTime <= :endTime AND ea.createdTime <= :endTime)) " +
            "AND ((:clearFilterEnabled) = FALSE OR a.cleared = :clearFilter) " +
            "AND ((:ackFilterEnabled) = FALSE OR a.acknowledged = :ackFilter) " +
            "AND (:assigneeId IS NULL OR a.assigneeId = :assigneeId) " +
            "AND (:searchText IS NULL OR (ilike(a.type, CONCAT('%', :searchText, '%')) = true  " +
            "  OR ilike(a.severity, CONCAT('%', :searchText, '%')) = true " +
            "  OR ilike(a.status, CONCAT('%', :searchText, '%')) = true)) "
            ,
            countQuery = "" +
                    "SELECT count(a) " + //alarms with relations only
                    "FROM AlarmInfoEntity a " +
                    "LEFT JOIN EntityAlarmEntity ea ON a.id = ea.alarmId AND a.originatorId = ea.originatorId " +
                    "WHERE a.tenantId = :tenantId " +
                    "AND ea.tenantId = :tenantId " +
                    "AND ea.entityId = :affectedEntityId " +
                    "AND ea.entityType = :affectedEntityType " +
                    "AND (:startTime IS NULL OR (a.createdTime >= :startTime AND ea.createdTime >= :startTime)) " +
                    "AND (:endTime IS NULL OR (a.createdTime <= :endTime AND ea.createdTime <= :endTime)) " +
                    "AND ((:clearFilterEnabled) = FALSE OR a.cleared = :clearFilter) " +
                    "AND ((:ackFilterEnabled) = FALSE OR a.acknowledged = :ackFilter) " +
                    "AND (:assigneeId IS NULL OR a.assigneeId = :assigneeId) " +
                    "AND (:searchText IS NULL OR (ilike(a.type, CONCAT('%', :searchText, '%')) = true " +
                    "  OR ilike(a.severity, CONCAT('%', :searchText, '%')) = true  " +
                    "  OR ilike(a.status, CONCAT('%', :searchText, '%')) = true))")
    Page<AlarmInfoEntity> findAlarms(@Param("tenantId") UUID tenantId,
                                     @Param("affectedEntityId") UUID affectedEntityId,
                                     @Param("affectedEntityType") String affectedEntityType,
                                     @Param("startTime") Long startTime,
                                     @Param("endTime") Long endTime,
                                     @Param("clearFilterEnabled") boolean clearFilterEnabled,
                                     @Param("clearFilter") boolean clearFilter,
                                     @Param("ackFilterEnabled") boolean ackFilterEnabled,
                                     @Param("ackFilter") boolean ackFilter,
                                     @Param("assigneeId") UUID assigneeId,
                                     @Param("searchText") String searchText,
                                     Pageable pageable);

    @Query(value = "SELECT a " +
            "FROM AlarmInfoEntity a " +
            "LEFT JOIN EntityAlarmEntity ea ON a.id = ea.alarmId AND a.originatorId = ea.originatorId " +
            "WHERE a.tenantId = :tenantId " +
            "AND ea.tenantId = :tenantId " +
            "AND ea.entityId = :affectedEntityId " +
            "AND ea.entityType = :affectedEntityType " +
            "AND (:startTime IS NULL OR (a.createdTime >= :startTime AND ea.createdTime >= :startTime)) " +
            "AND (:endTime IS NULL OR (a.createdTime <= :endTime AND ea.createdTime <= :endTime)) " +
            "AND ((:alarmTypes) IS NULL OR a.type IN (:alarmTypes)) " +
            "AND ((:alarmSeverities) IS NULL OR a.severity IN (:alarmSeverities)) " +
            "AND ((:clearFilterEnabled) = FALSE OR a.cleared = :clearFilter) " +
            "AND ((:ackFilterEnabled) = FALSE OR a.acknowledged = :ackFilter) " +
            "AND (:assigneeId IS NULL OR a.assigneeId = :assigneeId) " +
            "AND (:searchText IS NULL OR (ilike(a.type, CONCAT('%', :searchText, '%')) = true  " +
            "  OR ilike(a.severity, CONCAT('%', :searchText, '%')) = true " +
            "  OR ilike(a.status, CONCAT('%', :searchText, '%')) = true)) "
            ,
            countQuery = "" +
                    "SELECT count(a) " + //alarms with relations only
                    "FROM AlarmInfoEntity a " +
                    "LEFT JOIN EntityAlarmEntity ea ON a.id = ea.alarmId AND a.originatorId = ea.originatorId " +
                    "WHERE a.tenantId = :tenantId " +
                    "AND ea.tenantId = :tenantId " +
                    "AND ea.entityId = :affectedEntityId " +
                    "AND ea.entityType = :affectedEntityType " +
                    "AND (:startTime IS NULL OR (a.createdTime >= :startTime AND ea.createdTime >= :startTime)) " +
                    "AND (:endTime IS NULL OR (a.createdTime <= :endTime AND ea.createdTime <= :endTime)) " +
                    "AND ((:alarmTypes) IS NULL OR a.type IN (:alarmTypes)) " +
                    "AND ((:alarmSeverities) IS NULL OR a.severity IN (:alarmSeverities)) " +
                    "AND ((:clearFilterEnabled) = FALSE OR a.cleared = :clearFilter) " +
                    "AND ((:ackFilterEnabled) = FALSE OR a.acknowledged = :ackFilter) " +
                    "AND (:assigneeId IS NULL OR a.assigneeId = :assigneeId) " +
                    "AND (:searchText IS NULL OR (ilike(a.type, CONCAT('%', :searchText, '%')) = true " +
                    "  OR ilike(a.severity, CONCAT('%', :searchText, '%')) = true  " +
                    "  OR ilike(a.status, CONCAT('%', :searchText, '%')) = true))")
    Page<AlarmInfoEntity> findAlarmsV2(@Param("tenantId") UUID tenantId,
                                       @Param("affectedEntityId") UUID affectedEntityId,
                                       @Param("affectedEntityType") String affectedEntityType,
                                       @Param("startTime") Long startTime,
                                       @Param("endTime") Long endTime,
                                       @Param("alarmTypes") List<String> alarmTypes,
                                       @Param("alarmSeverities") List<AlarmSeverity> alarmSeverities,
                                       @Param("clearFilterEnabled") boolean clearFilterEnabled,
                                       @Param("clearFilter") boolean clearFilter,
                                       @Param("ackFilterEnabled") boolean ackFilterEnabled,
                                       @Param("ackFilter") boolean ackFilter,
                                       @Param("assigneeId") UUID assigneeId,
                                       @Param("searchText") String searchText,
                                       Pageable pageable);

    @Query(value = "SELECT a " +
            "FROM AlarmInfoEntity a " +
            "WHERE a.tenantId = :tenantId " +
            "AND (:startTime IS NULL OR a.createdTime >= :startTime) " +
            "AND (:endTime IS NULL OR a.createdTime <= :endTime) " +
            "AND ((:clearFilterEnabled) = FALSE OR a.cleared = :clearFilter) " +
            "AND ((:ackFilterEnabled) = FALSE OR a.acknowledged = :ackFilter) " +
            "AND (:assigneeId IS NULL OR a.assigneeId = :assigneeId) " +
            "AND (:searchText IS NULL OR (ilike(a.type, CONCAT('%', :searchText, '%')) = true  " +
            "  OR ilike(a.severity, CONCAT('%', :searchText, '%')) = true " +
            "  OR ilike(a.status, CONCAT('%', :searchText, '%')) = true)) ",
            countQuery = "" +
                    "SELECT count(a) " +
                    "FROM AlarmInfoEntity a " +
                    "WHERE a.tenantId = :tenantId " +
                    "AND (:startTime IS NULL OR a.createdTime >= :startTime) " +
                    "AND (:endTime IS NULL OR a.createdTime <= :endTime) " +
                    "AND ((:clearFilterEnabled) = FALSE OR a.cleared = :clearFilter) " +
                    "AND ((:ackFilterEnabled) = FALSE OR a.acknowledged = :ackFilter) " +
                    "AND (:assigneeId IS NULL OR a.assigneeId = :assigneeId) " +
                    "AND (:searchText IS NULL OR (ilike(a.type, CONCAT('%', :searchText, '%')) = true " +
                    "  OR ilike(a.severity, CONCAT('%', :searchText, '%')) = true  " +
                    "  OR ilike(a.status, CONCAT('%', :searchText, '%')) = true))")
    Page<AlarmInfoEntity> findAllAlarms(@Param("tenantId") UUID tenantId,
                                        @Param("startTime") Long startTime,
                                        @Param("endTime") Long endTime,
                                        @Param("clearFilterEnabled") boolean clearFilterEnabled,
                                        @Param("clearFilter") boolean clearFilter,
                                        @Param("ackFilterEnabled") boolean ackFilterEnabled,
                                        @Param("ackFilter") boolean ackFilter,
                                        @Param("assigneeId") UUID assigneeId,
                                        @Param("searchText") String searchText,
                                        Pageable pageable);

    @Query(value = "SELECT a " +
            "FROM AlarmInfoEntity a " +
            "WHERE a.tenantId = :tenantId " +
            "AND (:startTime IS NULL OR a.createdTime >= :startTime) " +
            "AND (:endTime IS NULL OR a.createdTime <= :endTime) " +
            "AND ((:alarmTypes) IS NULL OR a.type IN (:alarmTypes)) " +
            "AND ((:alarmSeverities) IS NULL OR a.severity IN (:alarmSeverities)) " +
            "AND ((:clearFilterEnabled) = FALSE OR a.cleared = :clearFilter) " +
            "AND ((:ackFilterEnabled) = FALSE OR a.acknowledged = :ackFilter) " +
            "AND (:assigneeId IS NULL OR a.assigneeId = :assigneeId) " +
            "AND (:searchText IS NULL OR (ilike(a.type, CONCAT('%', :searchText, '%')) = true  " +
            "  OR ilike(a.severity, CONCAT('%', :searchText, '%')) = true " +
            "  OR ilike(a.status, CONCAT('%', :searchText, '%')) = true)) ",
            countQuery = "" +
                    "SELECT count(a) " +
                    "FROM AlarmInfoEntity a " +
                    "WHERE a.tenantId = :tenantId " +
                    "AND (:startTime IS NULL OR a.createdTime >= :startTime) " +
                    "AND (:endTime IS NULL OR a.createdTime <= :endTime) " +
                    "AND ((:alarmTypes) IS NULL OR a.type IN (:alarmTypes)) " +
                    "AND ((:alarmSeverities) IS NULL OR a.severity IN (:alarmSeverities)) " +
                    "AND ((:clearFilterEnabled) = FALSE OR a.cleared = :clearFilter) " +
                    "AND ((:ackFilterEnabled) = FALSE OR a.acknowledged = :ackFilter) " +
                    "AND (:assigneeId IS NULL OR a.assigneeId = :assigneeId) " +
                    "AND (:searchText IS NULL OR (ilike(a.type, CONCAT('%', :searchText, '%')) = true " +
                    "  OR ilike(a.severity, CONCAT('%', :searchText, '%')) = true  " +
                    "  OR ilike(a.status, CONCAT('%', :searchText, '%')) = true))")
    Page<AlarmInfoEntity> findAllAlarmsV2(@Param("tenantId") UUID tenantId,
                                          @Param("startTime") Long startTime,
                                          @Param("endTime") Long endTime,
                                          @Param("alarmTypes") List<String> alarmTypes,
                                          @Param("alarmSeverities") List<AlarmSeverity> alarmSeverities,
                                          @Param("clearFilterEnabled") boolean clearFilterEnabled,
                                          @Param("clearFilter") boolean clearFilter,
                                          @Param("ackFilterEnabled") boolean ackFilterEnabled,
                                          @Param("ackFilter") boolean ackFilter,
                                          @Param("assigneeId") UUID assigneeId,
                                          @Param("searchText") String searchText,
                                          Pageable pageable);

    @Query(value = "SELECT a " +
            "FROM AlarmInfoEntity a " +
            "WHERE a.tenantId = :tenantId AND a.customerId = :customerId " +
            "AND (:startTime IS NULL OR a.createdTime >= :startTime) " +
            "AND (:endTime IS NULL OR a.createdTime <= :endTime) " +
            "AND ((:clearFilterEnabled) = FALSE OR a.cleared = :clearFilter) " +
            "AND ((:ackFilterEnabled) = FALSE OR a.acknowledged = :ackFilter) " +
            "AND (:assigneeId IS NULL OR a.assigneeId = :assigneeId) " +
            "AND (:searchText IS NULL OR (ilike(a.type, CONCAT('%', :searchText, '%')) = true  " +
            "  OR ilike(a.severity, CONCAT('%', :searchText, '%')) = true " +
            "  OR ilike(a.status, CONCAT('%', :searchText, '%')) = true)) "
            ,
            countQuery = "" +
                    "SELECT count(a) " +
                    "FROM AlarmInfoEntity a " +
                    "WHERE a.tenantId = :tenantId AND a.customerId = :customerId " +
                    "AND (:startTime IS NULL OR a.createdTime >= :startTime) " +
                    "AND (:endTime IS NULL OR a.createdTime <= :endTime) " +
                    "AND ((:clearFilterEnabled) = FALSE OR a.cleared = :clearFilter) " +
                    "AND ((:ackFilterEnabled) = FALSE OR a.acknowledged = :ackFilter) " +
                    "AND (:assigneeId IS NULL OR a.assigneeId = :assigneeId) " +
                    "AND (:searchText IS NULL OR (ilike(a.type, CONCAT('%', :searchText, '%')) = true " +
                    "  OR ilike(a.severity, CONCAT('%', :searchText, '%')) = true  " +
                    "  OR ilike(a.status, CONCAT('%', :searchText, '%')) = true))")
    Page<AlarmInfoEntity> findCustomerAlarms(@Param("tenantId") UUID tenantId,
                                             @Param("customerId") UUID customerId,
                                             @Param("startTime") Long startTime,
                                             @Param("endTime") Long endTime,
                                             @Param("clearFilterEnabled") boolean clearFilterEnabled,
                                             @Param("clearFilter") boolean clearFilter,
                                             @Param("ackFilterEnabled") boolean ackFilterEnabled,
                                             @Param("ackFilter") boolean ackFilter,
                                             @Param("assigneeId") UUID assigneeId,
                                             @Param("searchText") String searchText,
                                             Pageable pageable);

    @Query(value = "SELECT a " +
            "FROM AlarmInfoEntity a " +
            "WHERE a.tenantId = :tenantId AND a.customerId = :customerId " +
            "AND (:startTime IS NULL OR a.createdTime >= :startTime) " +
            "AND (:endTime IS NULL OR a.createdTime <= :endTime) " +
            "AND ((:alarmTypes) IS NULL OR a.type IN (:alarmTypes)) " +
            "AND ((:alarmSeverities) IS NULL OR a.severity IN (:alarmSeverities)) " +
            "AND ((:clearFilterEnabled) = FALSE OR a.cleared = :clearFilter) " +
            "AND ((:ackFilterEnabled) = FALSE OR a.acknowledged = :ackFilter) " +
            "AND (:assigneeId IS NULL OR a.assigneeId = :assigneeId) " +
            "AND (:searchText IS NULL OR (ilike(a.type, CONCAT('%', :searchText, '%')) = true  " +
            "  OR ilike(a.severity, CONCAT('%', :searchText, '%')) = true " +
            "  OR ilike(a.status, CONCAT('%', :searchText, '%')) = true)) "
            ,
            countQuery = "" +
                    "SELECT count(a) " +
                    "FROM AlarmInfoEntity a " +
                    "WHERE a.tenantId = :tenantId AND a.customerId = :customerId " +
                    "AND (:startTime IS NULL OR a.createdTime >= :startTime) " +
                    "AND (:endTime IS NULL OR a.createdTime <= :endTime) " +
                    "AND ((:alarmTypes) IS NULL OR a.type IN (:alarmTypes)) " +
                    "AND ((:alarmSeverities) IS NULL OR a.severity IN (:alarmSeverities)) " +
                    "AND ((:clearFilterEnabled) = FALSE OR a.cleared = :clearFilter) " +
                    "AND ((:ackFilterEnabled) = FALSE OR a.acknowledged = :ackFilter) " +
                    "AND (:assigneeId IS NULL OR a.assigneeId = :assigneeId) " +
                    "AND (:searchText IS NULL OR (ilike(a.type, CONCAT('%', :searchText, '%')) = true " +
                    "  OR ilike(a.severity, CONCAT('%', :searchText, '%')) = true  " +
                    "  OR ilike(a.status, CONCAT('%', :searchText, '%')) = true))")
    Page<AlarmInfoEntity> findCustomerAlarmsV2(@Param("tenantId") UUID tenantId,
                                               @Param("customerId") UUID customerId,
                                               @Param("startTime") Long startTime,
                                               @Param("endTime") Long endTime,
                                               @Param("alarmTypes") List<String> alarmTypes,
                                               @Param("alarmSeverities") List<AlarmSeverity> alarmSeverities,
                                               @Param("clearFilterEnabled") boolean clearFilterEnabled,
                                               @Param("clearFilter") boolean clearFilter,
                                               @Param("ackFilterEnabled") boolean ackFilterEnabled,
                                               @Param("ackFilter") boolean ackFilter,
                                               @Param("assigneeId") UUID assigneeId,
                                               @Param("searchText") String searchText,
                                               Pageable pageable);

    @Query(value = "SELECT a.severity FROM AlarmEntity a " +
            "LEFT JOIN EntityAlarmEntity ea ON a.id = ea.alarmId AND a.originatorId = ea.originatorId " +
            "WHERE a.tenantId = :tenantId " +
            "AND ea.tenantId = :tenantId " +
            "AND ea.entityId = :affectedEntityId " +
            "AND ea.entityType = :affectedEntityType " +
            "AND ((:clearFilterEnabled) = FALSE OR a.cleared = :clearFilter) " +
            "AND ((:ackFilterEnabled) = FALSE OR a.acknowledged = :ackFilter) " +
            "AND (:assigneeId IS NULL OR a.assigneeId = :assigneeId)")
    Set<AlarmSeverity> findAlarmSeverities(@Param("tenantId") UUID tenantId,
                                           @Param("affectedEntityId") UUID affectedEntityId,
                                           @Param("affectedEntityType") String affectedEntityType,
                                           @Param("clearFilterEnabled") boolean clearFilterEnabled,
                                           @Param("clearFilter") boolean clearFilter,
                                           @Param("ackFilterEnabled") boolean ackFilterEnabled,
                                           @Param("ackFilter") boolean ackFilter,
                                           @Param("assigneeId") UUID assigneeId);

    @Query("SELECT COUNT(a) " +
            "FROM AlarmEntity a " +
            "LEFT JOIN EntityAlarmEntity ea ON a.id = ea.alarmId AND a.originatorId = ea.originatorId " +
            "WHERE a.tenantId = :tenantId " +
            "AND ea.tenantId = :tenantId " +
            "AND ea.entityId = :affectedEntityId " +
            "AND ea.entityType = :affectedEntityType " +
            "AND (:startTime IS NULL OR (a.createdTime >= :startTime AND ea.createdTime >= :startTime)) " +
            "AND (:endTime IS NULL OR (a.createdTime <= :endTime AND ea.createdTime <= :endTime)) " +
            "AND ((:typesList) IS NULL OR (a.type IN (:typesList) AND ea.alarmType IN (:typesList))) " +
            "AND ((:severityList) IS NULL OR a.severity IN (:severityList)) " +
            "AND ((:clearFilterEnabled) = FALSE OR a.cleared = :clearFilter) " +
            "AND ((:ackFilterEnabled) = FALSE OR a.acknowledged = :ackFilter)")
    long findAlarmCount(@Param("tenantId") UUID tenantId,
                        @Param("affectedEntityId") UUID affectedEntityId,
                        @Param("affectedEntityType") String affectedEntityType,
                        @Param("startTime") Long startTime,
                        @Param("endTime") Long endTime,
                        @Param("typesList") List<String> typesList,
                        @Param("severityList") List<AlarmSeverity> severityList,
                        @Param("clearFilterEnabled") boolean clearFilterEnabled,
                        @Param("clearFilter") boolean clearFilter,
                        @Param("ackFilterEnabled") boolean ackFilterEnabled,
                        @Param("ackFilter") boolean ackFilter);

    // Projects only (id, originator_id, originator_type, created_time) -- the minimum the TTL cleanup needs to route
    // each delete to the single owning shard of the originator-sharded alarm table -- instead of the whole row, which
    // would drag the details JSON across every shard. The full row is loaded once inside delAlarm (single-shard) for
    // the delete event.
    // using Slice so that count query is not executed (a cross-shard scatter-gather count over the whole expired set
    // on every TTL batch); the consumer only iterates and checks hasNext().
    @Query("SELECT new org.thingsboard.server.common.data.alarm.AlarmRef(a.id, a.originatorId, a.originatorType, a.createdTime) " +
            "FROM AlarmEntity a WHERE a.tenantId = :tenantId AND a.createdTime < :time AND a.endTs < :time")
    Slice<AlarmRef> findExpiredAlarmRefsByTenantId(@Param("time") Long time, @Param("tenantId") UUID tenantId, Pageable pageable);

    @Query(value = "SELECT a FROM AlarmInfoEntity a WHERE a.tenantId = :tenantId AND a.id = :alarmId")
    AlarmInfoEntity findAlarmInfoById(@Param("tenantId") UUID tenantId, @Param("alarmId") UUID alarmId);

    // Single-shard load: originator_id is the Citus distribution column, so filtering on it routes to one shard
    // (the by-id-only findAlarmInfoById fans out across every shard). Used by delAlarm once the caller knows the
    // originator.
    @Query(value = "SELECT a FROM AlarmInfoEntity a WHERE a.tenantId = :tenantId AND a.originatorId = :originatorId AND a.id = :alarmId")
    AlarmInfoEntity findAlarmInfoByOriginatorAndId(@Param("tenantId") UUID tenantId, @Param("originatorId") UUID originatorId, @Param("alarmId") UUID alarmId);

    // Lightweight (id, originator) refs, iterated by a (created_time, id) compound keyset, for every alarm assigned
    // to the user. idx_alarm_tenant_assignee_created_time (tenant_id, assignee_id, created_time) serves the
    // created_time component; the id tiebreaker only orders within equal-created_time groups (a small incremental
    // sort), unlike an id-only cursor which would re-sort the whole assignee set on every page. Carrying the
    // originator lets the deleted-user unassign route each alarm to its single owning shard without a separate
    // per-alarm AlarmInfo lookup.
    // using Slice so that count query is not executed
    @Query("SELECT new org.thingsboard.server.common.data.alarm.AlarmRef(a.id, a.originatorId, a.originatorType, a.createdTime) " +
            "FROM AlarmEntity a WHERE a.tenantId = :tenantId AND a.assigneeId = :assigneeId")
    Slice<AlarmRef> findAlarmRefsByAssigneeId(@Param("tenantId") UUID tenantId,
                                              @Param("assigneeId") UUID assigneeId,
                                              Pageable pageable);

    // using Slice so that count query is not executed
    @Query("SELECT new org.thingsboard.server.common.data.alarm.AlarmRef(a.id, a.originatorId, a.originatorType, a.createdTime) " +
            "FROM AlarmEntity a WHERE a.tenantId = :tenantId AND a.assigneeId = :assigneeId " +
            "AND (a.createdTime > :createdTimeOffset OR (a.createdTime = :createdTimeOffset AND a.id > :idOffset))")
    Slice<AlarmRef> findAlarmRefsByAssigneeId(@Param("tenantId") UUID tenantId,
                                              @Param("assigneeId") UUID assigneeId,
                                              @Param("createdTimeOffset") long createdTimeOffset,
                                              @Param("idOffset") UUID idOffset,
                                              Pageable pageable);

    // using Slice so that count query is not executed
    @Query("SELECT new org.thingsboard.server.common.data.util.TbPair(a.id, a.createdTime) " +
            "FROM AlarmEntity a WHERE a.originatorId = :originatorId " +
            "AND (a.createdTime > :createdTimeOffset OR " +
            "(a.createdTime = :createdTimeOffset AND a.id > :idOffset))")
    Slice<TbPair<UUID, Long>> findAlarmIdsByOriginatorId(@Param("originatorId") UUID originatorId,
                                                         @Param("createdTimeOffset") long createdTimeOffset,
                                                         @Param("idOffset") UUID idOffset,
                                                         Pageable pageable);

    // using Slice so that count query is not executed
    @Query("SELECT new org.thingsboard.server.common.data.util.TbPair(a.id, a.createdTime) " +
            "FROM AlarmEntity a WHERE a.originatorId = :originatorId")
    Slice<TbPair<UUID, Long>> findAlarmIdsByOriginatorId(@Param("originatorId") UUID originatorId,
                                                         Pageable pageable);

    @Query(value = "SELECT create_or_update_active_alarm(:t_id, :c_id, :a_id, :a_created_ts, :a_o_id, :a_o_type, :a_type, :a_severity, " +
            ":a_start_ts, :a_end_ts, :a_details, :a_propagate, :a_propagate_to_owner, :a_propagate_to_owner_hierarchy, " +
            ":a_propagate_to_tenant, :a_propagation_types, :a_creation_enabled)", nativeQuery = true)
    String createOrUpdateActiveAlarm(@Param("t_id") UUID tenantId, @Param("c_id") UUID customerId,
                                     @Param("a_id") UUID alarmId, @Param("a_created_ts") long createdTime,
                                     @Param("a_o_id") UUID originatorId, @Param("a_o_type") int originatorType,
                                     @Param("a_type") String type, @Param("a_severity") String severity,
                                     @Param("a_start_ts") long startTs, @Param("a_end_ts") long endTs, @Param("a_details") String detailsAsString,
                                     @Param("a_propagate") boolean propagate, @Param("a_propagate_to_owner") boolean propagateToOwner,
                                     @Param("a_propagate_to_owner_hierarchy") boolean propagateToOwnerHierarchy,
                                     @Param("a_propagate_to_tenant") boolean propagateToTenant, @Param("a_propagation_types") String propagationTypes,
                                     @Param("a_creation_enabled") boolean alarmCreationEnabled);

    @Query(value = "SELECT update_alarm(:t_id, :a_o_id, :a_id, :a_severity, :a_start_ts, :a_end_ts, :a_details, :a_propagate, :a_propagate_to_owner, " +
            ":a_propagate_to_owner_hierarchy, :a_propagate_to_tenant, :a_propagation_types)", nativeQuery = true)
    String updateAlarm(@Param("t_id") UUID tenantId, @Param("a_o_id") UUID originatorId, @Param("a_id") UUID alarmId,
                       @Param("a_severity") String severity,
                       @Param("a_start_ts") long startTs, @Param("a_end_ts") long endTs, @Param("a_details") String detailsAsString,
                       @Param("a_propagate") boolean propagate, @Param("a_propagate_to_owner") boolean propagateToOwner,
                       @Param("a_propagate_to_owner_hierarchy") boolean propagateToOwnerHierarchy,
                       @Param("a_propagate_to_tenant") boolean propagateToTenant, @Param("a_propagation_types") String propagationTypes);

    @Query(value = "SELECT acknowledge_alarm(:t_id, :a_o_id, :a_id, :a_ts)", nativeQuery = true)
    String acknowledgeAlarm(@Param("t_id") UUID tenantId, @Param("a_o_id") UUID originatorId, @Param("a_id") UUID alarmId, @Param("a_ts") long ts);

    @Query(value = "SELECT clear_alarm(:t_id, :a_o_id, :a_id, :a_ts, :a_details)", nativeQuery = true)
    String clearAlarm(@Param("t_id") UUID tenantId, @Param("a_o_id") UUID originatorId, @Param("a_id") UUID alarmId, @Param("a_ts") long ts, @Param("a_details") String details);

    @Query(value = "SELECT assign_alarm(:t_id, :a_o_id, :a_id, :u_id, :a_ts)", nativeQuery = true)
    String assignAlarm(@Param("t_id") UUID tenantId, @Param("a_o_id") UUID originatorId, @Param("a_id") UUID alarmId, @Param("u_id") UUID userId, @Param("a_ts") long assignTime);

    @Query(value = "SELECT unassign_alarm(:t_id, :a_o_id, :a_id, :a_ts)", nativeQuery = true)
    String unassignAlarm(@Param("t_id") UUID tenantId, @Param("a_o_id") UUID originatorId, @Param("a_id") UUID alarmId, @Param("a_ts") long unassignTime);

    @Query(value = "SELECT at.type FROM alarm_types AS at WHERE at.tenant_id = :tenantId AND at.type ILIKE CONCAT('%', :searchText, '%')", nativeQuery = true)
    Page<String> findTenantAlarmTypes(@Param("tenantId") UUID tenantId, @Param("searchText") String searchText, Pageable pageable);

    @Query(value = "SELECT at.type FROM alarm_types AS at WHERE at.tenant_id = :tenantId ORDER BY at.type LIMIT :limit", nativeQuery = true)
    List<String> findTenantAlarmTypeNames(@Param("tenantId") UUID tenantId, @Param("limit") int limit);

    @Transactional
    @Modifying
    @Query(value = "INSERT INTO alarm_types (tenant_id, type) VALUES (:tenantId, :type) ON CONFLICT (tenant_id, type) DO NOTHING", nativeQuery = true)
    int addAlarmType(@Param("tenantId") UUID tenantId, @Param("type") String type);

    // The candidate types (from :types) that still have at least one alarm for this tenant. A plain multi-shard
    // SELECT on the distributed alarm table (scatter-gather) — legal under Citus. Kept separate from the alarm_types
    // cleanup DELETE below: a single DELETE on the reference alarm_types table that joined the distributed alarm
    // table would be rejected by Citus ("only reference tables may be queried when targeting a reference table with
    // multi shard UPDATE/DELETE queries with multiple tables"), so the existence check and the delete are split.
    @Query(value = "SELECT DISTINCT a.type FROM alarm AS a WHERE a.tenant_id = :tenantId AND a.type IN (:types)", nativeQuery = true)
    Set<String> findExistingAlarmTypes(@Param("tenantId") UUID tenantId, @Param("types") Set<String> types);

    // Pure reference-table DELETE on alarm_types (no distributed join) — legal under Citus. The caller passes the
    // types that have NO remaining alarms (computed via findExistingAlarmTypes), preserving the original
    // delete-only-if-orphaned semantics.
    @Transactional
    @Modifying
    @Query(value = "DELETE FROM alarm_types AS at WHERE at.tenant_id = :tenantId AND at.type IN (:types)", nativeQuery = true)
    int deleteAlarmTypes(@Param("tenantId") UUID tenantId, @Param("types") Set<String> types);

    @Query(value = "SELECT a.id FROM alarm a " +
            "WHERE a.originator_id = :originatorId " +
            "AND (COALESCE(:alarmTypes) IS NULL OR a.type IN (:alarmTypes)) " +
            "AND (COALESCE(:alarmSeverities) IS NULL OR a.severity IN (:alarmSeverities)) " +
            "AND (a.cleared = false) ORDER BY id LIMIT :limit", nativeQuery = true)
    List<UUID> findActiveOriginatorAlarms(@Param("originatorId") UUID originatorId,
                                          @Param("alarmTypes") List<String> alarmTypes,
                                          @Param("alarmSeverities") List<String> alarmSeverities,
                                          int limit);

    Page<AlarmEntity> findByTenantId(UUID tenantId, Pageable pageable);

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM alarm WHERE originator_id = :originatorId AND id = :alarmId", nativeQuery = true)
    void deleteByOriginatorIdAndId(@Param("originatorId") UUID originatorId, @Param("alarmId") UUID alarmId);

    // Bulk unassign: one statement the Citus coordinator fans out to every shard (each filters locally via
    // idx_alarm_tenant_assignee_created_time). Replaces a per-alarm lookup-and-unassign loop. assignee_id is not the
    // distribution column, so this is a multi-shard write; that is fine here -- there is no per-row work to do.
    @Transactional
    @Modifying
    @Query(value = "UPDATE alarm SET assignee_id = NULL, assign_ts = :ts WHERE tenant_id = :tenantId AND assignee_id = :assigneeId", nativeQuery = true)
    int unassignAlarmsByAssignee(@Param("tenantId") UUID tenantId, @Param("assigneeId") UUID assigneeId, @Param("ts") long ts);

}
