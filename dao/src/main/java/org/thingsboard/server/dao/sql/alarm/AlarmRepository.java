/**
 * Copyright © 2016-2022 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.sql.alarm;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.dao.model.sql.AlarmEntity;
import org.thingsboard.server.dao.model.sql.AlarmInfoEntity;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 5/21/2017.
 */
public interface AlarmRepository extends JpaRepository<AlarmEntity, UUID> {

    @Query("SELECT a FROM AlarmEntity a WHERE a.originatorId = :originatorId AND a.type = :alarmType ORDER BY a.startTs DESC")
    List<AlarmEntity> findLatestByOriginatorAndType(@Param("originatorId") UUID originatorId,
                                                    @Param("alarmType") String alarmType,
                                                    Pageable pageable);

    @Query(value = "SELECT new org.thingsboard.server.dao.model.sql.AlarmInfoEntity(a) FROM AlarmEntity a " +
            "LEFT JOIN EntityAlarmEntity ea ON a.id = ea.alarmId " +
            "WHERE a.tenantId = :tenantId " +
            "AND ea.tenantId = :tenantId " +
            "AND ea.entityId = :affectedEntityId " +
            "AND ea.entityType = :affectedEntityType " +
            "AND (:startTime IS NULL OR (a.createdTime >= :startTime AND ea.createdTime >= :startTime)) " +
            "AND (:endTime IS NULL OR (a.createdTime <= :endTime AND ea.createdTime <= :endTime)) " +
            "AND ((:alarmStatuses) IS NULL OR a.status in (:alarmStatuses)) " +
            "AND (LOWER(a.type) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
            "  OR LOWER(a.severity) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
            "  OR LOWER(a.status) LIKE LOWER(CONCAT('%', :searchText, '%'))) "
            ,
            countQuery = "" +
                    "SELECT count(a) " + //alarms with relations only
                    "FROM AlarmEntity a " +
                    "LEFT JOIN EntityAlarmEntity ea ON a.id = ea.alarmId " +
                    "WHERE a.tenantId = :tenantId " +
                    "AND ea.tenantId = :tenantId " +
                    "AND ea.entityId = :affectedEntityId " +
                    "AND ea.entityType = :affectedEntityType " +
                    "AND (:startTime IS NULL OR (a.createdTime >= :startTime AND ea.createdTime >= :startTime)) " +
                    "AND (:endTime IS NULL OR (a.createdTime <= :endTime AND ea.createdTime <= :endTime)) " +
                    "AND ((:alarmStatuses) IS NULL OR a.status in (:alarmStatuses)) " +
                    "AND (LOWER(a.type) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
                    "  OR LOWER(a.severity) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
                    "  OR LOWER(a.status) LIKE LOWER(CONCAT('%', :searchText, '%'))) ")
    Page<AlarmInfoEntity> findAlarms(@Param("tenantId") UUID tenantId,
                                     @Param("affectedEntityId") UUID affectedEntityId,
                                     @Param("affectedEntityType") String affectedEntityType,
                                     @Param("startTime") Long startTime,
                                     @Param("endTime") Long endTime,
                                     @Param("alarmStatuses") Set<AlarmStatus> alarmStatuses,
                                     @Param("searchText") String searchText,
                                     Pageable pageable);

    @Query(value = "SELECT new org.thingsboard.server.dao.model.sql.AlarmInfoEntity(a) FROM AlarmEntity a " +
            "WHERE a.tenantId = :tenantId " +
            "AND (:startTime IS NULL OR a.createdTime >= :startTime) " +
            "AND (:endTime IS NULL OR a.createdTime <= :endTime) " +
            "AND ((:alarmStatuses) IS NULL OR a.status in (:alarmStatuses)) " +
            "AND (LOWER(a.type) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
            "  OR LOWER(a.severity) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
            "  OR LOWER(a.status) LIKE LOWER(CONCAT('%', :searchText, '%'))) ",
            countQuery = "" +
                    "SELECT count(a) " +
                    "FROM AlarmEntity a " +
                    "WHERE a.tenantId = :tenantId " +
                    "AND (:startTime IS NULL OR a.createdTime >= :startTime) " +
                    "AND (:endTime IS NULL OR a.createdTime <= :endTime) " +
                    "AND ((:alarmStatuses) IS NULL OR a.status in (:alarmStatuses)) " +
                    "AND (LOWER(a.type) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
                    "  OR LOWER(a.severity) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
                    "  OR LOWER(a.status) LIKE LOWER(CONCAT('%', :searchText, '%'))) ")
    Page<AlarmInfoEntity> findAllAlarms(@Param("tenantId") UUID tenantId,
                                        @Param("startTime") Long startTime,
                                        @Param("endTime") Long endTime,
                                        @Param("alarmStatuses") Set<AlarmStatus> alarmStatuses,
                                        @Param("searchText") String searchText,
                                        Pageable pageable);

    @Query(value = "SELECT new org.thingsboard.server.dao.model.sql.AlarmInfoEntity(a) FROM AlarmEntity a " +
            "WHERE a.tenantId = :tenantId AND a.customerId = :customerId " +
            "AND (:startTime IS NULL OR a.createdTime >= :startTime) " +
            "AND (:endTime IS NULL OR a.createdTime <= :endTime) " +
            "AND ((:alarmStatuses) IS NULL OR a.status in (:alarmStatuses)) " +
            "AND (LOWER(a.type) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
            "  OR LOWER(a.severity) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
            "  OR LOWER(a.status) LIKE LOWER(CONCAT('%', :searchText, '%'))) "
            ,
            countQuery = "" +
                    "SELECT count(a) " +
                    "FROM AlarmEntity a " +
                    "WHERE a.tenantId = :tenantId AND a.customerId = :customerId " +
                    "AND (:startTime IS NULL OR a.createdTime >= :startTime) " +
                    "AND (:endTime IS NULL OR a.createdTime <= :endTime) " +
                    "AND ((:alarmStatuses) IS NULL OR a.status in (:alarmStatuses)) " +
                    "AND (LOWER(a.type) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
                    "  OR LOWER(a.severity) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
                    "  OR LOWER(a.status) LIKE LOWER(CONCAT('%', :searchText, '%'))) ")
    Page<AlarmInfoEntity> findCustomerAlarms(@Param("tenantId") UUID tenantId,
                                             @Param("customerId") UUID customerId,
                                             @Param("startTime") Long startTime,
                                             @Param("endTime") Long endTime,
                                             @Param("alarmStatuses") Set<AlarmStatus> alarmStatuses,
                                             @Param("searchText") String searchText,
                                             Pageable pageable);

    @Query(value = "SELECT a.severity FROM AlarmEntity a " +
            "LEFT JOIN EntityAlarmEntity ea ON a.id = ea.alarmId " +
            "WHERE a.tenantId = :tenantId " +
            "AND ea.tenantId = :tenantId " +
            "AND ea.entityId = :affectedEntityId " +
            "AND ea.entityType = :affectedEntityType " +
            "AND ((:alarmStatuses) IS NULL OR a.status in (:alarmStatuses))")
    Set<AlarmSeverity> findAlarmSeverities(@Param("tenantId") UUID tenantId,
                                           @Param("affectedEntityId") UUID affectedEntityId,
                                           @Param("affectedEntityType") String affectedEntityType,
                                           @Param("alarmStatuses") Set<AlarmStatus> alarmStatuses);

    @Query("SELECT a.id FROM AlarmEntity a WHERE a.tenantId = :tenantId AND a.createdTime < :time AND a.endTs < :time")
    Page<UUID> findAlarmsIdsByEndTsBeforeAndTenantId(@Param("time") Long time, @Param("tenantId") UUID tenantId, Pageable pageable);

}
