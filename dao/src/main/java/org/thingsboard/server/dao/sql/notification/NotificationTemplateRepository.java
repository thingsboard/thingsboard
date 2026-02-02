/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
import org.thingsboard.server.common.data.notification.NotificationType;
import org.thingsboard.server.dao.ExportableEntityRepository;
import org.thingsboard.server.dao.model.sql.NotificationTemplateEntity;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplateEntity, UUID>, ExportableEntityRepository<NotificationTemplateEntity> {

    @Query("SELECT t FROM NotificationTemplateEntity t WHERE t.tenantId = :tenantId AND " +
            "t.notificationType IN :notificationTypes " +
            "AND (:searchText is NULL OR ilike(t.name, concat('%', :searchText, '%')) = true " +
            "OR ilike(t.notificationType, concat('%', :searchText, '%')) = true)")
    Page<NotificationTemplateEntity> findByTenantIdAndNotificationTypesAndSearchText(@Param("tenantId") UUID tenantId,
                                                                                     @Param("notificationTypes") List<NotificationType> notificationTypes,
                                                                                     @Param("searchText") String searchText,
                                                                                     Pageable pageable);

    @Query("SELECT count(t) FROM NotificationTemplateEntity t WHERE t.tenantId = :tenantId AND " +
            "t.notificationType IN :notificationTypes")
    int countByTenantIdAndNotificationTypes(@Param("tenantId") UUID tenantId,
                                            @Param("notificationTypes") Collection<NotificationType> notificationTypes);

    @Transactional
    @Modifying
    @Query("DELETE FROM NotificationTemplateEntity t WHERE t.tenantId = :tenantId")
    void deleteByTenantId(@Param("tenantId") UUID tenantId);

    NotificationTemplateEntity findByTenantIdAndName(UUID tenantId, String name);

    Page<NotificationTemplateEntity> findByTenantId(UUID tenantId, Pageable pageable);

    @Query("SELECT externalId FROM NotificationTemplateEntity WHERE id = :id")
    UUID getExternalIdByInternal(@Param("id") UUID internalId);

}
