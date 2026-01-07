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
import org.thingsboard.server.common.data.edqs.fields.WidgetsBundleFields;
import org.thingsboard.server.dao.ExportableEntityRepository;
import org.thingsboard.server.dao.model.sql.WidgetsBundleEntity;

import java.util.List;
import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 4/23/2017.
 */
public interface WidgetsBundleRepository extends JpaRepository<WidgetsBundleEntity, UUID>, ExportableEntityRepository<WidgetsBundleEntity> {

    WidgetsBundleEntity findWidgetsBundleByTenantIdAndAlias(UUID tenantId, String alias);

    @Query("SELECT wb FROM WidgetsBundleEntity wb WHERE wb.tenantId = :systemTenantId " +
            "AND (:textSearch is NULL OR ilike(wb.title, CONCAT('%', :textSearch, '%')) = true)")
    Page<WidgetsBundleEntity> findSystemWidgetsBundles(@Param("systemTenantId") UUID systemTenantId,
                                                       @Param("textSearch") String textSearch,
                                                       Pageable pageable);

    @Query(nativeQuery = true,
            value = "SELECT * FROM widgets_bundle wb WHERE wb.tenant_id = :systemTenantId " +
                "AND (:textSearch IS NULL OR wb.title ILIKE CONCAT('%', :textSearch, '%') " +
                "OR wb.description ILIKE CONCAT('%', :textSearch, '%') " +
                "OR wb.id in (SELECT wbw.widgets_bundle_id FROM widgets_bundle_widget wbw, widget_type wtd " +
                "WHERE wtd.id = wbw.widget_type_id " +
                "AND (:textSearch IS NULL OR wtd.name ILIKE CONCAT('%', :textSearch, '%') " +
                "OR wtd.description ILIKE CONCAT('%', :textSearch, '%') " +
                "OR EXISTS (" +
                    "SELECT 1 " +
                    "FROM unnest(wtd.tags) AS currentTag " +
                    "WHERE :textSearch ILIKE '%' || currentTag || '%' " +
                        "AND (length(:textSearch) = length(currentTag) " +
                        "OR :textSearch ILIKE currentTag || ' %' " +
                        "OR :textSearch ILIKE '% ' || currentTag " +
                        "OR :textSearch ILIKE '% ' || currentTag || ' %')" +
                "))))",
            countQuery = "SELECT count(*) FROM widgets_bundle wb WHERE wb.tenant_id = :systemTenantId " +
                "AND (:textSearch IS NULL OR wb.title ILIKE CONCAT('%', :textSearch, '%') " +
                "OR wb.description ILIKE CONCAT('%', :textSearch, '%') " +
                "OR wb.id in (SELECT wbw.widgets_bundle_id FROM widgets_bundle_widget wbw, widget_type wtd " +
                "WHERE wtd.id = wbw.widget_type_id " +
                "AND (:textSearch IS NULL OR wtd.name ILIKE CONCAT('%', :textSearch, '%') " +
                "OR wtd.description ILIKE CONCAT('%', :textSearch, '%') " +
                "OR EXISTS (" +
                    "SELECT 1 " +
                    "FROM unnest(wtd.tags) AS currentTag " +
                    "WHERE :textSearch ILIKE '%' || currentTag || '%' " +
                        "AND (length(:textSearch) = length(currentTag) " +
                        "OR :textSearch ILIKE currentTag || ' %' " +
                        "OR :textSearch ILIKE '% ' || currentTag " +
                        "OR :textSearch ILIKE '% ' || currentTag || ' %')" +
                "))))"
    )
    Page<WidgetsBundleEntity> findSystemWidgetsBundlesFullSearch(@Param("systemTenantId") UUID systemTenantId,
                                                                 @Param("textSearch") String textSearch,
                                                                 Pageable pageable);

    @Query("SELECT wb FROM WidgetsBundleEntity wb WHERE wb.tenantId = :tenantId " +
            "AND (:textSearch IS NULL OR ilike(wb.title, CONCAT('%', :textSearch, '%')) = true)")
    Page<WidgetsBundleEntity> findTenantWidgetsBundlesByTenantId(@Param("tenantId") UUID tenantId,
                                                                 @Param("textSearch") String textSearch,
                                                                 Pageable pageable);

    @Query("SELECT wb FROM WidgetsBundleEntity wb WHERE wb.tenantId IN (:tenantIds) " +
            "AND (:textSearch IS NULL OR ilike(wb.title, CONCAT('%', :textSearch, '%')) = true)")
    Page<WidgetsBundleEntity> findAllTenantWidgetsBundlesByTenantIds(@Param("tenantIds") List<UUID> tenantIds,
                                                                    @Param("textSearch") String textSearch,
                                                                    Pageable pageable);

    @Query(nativeQuery = true,
            value = "SELECT * FROM widgets_bundle wb WHERE wb.tenant_id IN (:tenantIds) " +
                    "AND (:textSearch IS NULL OR wb.title ILIKE CONCAT('%', :textSearch, '%') " +
                    "OR wb.description ILIKE CONCAT('%', :textSearch, '%') " +
                    "OR wb.id in (SELECT wbw.widgets_bundle_id FROM widgets_bundle_widget wbw, widget_type wtd " +
                    "WHERE wtd.id = wbw.widget_type_id " +
                    "AND (:textSearch IS NULL OR wtd.name ILIKE CONCAT('%', :textSearch, '%') " +
                    "OR wtd.description ILIKE CONCAT('%', :textSearch, '%') " +
                    "OR EXISTS (" +
                        "SELECT 1 " +
                        "FROM unnest(wtd.tags) AS currentTag " +
                        "WHERE :textSearch ILIKE '%' || currentTag || '%' " +
                            "AND (length(:textSearch) = length(currentTag) " +
                            "OR :textSearch ILIKE currentTag || ' %' " +
                            "OR :textSearch ILIKE '% ' || currentTag " +
                            "OR :textSearch ILIKE '% ' || currentTag || ' %')" +
                    ")))) " +
                    "ORDER BY CASE WHEN :scadaFirst then wb.scada END DESC, wb.widgets_bundle_order ASC NULLS LAST",
            countQuery = "SELECT count(*) FROM widgets_bundle wb WHERE wb.tenant_id IN (:tenantIds) " +
                    "AND (:textSearch IS NULL OR wb.title ILIKE CONCAT('%', :textSearch, '%') " +
                    "OR wb.description ILIKE CONCAT('%', :textSearch, '%') " +
                    "OR wb.id in (SELECT wbw.widgets_bundle_id FROM widgets_bundle_widget wbw, widget_type wtd " +
                    "WHERE wtd.id = wbw.widget_type_id " +
                    "AND (:textSearch IS NULL OR wtd.name ILIKE CONCAT('%', :textSearch, '%') " +
                    "OR wtd.description ILIKE CONCAT('%', :textSearch, '%') " +
                    "OR EXISTS (" +
                        "SELECT 1 " +
                        "FROM unnest(wtd.tags) AS currentTag " +
                        "WHERE :textSearch ILIKE '%' || currentTag || '%' " +
                            "AND (length(:textSearch) = length(currentTag) " +
                            "OR :textSearch ILIKE currentTag || ' %' " +
                            "OR :textSearch ILIKE '% ' || currentTag " +
                            "OR :textSearch ILIKE '% ' || currentTag || ' %')" +
                    "))))"
    )
    Page<WidgetsBundleEntity> findAllTenantWidgetsBundlesByTenantIdsFullSearch(@Param("tenantIds") List<UUID> tenantIds,
                                                                               @Param("textSearch") String textSearch,
                                                                               @Param("scadaFirst") boolean scadaFirst,
                                                                               Pageable pageable);

    WidgetsBundleEntity findFirstByTenantIdAndTitle(UUID tenantId, String title);

    @Query("SELECT externalId FROM WidgetsBundleEntity WHERE id = :id")
    UUID getExternalIdById(@Param("id") UUID id);

    @Query(nativeQuery = true, value = "SELECT * FROM widgets_bundle wb WHERE wb.tenant_id = :tenantId and wb.image = :imageLink limit :lmt")
    List<WidgetsBundleEntity> findByTenantAndImageUrl(@Param("tenantId") UUID tenantId, @Param("imageLink") String imageLink, @Param("lmt") int lmt);

    @Query(nativeQuery = true, value = "SELECT * FROM widgets_bundle wb WHERE wb.image = :imageLink limit :lmt")
    List<WidgetsBundleEntity> findByImageUrl(@Param("imageLink") String imageLink, @Param("lmt") int lmt);

    @Query("SELECT new org.thingsboard.server.common.data.edqs.fields.WidgetsBundleFields(w.id, w.createdTime, w.tenantId," +
            "w.alias, w.version) FROM WidgetsBundleEntity w WHERE w.id > :id ORDER BY w.id")
    List<WidgetsBundleFields> findNextBatch(@Param("id") UUID id, Limit limit);
}
