/**
 * Copyright © 2016-2017 The Thingsboard Authors
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

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.annotation.SqlDao;
import org.thingsboard.server.dao.model.sql.WidgetsBundleEntity;

import java.util.List;
import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 4/23/2017.
 */
@SqlDao
public interface WidgetsBundleRepository extends CrudRepository<WidgetsBundleEntity, UUID> {

    WidgetsBundleEntity findWidgetsBundleByTenantIdAndAlias(UUID tenantId, String alias);

    @Query("SELECT wb FROM WidgetsBundleEntity wb WHERE wb.tenantId = :systemTenantId " +
            "AND LOWER(wb.searchText) LIKE LOWER(CONCAT(:searchText, '%')) " +
            "AND wb.id > :idOffset ORDER BY wb.id")
    List<WidgetsBundleEntity> findSystemWidgetsBundles(@Param("systemTenantId") UUID systemTenantId,
                                                       @Param("searchText") String searchText,
                                                       @Param("idOffset") UUID idOffset,
                                                       Pageable pageable);

    @Query("SELECT wb FROM WidgetsBundleEntity wb WHERE wb.tenantId = :tenantId " +
            "AND LOWER(wb.searchText) LIKE LOWER(CONCAT(:textSearch, '%')) " +
            "AND wb.id > :idOffset ORDER BY wb.id")
    List<WidgetsBundleEntity> findTenantWidgetsBundlesByTenantId(@Param("tenantId") UUID tenantId,
                                                                 @Param("textSearch") String textSearch,
                                                                 @Param("idOffset") UUID idOffset,
                                                                 Pageable pageable);

    @Query("SELECT wb FROM WidgetsBundleEntity wb WHERE wb.tenantId IN (:tenantId, :nullTenantId) " +
            "AND LOWER(wb.searchText) LIKE LOWER(CONCAT(:textSearch, '%')) " +
            "AND wb.id > :idOffset ORDER BY wb.id")
    List<WidgetsBundleEntity> findAllTenantWidgetsBundlesByTenantId(@Param("tenantId") UUID tenantId,
                                                                    @Param("nullTenantId") UUID nullTenantId,
                                                                    @Param("textSearch") String textSearch,
                                                                    @Param("idOffset") UUID idOffset,
                                                                    Pageable pageable);
}
