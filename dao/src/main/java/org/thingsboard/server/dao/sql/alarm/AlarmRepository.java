/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
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
public interface AlarmRepository extends CrudRepository<AlarmEntity, UUID> {

    @Query("SELECT a FROM AlarmEntity a WHERE a.originatorId = :originatorId AND a.type = :alarmType ORDER BY a.startTs DESC")
    List<AlarmEntity> findLatestByOriginatorAndType(@Param("originatorId") UUID originatorId,
                                                    @Param("alarmType") String alarmType,
                                                    Pageable pageable);

    @Query(value = "SELECT new org.thingsboard.server.dao.model.sql.AlarmInfoEntity(a) FROM AlarmEntity a " +
            "LEFT JOIN RelationEntity re ON a.id = re.toId " +
            "AND re.relationTypeGroup = 'ALARM' " +
            "AND re.toType = 'ALARM' " +
            "AND re.fromId = :affectedEntityId " +
            "AND re.fromType = :affectedEntityType " +
            "WHERE a.tenantId = :tenantId " +
            "AND (a.originatorId = :affectedEntityId or re.fromId IS NOT NULL) " +
            "AND (:startTime IS NULL OR a.createdTime >= :startTime) " +
            "AND (:endTime IS NULL OR a.createdTime <= :endTime) " +
            "AND ((:alarmStatuses) IS NULL OR a.status in (:alarmStatuses)) " +
            "AND (LOWER(a.type) LIKE LOWER(CONCAT(:searchText, '%'))" +
            "OR LOWER(a.severity) LIKE LOWER(CONCAT(:searchText, '%'))" +
            "OR LOWER(a.status) LIKE LOWER(CONCAT(:searchText, '%')))",
            countQuery = "SELECT count(a) FROM AlarmEntity a " +
                    "LEFT JOIN RelationEntity re ON a.id = re.toId " +
                    "AND re.relationTypeGroup = 'ALARM' " +
                    "AND re.toType = 'ALARM' " +
                    "AND re.fromId = :affectedEntityId " +
                    "AND re.fromType = :affectedEntityType " +
                    "WHERE a.tenantId = :tenantId " +
                    "AND (a.originatorId = :affectedEntityId or re.fromId IS NOT NULL) " +
                    "AND (:startTime IS NULL OR a.createdTime >= :startTime) " +
                    "AND (:endTime IS NULL OR a.createdTime <= :endTime) " +
                    "AND ((:alarmStatuses) IS NULL OR a.status in (:alarmStatuses)) " +
                    "AND (LOWER(a.type) LIKE LOWER(CONCAT(:searchText, '%'))" +
                    "OR LOWER(a.severity) LIKE LOWER(CONCAT(:searchText, '%'))" +
                    "OR LOWER(a.status) LIKE LOWER(CONCAT(:searchText, '%')))")
    Page<AlarmInfoEntity> findAlarms(@Param("tenantId") UUID tenantId,
                                     @Param("affectedEntityId") UUID affectedEntityId,
                                     @Param("affectedEntityType") String affectedEntityType,
                                     @Param("startTime") Long startTime,
                                     @Param("endTime") Long endTime,
                                     @Param("alarmStatuses") Set<AlarmStatus> alarmStatuses,
                                     @Param("searchText") String searchText,
                                     Pageable pageable);

    @Query("SELECT alarm.severity FROM AlarmEntity alarm" +
            " WHERE alarm.tenantId = :tenantId" +
            " AND alarm.originatorId = :entityId" +
            " AND ((:status) IS NULL OR alarm.status in (:status))")
    Set<AlarmSeverity> findAlarmSeverities(@Param("tenantId") UUID tenantId,
                                           @Param("entityId") UUID entityId,
                                           @Param("status") Set<AlarmStatus> status);

}
