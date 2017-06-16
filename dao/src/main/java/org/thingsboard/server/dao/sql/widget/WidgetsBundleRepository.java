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

    @Query(nativeQuery = true, value = "SELECT * FROM WIDGETS_BUNDLE WHERE TENANT_ID = :systemTenantId " +
            "AND LOWER(SEARCH_TEXT) LIKE LOWER(CONCAT(:searchText, '%')) " +
            "AND ID > :idOffset ORDER BY ID LIMIT :limit")
    List<WidgetsBundleEntity> findSystemWidgetsBundles(@Param("limit") int limit,
                                                       @Param("systemTenantId") UUID systemTenantId,
                                                       @Param("searchText") String searchText,
                                                       @Param("idOffset") UUID idOffset);

    @Query(nativeQuery = true, value = "SELECT * FROM WIDGETS_BUNDLE WHERE TENANT_ID = :tenantId " +
            "AND LOWER(SEARCH_TEXT) LIKE LOWER(CONCAT(:textSearch, '%')) " +
            "AND ID > :idOffset ORDER BY ID LIMIT :limit")
    List<WidgetsBundleEntity> findTenantWidgetsBundlesByTenantId(@Param("limit") int limit,
                                                                 @Param("tenantId") UUID tenantId,
                                                                 @Param("textSearch") String textSearch,
                                                                 @Param("idOffset") UUID idOffset);

    @Query(nativeQuery = true, value = "SELECT * FROM WIDGETS_BUNDLE WHERE TENANT_ID IN (:tenantId, :nullTenantId) " +
            "AND LOWER(SEARCH_TEXT) LIKE LOWER(CONCAT(:textSearch, '%')) " +
            "AND ID > :idOffset ORDER BY ID LIMIT :limit")
    List<WidgetsBundleEntity> findAllTenantWidgetsBundlesByTenantId(@Param("limit") int limit,
                                                                    @Param("tenantId") UUID tenantId,
                                                                    @Param("nullTenantId") UUID nullTenantId,
                                                                    @Param("textSearch") String textSearch,
                                                                    @Param("idOffset") UUID idOffset);
}
