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
package org.thingsboard.server.dao.sql.plugin;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.PluginMetaDataEntity;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;

/**
 * Created by Valerii Sosliuk on 5/1/2017.
 */
@SqlDao
public interface PluginMetaDataRepository extends CrudRepository<PluginMetaDataEntity, String> {

    PluginMetaDataEntity findByApiToken(String apiToken);

    @Query("SELECT pmd FROM PluginMetaDataEntity pmd WHERE pmd.tenantId = :tenantId " +
            "AND LOWER(pmd.searchText) LIKE LOWER(CONCAT(:textSearch, '%')) " +
            "AND pmd.id > :idOffset ORDER BY pmd.id")
    List<PluginMetaDataEntity> findByTenantIdAndPageLink(@Param("tenantId") String tenantId,
                                                         @Param("textSearch") String textSearch,
                                                         @Param("idOffset") String idOffset,
                                                         Pageable pageable);

    @Query("SELECT pmd FROM PluginMetaDataEntity pmd WHERE pmd.tenantId IN (:tenantId, :nullTenantId) " +
            "AND LOWER(pmd.searchText) LIKE LOWER(CONCAT(:textSearch, '%')) " +
            "AND pmd.id > :idOffset ORDER BY pmd.id")
    List<PluginMetaDataEntity> findAllTenantPluginsByTenantId(@Param("tenantId") String tenantId,
                                                              @Param("nullTenantId") String nullTenantId,
                                                              @Param("textSearch") String textSearch,
                                                              @Param("idOffset") String idOffset,
                                                              Pageable pageable);
}
