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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.dao.model.sql.WidgetTypeInfoEntity;

import java.util.List;
import java.util.UUID;

public interface WidgetTypeInfoRepository extends JpaRepository<WidgetTypeInfoEntity, UUID>  {

    @Query(nativeQuery = true,
            value = "SELECT * FROM widget_type_info_view wti WHERE wti.tenant_id = :systemTenantId " +
                    "AND ((:deprecatedFilterEnabled) IS FALSE OR wti.deprecated = :deprecatedFilter) " +
                    "AND ((:widgetTypesEmpty) IS TRUE OR wti.widget_type IN (:widgetTypes)) " +
                    "AND (wti.name ILIKE CONCAT('%', :searchText, '%') " +
                    "OR ((:fullSearch) IS TRUE AND (wti.description ILIKE CONCAT('%', :searchText, '%') " +
                    "OR EXISTS (" +
                        "SELECT 1 " +
                        "FROM unnest(wti.tags) AS currentTag " +
                        "WHERE :searchText ILIKE '%' || currentTag || '%' " +
                            "AND (length(:searchText) = length(currentTag) " +
                            "OR :searchText ILIKE currentTag || ' %' " +
                            "OR :searchText ILIKE '% ' || currentTag " +
                            "OR :searchText ILIKE '% ' || currentTag || ' %')" +
                    ")))) " +
                    "ORDER BY CASE WHEN :scadaFirst then wti.scada END DESC",
            countQuery = "SELECT count(*) FROM widget_type_info_view wti WHERE wti.tenant_id = :systemTenantId " +
                    "AND ((:deprecatedFilterEnabled) IS FALSE OR wti.deprecated = :deprecatedFilter) " +
                    "AND ((:widgetTypesEmpty) IS TRUE OR wti.widget_type IN (:widgetTypes)) " +
                    "AND (wti.name ILIKE CONCAT('%', :searchText, '%') " +
                    "OR ((:fullSearch) IS TRUE AND (wti.description ILIKE CONCAT('%', :searchText, '%') " +
                    "OR EXISTS (" +
                        "SELECT 1 " +
                        "FROM unnest(wti.tags) AS currentTag " +
                        "WHERE :searchText ILIKE '%' || currentTag || '%' " +
                            "AND (length(:searchText) = length(currentTag) " +
                            "OR :searchText ILIKE currentTag || ' %' " +
                            "OR :searchText ILIKE '% ' || currentTag " +
                            "OR :searchText ILIKE '% ' || currentTag || ' %')" +
                    "))))"
    )
    Page<WidgetTypeInfoEntity> findSystemWidgetTypes(@Param("systemTenantId") UUID systemTenantId,
                                                          @Param("searchText") String searchText,
                                                          @Param("fullSearch") boolean fullSearch,
                                                          @Param("deprecatedFilterEnabled") boolean deprecatedFilterEnabled,
                                                          @Param("deprecatedFilter") boolean deprecatedFilter,
                                                          @Param("widgetTypesEmpty") boolean widgetTypesEmpty,
                                                          @Param("widgetTypes") List<String> widgetTypes,
                                                          @Param("scadaFirst") boolean scadaFirst,
                                                          Pageable pageable);

    @Query(nativeQuery = true,
            value = "SELECT * FROM widget_type_info_view wti WHERE wti.tenant_id IN (:tenantId, :nullTenantId) " +
                    "AND ((:deprecatedFilterEnabled) IS FALSE OR wti.deprecated = :deprecatedFilter) " +
                    "AND ((:widgetTypesEmpty) IS TRUE OR wti.widget_type IN (:widgetTypes)) " +
                    "AND (wti.name ILIKE CONCAT('%', :searchText, '%') " +
                    "OR ((:fullSearch) IS TRUE AND (wti.description ILIKE CONCAT('%', :searchText, '%') " +
                    "OR EXISTS (" +
                        "SELECT 1 " +
                        "FROM unnest(wti.tags) AS currentTag " +
                        "WHERE :searchText ILIKE '%' || currentTag || '%' " +
                            "AND (length(:searchText) = length(currentTag) " +
                            "OR :searchText ILIKE currentTag || ' %' " +
                            "OR :searchText ILIKE '% ' || currentTag " +
                            "OR :searchText ILIKE '% ' || currentTag || ' %')" +
                    ")))) " +
                    "ORDER BY CASE WHEN :scadaFirst then wti.scada END DESC",
            countQuery = "SELECT count(*) FROM widget_type_info_view wti WHERE wti.tenant_id IN (:tenantId, :nullTenantId) " +
                    "AND ((:deprecatedFilterEnabled) IS FALSE OR wti.deprecated = :deprecatedFilter) " +
                    "AND ((:widgetTypesEmpty) IS TRUE OR wti.widget_type IN (:widgetTypes)) " +
                    "AND (wti.name ILIKE CONCAT('%', :searchText, '%') " +
                    "OR ((:fullSearch) IS TRUE AND (wti.description ILIKE CONCAT('%', :searchText, '%') " +
                    "OR EXISTS (" +
                        "SELECT 1 " +
                        "FROM unnest(wti.tags) AS currentTag " +
                        "WHERE :searchText ILIKE '%' || currentTag || '%' " +
                            "AND (length(:searchText) = length(currentTag) " +
                            "OR :searchText ILIKE currentTag || ' %' " +
                            "OR :searchText ILIKE '% ' || currentTag " +
                            "OR :searchText ILIKE '% ' || currentTag || ' %')" +
                    "))))"
    )
    Page<WidgetTypeInfoEntity> findAllTenantWidgetTypesByTenantId(@Param("tenantId") UUID tenantId,
                                                                  @Param("nullTenantId") UUID nullTenantId,
                                                                  @Param("searchText") String searchText,
                                                                  @Param("fullSearch") boolean fullSearch,
                                                                  @Param("deprecatedFilterEnabled") boolean deprecatedFilterEnabled,
                                                                  @Param("deprecatedFilter") boolean deprecatedFilter,
                                                                  @Param("widgetTypesEmpty") boolean widgetTypesEmpty,
                                                                  @Param("widgetTypes") List<String> widgetTypes,
                                                                  @Param("scadaFirst") boolean scadaFirst,
                                                                  Pageable pageable);

    @Query(nativeQuery = true,
            value = "SELECT * FROM widget_type_info_view wti WHERE wti.tenant_id = :tenantId " +
                    "AND ((:deprecatedFilterEnabled) IS FALSE OR wti.deprecated = :deprecatedFilter) " +
                    "AND ((:widgetTypesEmpty) IS TRUE OR wti.widget_type IN (:widgetTypes)) " +
                    "AND (wti.name ILIKE CONCAT('%', :searchText, '%') " +
                    "OR ((:fullSearch) IS TRUE AND (wti.description ILIKE CONCAT('%', :searchText, '%') " +
                    "OR EXISTS (" +
                        "SELECT 1 " +
                        "FROM unnest(wti.tags) AS currentTag " +
                        "WHERE :searchText ILIKE '%' || currentTag || '%' " +
                            "AND (length(:searchText) = length(currentTag) " +
                            "OR :searchText ILIKE currentTag || ' %' " +
                            "OR :searchText ILIKE '% ' || currentTag " +
                            "OR :searchText ILIKE '% ' || currentTag || ' %')" +
                    ")))) " +
                    "ORDER BY CASE WHEN :scadaFirst then wti.scada END DESC",
            countQuery = "SELECT count(*) FROM widget_type_info_view wti WHERE wti.tenant_id = :tenantId " +
                    "AND ((:deprecatedFilterEnabled) IS FALSE OR wti.deprecated = :deprecatedFilter) " +
                    "AND ((:widgetTypesEmpty) IS TRUE OR wti.widget_type IN (:widgetTypes)) " +
                    "AND (wti.name ILIKE CONCAT('%', :searchText, '%') " +
                    "OR ((:fullSearch) IS TRUE AND (wti.description ILIKE CONCAT('%', :searchText, '%') " +
                    "OR EXISTS (" +
                        "SELECT 1 " +
                        "FROM unnest(wti.tags) AS currentTag " +
                        "WHERE :searchText ILIKE '%' || currentTag || '%' " +
                            "AND (length(:searchText) = length(currentTag) " +
                            "OR :searchText ILIKE currentTag || ' %' " +
                            "OR :searchText ILIKE '% ' || currentTag " +
                            "OR :searchText ILIKE '% ' || currentTag || ' %')" +
                    "))))"
    )
    Page<WidgetTypeInfoEntity> findTenantWidgetTypesByTenantId(@Param("tenantId") UUID tenantId,
                                                               @Param("searchText") String searchText,
                                                               @Param("fullSearch") boolean fullSearch,
                                                               @Param("deprecatedFilterEnabled") boolean deprecatedFilterEnabled,
                                                               @Param("deprecatedFilter") boolean deprecatedFilter,
                                                               @Param("widgetTypesEmpty") boolean widgetTypesEmpty,
                                                               @Param("widgetTypes") List<String> widgetTypes,
                                                               @Param("scadaFirst") boolean scadaFirst,
                                                               Pageable pageable);

    @Query("SELECT wti FROM WidgetTypeInfoEntity wti, WidgetsBundleWidgetEntity wbw " +
            "WHERE wbw.widgetsBundleId = :widgetsBundleId " +
            "AND wbw.widgetTypeId = wti.id ORDER BY wbw.widgetTypeOrder")
    List<WidgetTypeInfoEntity> findWidgetTypesInfosByWidgetsBundleId(@Param("widgetsBundleId") UUID widgetsBundleId);

    @Query(nativeQuery = true,
            value = "SELECT * FROM widget_type_info_view wti, widgets_bundle_widget wbw " +
                    "WHERE wbw.widgets_bundle_id = :widgetsBundleId " +
                    "AND wbw.widget_type_id = wti.id " +
                    "AND ((:deprecatedFilterEnabled) IS FALSE OR wti.deprecated = :deprecatedFilter) " +
                    "AND ((:widgetTypesEmpty) IS TRUE OR wti.widget_type IN (:widgetTypes)) " +
                    "AND (wti.name ILIKE CONCAT('%', :searchText, '%') " +
                    "OR ((:fullSearch) IS TRUE AND (wti.description ILIKE CONCAT('%', :searchText, '%') " +
                    "OR EXISTS (" +
                        "SELECT 1 " +
                        "FROM unnest(wti.tags) AS currentTag " +
                        "WHERE :searchText ILIKE '%' || currentTag || '%' " +
                            "AND (length(:searchText) = length(currentTag) " +
                            "OR :searchText ILIKE currentTag || ' %' " +
                            "OR :searchText ILIKE '% ' || currentTag " +
                            "OR :searchText ILIKE '% ' || currentTag || ' %')" +
                    ")))) " +
                    "ORDER BY wbw.widget_type_order",
            countQuery = "SELECT count(*) FROM widget_type_info_view wti, widgets_bundle_widget wbw " +
                    "WHERE wbw.widgets_bundle_id = :widgetsBundleId " +
                    "AND wbw.widget_type_id = wti.id " +
                    "AND ((:deprecatedFilterEnabled) IS FALSE OR wti.deprecated = :deprecatedFilter) " +
                    "AND ((:widgetTypesEmpty) IS TRUE OR wti.widget_type IN (:widgetTypes)) " +
                    "AND (wti.name ILIKE CONCAT('%', :searchText, '%') " +
                    "OR ((:fullSearch) IS TRUE AND (wti.description ILIKE CONCAT('%', :searchText, '%') " +
                    "OR EXISTS (" +
                        "SELECT 1 " +
                        "FROM unnest(wti.tags) AS currentTag " +
                        "WHERE :searchText ILIKE '%' || currentTag || '%' " +
                            "AND (length(:searchText) = length(currentTag) " +
                            "OR :searchText ILIKE currentTag || ' %' " +
                            "OR :searchText ILIKE '% ' || currentTag " +
                            "OR :searchText ILIKE '% ' || currentTag || ' %')" +
                    "))))"
    )
    Page<WidgetTypeInfoEntity> findWidgetTypesInfosByWidgetsBundleId(@Param("widgetsBundleId") UUID widgetsBundleId,
                                                                     @Param("searchText") String searchText,
                                                                     @Param("fullSearch") boolean fullSearch,
                                                                     @Param("deprecatedFilterEnabled") boolean deprecatedFilterEnabled,
                                                                     @Param("deprecatedFilter") boolean deprecatedFilter,
                                                                     @Param("widgetTypesEmpty") boolean widgetTypesEmpty,
                                                                     @Param("widgetTypes") List<String> widgetTypes,
                                                                     Pageable pageable);


    @Query(nativeQuery = true,
            value = "SELECT * FROM widget_type_info_view wti WHERE wti.id IN " +
                    "(select id from widget_type where tenant_id = :tenantId " +
                    "and (image = :imageLink or descriptor ILIKE CONCAT('%\"', :imageLink, '\"%')) limit :limit)"
    )
    List<WidgetTypeInfoEntity> findByTenantAndImageUrl(@Param("tenantId") UUID tenantId, @Param("imageLink") String imageLink, @Param("limit") int limit);

    @Query(nativeQuery = true,
            value = "SELECT * FROM widget_type_info_view wti WHERE wti.id IN " +
                    "(select id from widget_type where image = :imageLink or descriptor ILIKE CONCAT('%', :imageLink, '%') limit :limit)"
    )
    List<WidgetTypeInfoEntity> findByImageUrl(@Param("imageLink") String imageLink, @Param("limit") int limit);

    @Query("SELECT new org.thingsboard.server.common.data.EntityInfo(w.id, 'WIDGET_TYPE', w.name) " +
            "FROM WidgetTypeEntity w WHERE w.tenantId = :tenantId AND ilike(cast(w.descriptor as string), CONCAT('%', :link, '%')) = true")
    List<EntityInfo> findWidgetTypeInfosByTenantIdAndResourceLink(@Param("tenantId") UUID tenantId,
                                                                  @Param("link") String link,
                                                                  Pageable pageable);

    @Query("SELECT new org.thingsboard.server.common.data.EntityInfo(w.id, 'WIDGET_TYPE', w.name) " +
            "FROM WidgetTypeEntity w WHERE ilike(cast(w.descriptor as string), CONCAT('%', :link, '%')) = true")
    List<EntityInfo> findWidgetTypeInfosByResourceLink(@Param("link") String link,
                                                       Pageable pageable);
}
