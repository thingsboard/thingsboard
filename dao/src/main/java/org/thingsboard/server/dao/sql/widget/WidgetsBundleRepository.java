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
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.dao.model.ToData;
import org.thingsboard.server.dao.model.sql.WidgetsBundleEntity;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 4/23/2017.
 */
//public interface WidgetsBundleRepository extends CrudRepository<WidgetsBundleEntity, UUID> {
public interface WidgetsBundleRepository extends JpaRepository<WidgetsBundleEntity, UUID>, JpaSpecificationExecutor {

    WidgetsBundleEntity findWidgetsBundleByTenantIdAndAlias(UUID tenantId, String alias);

    @Query(nativeQuery = true, value = "SELECT * FROM WIDGETS_BUNDLE WHERE TENANT_ID IS NULL " +
            "AND LOWER(SEARCH_TEXT) LIKE LOWER(CONCAT(?2, '%')) " +
            "ORDER BY ID LIMIT ?1")
    List<WidgetsBundleEntity> findSystemWidgetsBundlesFirstPage(Integer limit, String searchText);

    @Query(nativeQuery = true, value = "SELECT * FROM WIDGETS_BUNDLE WHERE TENANT_ID IS NULL " +
            "AND LOWER(SEARCH_TEXT) LIKE LOWER(CONCAT(?2, '%')) " +
            "AND ID > ?3 ORDER BY ID LIMIT ?1")
    List<WidgetsBundleEntity> findSystemWidgetsBundlesNextPage(Integer limit, String searchText, UUID idOffset);

    @Query(nativeQuery = true, value = "SELECT * FROM WIDGETS_BUNDLE WHERE TENANT_ID = ?2 " +
            "AND LOWER(SEARCH_TEXT) LIKE LOWER(CONCAT(?3, '%')) " +
            "ORDER BY ID LIMIT ?1")
    List<WidgetsBundleEntity> findTenantWidgetsBundlesByTenantIdFirstPage(int limit, UUID tenantId, String textSearch);

    @Query(nativeQuery = true, value = "SELECT * FROM WIDGETS_BUNDLE WHERE TENANT_ID = ?2 " +
            "AND LOWER(SEARCH_TEXT) LIKE LOWER(CONCAT(?3, '%')) " +
            "AND ID > ?4 ORDER BY ID LIMIT ?1")
    List<WidgetsBundleEntity> findTenantWidgetsBundlesByTenantIdNextPage(int limit, UUID tenantId, String textSearch, UUID idOffset);

    @Query(nativeQuery = true, value = "SELECT * FROM WIDGETS_BUNDLE WHERE (TENANT_ID IS NULL OR TENANT_ID = ?2) " +
            "AND LOWER(SEARCH_TEXT) LIKE LOWER(CONCAT(?3, '%')) " +
            "ORDER BY ID LIMIT ?1")
    List<WidgetsBundleEntity> findAllTenantWidgetsBundlesByTenantIdFirstPage(int limit, UUID tenantId, String textSearch);

    @Query(nativeQuery = true, value = "SELECT * FROM WIDGETS_BUNDLE WHERE (TENANT_ID IS NULL OR TENANT_ID = ?2) " +
            "AND LOWER(SEARCH_TEXT) LIKE LOWER(CONCAT(?3, '%')) " +
            "AND ID > ?4 ORDER BY ID LIMIT ?1")
    List<WidgetsBundleEntity> findAllTenantWidgetsBundlesByTenantIdNextPage(int limit, UUID tenantId, String textSearch, UUID idOffset);
}
