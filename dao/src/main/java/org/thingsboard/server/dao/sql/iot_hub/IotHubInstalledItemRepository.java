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
package org.thingsboard.server.dao.sql.iot_hub;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.dao.model.sql.IotHubInstalledItemEntity;

import java.util.List;
import java.util.UUID;

interface IotHubInstalledItemRepository extends JpaRepository<IotHubInstalledItemEntity, UUID> {

    @Query("SELECT DISTINCT item.itemId FROM IotHubInstalledItemEntity item WHERE item.tenantId = :tenantId")
    List<UUID> findInstalledItemIdsByTenantId(@Param("tenantId") UUID tenantId);

    @Query("""
            SELECT item FROM IotHubInstalledItemEntity item
            WHERE item.tenantId = :tenantId
              AND (:itemTypes IS NULL OR item.itemType IN :itemTypes)
              AND (:itemId IS NULL OR item.itemId = :itemId)
              AND (:textSearch IS NULL
                OR ilike(item.itemName, CONCAT('%', :textSearch, '%')) = true
                OR ilike(item.itemType, CONCAT('%', :textSearch, '%')) = true
                OR ilike(item.version, CONCAT('%', :textSearch, '%')) = true)
            """)
    Page<IotHubInstalledItemEntity> findByTenantId(@Param("tenantId") UUID tenantId, @Param("itemTypes") List<String> itemTypes, @Param("itemId") UUID itemId, @Param("textSearch") String textSearch, Pageable pageable);

    @Query("""
            SELECT COUNT(item) FROM IotHubInstalledItemEntity item
            WHERE item.tenantId = :tenantId
              AND (:itemType IS NULL OR item.itemType = :itemType)
            """)
    long countByTenantId(@Param("tenantId") UUID tenantId, @Param("itemType") String itemType);

    @Query("""
            SELECT item.itemId, COUNT(item) FROM IotHubInstalledItemEntity item
            WHERE item.tenantId = :tenantId
              AND item.itemType = :itemType
            GROUP BY item.itemId
            """)
    List<Object[]> findInstalledItemCounts(@Param("tenantId") UUID tenantId, @Param("itemType") String itemType);

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM iot_hub_installed_item WHERE tenant_id = :tenantId", nativeQuery = true)
    void deleteByTenantId(@Param("tenantId") UUID tenantId);

}
