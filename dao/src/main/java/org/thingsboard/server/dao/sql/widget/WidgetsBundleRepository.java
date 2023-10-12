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
package org.thingsboard.server.dao.sql.widget;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.ExportableEntityRepository;
import org.thingsboard.server.dao.model.sql.WidgetsBundleEntity;

import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 4/23/2017.
 */
public interface WidgetsBundleRepository extends JpaRepository<WidgetsBundleEntity, UUID>, ExportableEntityRepository<WidgetsBundleEntity> {

    WidgetsBundleEntity findWidgetsBundleByTenantIdAndAlias(UUID tenantId, String alias);

    @Query("SELECT wb FROM WidgetsBundleEntity wb WHERE wb.tenantId = :systemTenantId " +
            "AND LOWER(wb.title) LIKE LOWER(CONCAT('%', :textSearch, '%'))")
    Page<WidgetsBundleEntity> findSystemWidgetsBundles(@Param("systemTenantId") UUID systemTenantId,
                                                       @Param("textSearch") String textSearch,
                                                       Pageable pageable);

    @Query(nativeQuery = true,
            value = "SELECT * FROM widgets_bundle wb WHERE wb.tenant_id = :systemTenantId " +
                "AND (wb.title ILIKE CONCAT('%', :textSearch, '%') " +
                "OR wb.description ILIKE CONCAT('%', :textSearch, '%') " +
                "OR wb.id in (SELECT wbw.widgets_bundle_id FROM widgets_bundle_widget wbw, widget_type wtd " +
                "WHERE wtd.id = wbw.widget_type_id " +
                "AND (wtd.name ILIKE CONCAT('%', :textSearch, '%') " +
                "OR wtd.description ILIKE CONCAT('%', :textSearch, '%') " +
                "OR lower(wtd.tags\\:\\:text)\\:\\:text[] && string_to_array(lower(:textSearch), ' '))))",
            countQuery = "SELECT count(*) FROM widgets_bundle wb WHERE wb.tenant_id = :systemTenantId " +
                "AND (wb.title ILIKE CONCAT('%', :textSearch, '%') " +
                "OR wb.description ILIKE CONCAT('%', :textSearch, '%') " +
                "OR wb.id in (SELECT wbw.widgets_bundle_id FROM widgets_bundle_widget wbw, widget_type wtd " +
                "WHERE wtd.id = wbw.widget_type_id " +
                "AND (wtd.name ILIKE CONCAT('%', :textSearch, '%') " +
                "OR wtd.description ILIKE CONCAT('%', :textSearch, '%') " +
                "OR lower(wtd.tags\\:\\:text)\\:\\:text[] && string_to_array(lower(:textSearch), ' '))))"
    )
    Page<WidgetsBundleEntity> findSystemWidgetsBundlesFullSearch(@Param("systemTenantId") UUID systemTenantId,
                                                                 @Param("textSearch") String textSearch,
                                                                 Pageable pageable);

    @Query("SELECT wb FROM WidgetsBundleEntity wb WHERE wb.tenantId = :tenantId " +
            "AND LOWER(wb.title) LIKE LOWER(CONCAT('%', :textSearch, '%'))")
    Page<WidgetsBundleEntity> findTenantWidgetsBundlesByTenantId(@Param("tenantId") UUID tenantId,
                                                                 @Param("textSearch") String textSearch,
                                                                 Pageable pageable);

    @Query("SELECT wb FROM WidgetsBundleEntity wb WHERE wb.tenantId IN (:tenantId, :nullTenantId) " +
            "AND LOWER(wb.title) LIKE LOWER(CONCAT('%', :textSearch, '%'))")
    Page<WidgetsBundleEntity> findAllTenantWidgetsBundlesByTenantId(@Param("tenantId") UUID tenantId,
                                                                    @Param("nullTenantId") UUID nullTenantId,
                                                                    @Param("textSearch") String textSearch,
                                                                    Pageable pageable);

    @Query(nativeQuery = true,
            value = "SELECT * FROM widgets_bundle wb WHERE wb.tenant_id IN (:tenantId, :nullTenantId) " +
                    "AND (wb.title ILIKE CONCAT('%', :textSearch, '%') " +
                    "OR wb.description ILIKE CONCAT('%', :textSearch, '%') " +
                    "OR wb.id in (SELECT wbw.widgets_bundle_id FROM widgets_bundle_widget wbw, widget_type wtd " +
                    "WHERE wtd.id = wbw.widget_type_id " +
                    "AND (wtd.name ILIKE CONCAT('%', :textSearch, '%') " +
                    "OR wtd.description ILIKE CONCAT('%', :textSearch, '%') " +
                    "OR lower(wtd.tags\\:\\:text)\\:\\:text[] && string_to_array(lower(:textSearch), ' '))))",
            countQuery = "SELECT count(*) FROM widgets_bundle wb WHERE wb.tenant_id IN (:tenantId, :nullTenantId) " +
                    "AND (wb.title ILIKE CONCAT('%', :textSearch, '%') " +
                    "OR wb.description ILIKE CONCAT('%', :textSearch, '%') " +
                    "OR wb.id in (SELECT wbw.widgets_bundle_id FROM widgets_bundle_widget wbw, widget_type wtd " +
                    "WHERE wtd.id = wbw.widget_type_id " +
                    "AND (wtd.name ILIKE CONCAT('%', :textSearch, '%') " +
                    "OR wtd.description ILIKE CONCAT('%', :textSearch, '%') " +
                    "OR lower(wtd.tags\\:\\:text)\\:\\:text[] && string_to_array(lower(:textSearch), ' '))))"
    )
    Page<WidgetsBundleEntity> findAllTenantWidgetsBundlesByTenantIdFullSearch(@Param("tenantId") UUID tenantId,
                                                                              @Param("nullTenantId") UUID nullTenantId,
                                                                              @Param("textSearch") String textSearch,
                                                                              Pageable pageable);

    WidgetsBundleEntity findFirstByTenantIdAndTitle(UUID tenantId, String title);

    @Query("SELECT externalId FROM WidgetsBundleEntity WHERE id = :id")
    UUID getExternalIdById(@Param("id") UUID id);

}
