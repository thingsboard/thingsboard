/**
 * Copyright © 2016-2023 The Thingsboard Authors
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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.notification.NotificationStatus;
import org.thingsboard.server.dao.model.sql.NotificationEntity;

import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {

    @Query("SELECT n FROM NotificationEntity n WHERE n.recipientId = :recipientId AND n.status <> :status " +
            "AND (:searchText = '' OR lower(n.subject) LIKE lower(concat('%', :searchText, '%')) " +
            "OR lower(n.text) LIKE lower(concat('%', :searchText, '%')))")
    Page<NotificationEntity> findByRecipientIdAndStatusNot(@Param("recipientId") UUID recipientId,
                                                           @Param("status") NotificationStatus status,
                                                           @Param("searchText") String searchText,
                                                           Pageable pageable);

    @Query("SELECT n FROM NotificationEntity n WHERE n.recipientId = :recipientId " +
            "AND (:searchText = '' OR lower(n.subject) LIKE lower(concat('%', :searchText, '%')) " +
            "OR lower(n.text) LIKE lower(concat('%', :searchText, '%')))")
    Page<NotificationEntity> findByRecipientId(@Param("recipientId") UUID recipientId,
                                               @Param("searchText") String searchText,
                                               Pageable pageable);

    @Modifying
    @Transactional
    @Query("UPDATE NotificationEntity n SET n.status = :status " +
            "WHERE n.id = :id AND n.recipientId = :recipientId AND n.status <> :status")
    int updateStatusByIdAndRecipientId(@Param("id") UUID id,
                                       @Param("recipientId") UUID recipientId,
                                       @Param("status") NotificationStatus status);

    int countByRecipientIdAndStatusNot(UUID recipientId, NotificationStatus status);

    Page<NotificationEntity> findByRequestId(UUID requestId, Pageable pageable);

    @Transactional
    int deleteByIdAndRecipientId(UUID id, UUID recipientId);

    @Modifying
    @Transactional
    @Query("UPDATE NotificationEntity n SET n.status = :status WHERE n.recipientId = :recipientId")
    int updateStatusByRecipientId(@Param("recipientId") UUID recipientId,
                                  @Param("status") NotificationStatus status);

}
