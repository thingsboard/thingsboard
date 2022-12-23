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
package org.thingsboard.server.dao.sql.notification;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.notification.NotificationRequestStatus;
import org.thingsboard.server.dao.model.sql.NotificationRequestEntity;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRequestRepository extends JpaRepository<NotificationRequestEntity, UUID> {

    Page<NotificationRequestEntity> findByTenantId(UUID tenantId, Pageable pageable);

    @Query("SELECT r.id FROM NotificationRequestEntity r WHERE r.status = :status AND r.ruleId = :ruleId")
    List<UUID> findAllIdsByStatusAndRuleId(@Param("status") NotificationRequestStatus status,
                                           @Param("ruleId") UUID ruleId);

    List<NotificationRequestEntity> findAllByRuleIdAndOriginatorEntityTypeAndOriginatorEntityId(UUID ruleId, EntityType originatorEntityType, UUID originatorEntityId);

    Page<NotificationRequestEntity> findAllByStatus(NotificationRequestStatus status, Pageable pageable);

    @Modifying
    @Transactional
    @Query("UPDATE NotificationRequestEntity r SET r.stats = :stats WHERE r.id = :id")
    void updateStatsById(@Param("id") UUID id, @Param("stats") JsonNode stats);

    boolean existsByStatusAndTargetsContaining(NotificationRequestStatus status, String targetIdStr);

    boolean existsByStatusAndTemplateId(NotificationRequestStatus status, UUID templateId);

    int deleteAllByCreatedTimeBefore(long ts);

}
