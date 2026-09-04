// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.sql.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.NotificationStatus;
import org.thingsboard.server.common.data.notification.NotificationType;
import org.thingsboard.server.dao.model.sql.NotificationEntity;

import java.util.Set;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {

    @Query("SELECT n FROM NotificationEntity n WHERE n.deliveryMethod = :deliveryMethod " +
            "AND n.recipientId = :recipientId AND n.status <> :status " +
            "AND (:searchText is NULL OR ilike(n.subject, concat('%', :searchText, '%')) = true " +
            "OR ilike(n.text, concat('%', :searchText, '%')) = true)")
    Page<NotificationEntity> findByDeliveryMethodAndRecipientIdAndStatusNot(@Param("deliveryMethod") NotificationDeliveryMethod deliveryMethod,
                                                                            @Param("recipientId") UUID recipientId,
                                                                            @Param("status") NotificationStatus status,
                                                                            @Param("searchText") String searchText,
                                                                            Pageable pageable);

    @Query("SELECT n FROM NotificationEntity n WHERE n.deliveryMethod = :deliveryMethod " +
            "AND n.recipientId = :recipientId AND n.status <> :status " +
            "AND (n.type IN :types) " +
            "AND (:searchText is NULL OR ilike(n.subject, concat('%', :searchText, '%')) = true " +
            "OR ilike(n.text, concat('%', :searchText, '%')) = true)")
    Page<NotificationEntity> findByDeliveryMethodAndRecipientIdAndTypeInAndStatusNot(@Param("deliveryMethod") NotificationDeliveryMethod deliveryMethod,
                                                                                     @Param("recipientId") UUID recipientId,
                                                                                     @Param("types") Set<NotificationType> types,
                                                                                     @Param("status") NotificationStatus status,
                                                                                     @Param("searchText") String searchText,
                                                                                     Pageable pageable);

    @Query("SELECT n FROM NotificationEntity n WHERE n.deliveryMethod = :deliveryMethod " +
            "AND n.recipientId = :recipientId " +
            "AND (:searchText is NULL OR ilike(n.subject, concat('%', :searchText, '%')) = true " +
            "OR ilike(n.text, concat('%', :searchText, '%')) = true)")
    Page<NotificationEntity> findByDeliveryMethodAndRecipientId(@Param("deliveryMethod") NotificationDeliveryMethod deliveryMethod,
                                                                @Param("recipientId") UUID recipientId,
                                                                @Param("searchText") String searchText,
                                                                Pageable pageable);

    @Modifying
    @Transactional
    @Query("UPDATE NotificationEntity n SET n.status = :status " +
            "WHERE n.id = :id AND n.recipientId = :recipientId AND n.status <> :status")
    int updateStatusByIdAndRecipientId(@Param("id") UUID id,
                                       @Param("recipientId") UUID recipientId,
                                       @Param("status") NotificationStatus status);

    int countByDeliveryMethodAndRecipientIdAndStatusNot(NotificationDeliveryMethod deliveryMethod, UUID recipientId, NotificationStatus status);

    @Transactional
    @Modifying
    @Query("DELETE FROM NotificationEntity n WHERE n.id = :id AND n.recipientId = :recipientId")
    int deleteByIdAndRecipientId(@Param("id") UUID id, @Param("recipientId") UUID recipientId);

    @Transactional
    @Modifying
    @Query("DELETE FROM NotificationEntity n WHERE n.requestId = :requestId")
    void deleteByRequestId(@Param("requestId") UUID requestId);

    @Transactional
    @Modifying
    @Query("DELETE FROM NotificationEntity n WHERE n.recipientId = :recipientId")
    void deleteByRecipientId(@Param("recipientId") UUID recipientId);

    @Modifying
    @Transactional
    @Query("UPDATE NotificationEntity n SET n.status = :status " +
            "WHERE n.deliveryMethod = :deliveryMethod AND n.recipientId = :recipientId AND n.status <> :status")
    int updateStatusByDeliveryMethodAndRecipientIdAndStatusNot(@Param("deliveryMethod") NotificationDeliveryMethod deliveryMethod,
                                                               @Param("recipientId") UUID recipientId,
                                                               @Param("status") NotificationStatus status);

}
