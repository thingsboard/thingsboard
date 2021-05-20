/**
 * Copyright © 2016-2021 The Thingsboard Authors
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

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.WidgetTypeDetailsEntity;
import org.thingsboard.server.dao.model.sql.WidgetTypeEntity;
import org.thingsboard.server.dao.model.sql.WidgetTypeInfoEntity;

import java.util.List;
import java.util.UUID;

public interface WidgetTypeRepository extends CrudRepository<WidgetTypeDetailsEntity, UUID> {

    @Query("SELECT wt FROM WidgetTypeEntity wt WHERE wt.id = :widgetTypeId")
    WidgetTypeEntity findWidgetTypeById(@Param("widgetTypeId") UUID widgetTypeId);

    @Query("SELECT wt FROM WidgetTypeEntity wt WHERE wt.tenantId = :tenantId AND wt.bundleAlias = :bundleAlias")
    List<WidgetTypeEntity> findWidgetTypesByTenantIdAndBundleAlias(@Param("tenantId") UUID tenantId,
                                                                   @Param("bundleAlias") String bundleAlias);

    @Query("SELECT new org.thingsboard.server.dao.model.sql.WidgetTypeInfoEntity(wtd) FROM WidgetTypeDetailsEntity wtd " +
            "WHERE wtd.tenantId = :tenantId AND wtd.bundleAlias = :bundleAlias")
    List<WidgetTypeInfoEntity> findWidgetTypesInfosByTenantIdAndBundleAlias(@Param("tenantId") UUID tenantId,
                                                                            @Param("bundleAlias") String bundleAlias);

    List<WidgetTypeDetailsEntity> findByTenantIdAndBundleAlias(UUID tenantId, String bundleAlias);

    @Query("SELECT wt FROM WidgetTypeEntity wt " +
            "WHERE wt.tenantId = :tenantId AND wt.bundleAlias = :bundleAlias AND wt.alias = :alias")
    WidgetTypeEntity findWidgetTypeByTenantIdAndBundleAliasAndAlias(@Param("tenantId") UUID tenantId,
                                                          @Param("bundleAlias") String bundleAlias,
                                                          @Param("alias") String alias);
}
