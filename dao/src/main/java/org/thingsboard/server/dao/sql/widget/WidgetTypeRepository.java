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
package org.thingsboard.server.dao.sql.widget;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.edqs.fields.WidgetTypeFields;
import org.thingsboard.server.dao.ExportableEntityRepository;
import org.thingsboard.server.dao.model.sql.WidgetTypeDetailsEntity;
import org.thingsboard.server.dao.model.sql.WidgetTypeEntity;
import org.thingsboard.server.dao.model.sql.WidgetTypeIdFqnEntity;

import java.util.List;
import java.util.UUID;

public interface WidgetTypeRepository extends JpaRepository<WidgetTypeDetailsEntity, UUID>, ExportableEntityRepository<WidgetTypeDetailsEntity> {

    @Query("SELECT wt FROM WidgetTypeEntity wt WHERE wt.id = :widgetTypeId")
    WidgetTypeEntity findWidgetTypeById(@Param("widgetTypeId") UUID widgetTypeId);

    boolean existsByTenantIdAndId(UUID tenantId, UUID id);

    @Query("SELECT wtd FROM WidgetTypeDetailsEntity wtd WHERE wtd.tenantId = :tenantId " +
            "AND (:textSearch IS NULL OR ilike(wtd.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<WidgetTypeDetailsEntity> findTenantWidgetTypeDetailsByTenantId(@Param("tenantId") UUID tenantId,
                                                                        @Param("textSearch") String textSearch,
                                                                        Pageable pageable);

    @Query("SELECT wt FROM WidgetTypeEntity wt, WidgetsBundleWidgetEntity wbw " +
            "WHERE wbw.widgetsBundleId = :widgetsBundleId " +
            "AND wbw.widgetTypeId = wt.id ORDER BY wbw.widgetTypeOrder")
    List<WidgetTypeEntity> findWidgetTypesByWidgetsBundleId(@Param("widgetsBundleId") UUID widgetsBundleId);

    @Query("SELECT wtd FROM WidgetTypeDetailsEntity wtd, WidgetsBundleWidgetEntity wbw " +
            "WHERE wbw.widgetsBundleId = :widgetsBundleId " +
            "AND wbw.widgetTypeId = wtd.id ORDER BY wbw.widgetTypeOrder")
    List<WidgetTypeDetailsEntity> findWidgetTypesDetailsByWidgetsBundleId(@Param("widgetsBundleId") UUID widgetsBundleId);


    @Query("SELECT wtd.fqn FROM WidgetTypeDetailsEntity wtd, WidgetsBundleWidgetEntity wbw " +
            "WHERE wbw.widgetsBundleId = :widgetsBundleId " +
            "AND wbw.widgetTypeId = wtd.id ORDER BY wbw.widgetTypeOrder")
    List<String> findWidgetFqnsByWidgetsBundleId(@Param("widgetsBundleId") UUID widgetsBundleId);

    @Query("SELECT new org.thingsboard.server.dao.model.sql.WidgetTypeIdFqnEntity(wtd.id, wtd.fqn) FROM WidgetTypeDetailsEntity wtd " +
            "WHERE wtd.tenantId = :tenantId " +
            "AND wtd.fqn IN (:widgetFqns)")
    List<WidgetTypeIdFqnEntity> findWidgetTypeIdsByTenantIdAndFqns(@Param("tenantId") UUID tenantId, @Param("widgetFqns") List<String> widgetFqns);

    @Query("SELECT wt FROM WidgetTypeEntity wt " +
            "WHERE wt.tenantId = :tenantId AND wt.fqn = :fqn")
    WidgetTypeEntity findWidgetTypeByTenantIdAndFqn(@Param("tenantId") UUID tenantId,
                                                    @Param("fqn") String fqn);

    WidgetTypeDetailsEntity findByTenantIdAndFqn(UUID tenantId, String fqn);

    @Query("SELECT externalId FROM WidgetTypeDetailsEntity WHERE id = :id")
    UUID getExternalIdById(@Param("id") UUID id);

    @Query("SELECT w.id FROM WidgetTypeDetailsEntity w")
    Page<UUID> findAllIds(Pageable pageable);

    @Query("SELECT w.id FROM WidgetTypeDetailsEntity w WHERE w.tenantId = :tenantId")
    Page<UUID> findIdsByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT new org.thingsboard.server.common.data.edqs.fields.WidgetTypeFields(w.id, w.createdTime, w.tenantId," +
            "w.name, w.version) FROM WidgetTypeEntity w WHERE w.id > :id ORDER BY w.id")
    List<WidgetTypeFields> findNextBatch(@Param("id") UUID id, Limit limit);
}
