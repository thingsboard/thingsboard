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
import org.thingsboard.server.dao.model.sql.AlarmCommentEntity;
import org.thingsboard.server.dao.model.sql.AlarmCommentInfoEntity;

import java.util.List;
import java.util.UUID;

public interface AlarmCommentRepository extends JpaRepository<AlarmCommentEntity, UUID> {

    @Query(value = "SELECT new org.thingsboard.server.dao.model.sql.AlarmCommentInfoEntity(a, u.firstName, u.lastName, u.email) FROM AlarmCommentEntity a " +
            "LEFT JOIN UserEntity u on u.id = a.userId " +
            "WHERE a.alarmId = :alarmId ",
            countQuery = "" +
                    "SELECT count(a) " +
                    "FROM AlarmCommentEntity a " +
                    "WHERE a.alarmId = :alarmId ")
    Page<AlarmCommentInfoEntity> findAllByAlarmId(@Param("alarmId") UUID alarmId,
                                                  Pageable pageable);

    @Query("SELECT c FROM AlarmCommentEntity c WHERE c.userId IN (SELECT u.id FROM UserEntity u WHERE u.tenantId = :tenantId)")
    Page<AlarmCommentEntity> findByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Transactional
    @Modifying
    @Query("DELETE FROM AlarmCommentEntity a WHERE a.alarmId = :alarmId")
    int deleteByAlarmId(@Param("alarmId") UUID alarmId);

    @Transactional
    @Modifying
    @Query("DELETE FROM AlarmCommentEntity a WHERE a.alarmId IN :alarmIds")
    int deleteByAlarmIdIn(@Param("alarmIds") List<UUID> alarmIds);

}
