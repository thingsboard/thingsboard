/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
package org.thingsboard.server.dao.sql.entityview;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.dao.model.sql.EntityViewEntity;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;

/**
 * Created by Victor Basanets on 8/31/2017.
 */
@SqlDao
public interface EntityViewRepository extends CrudRepository<EntityViewEntity, String> {

    @Query("SELECT e FROM EntityViewEntity e WHERE e.tenantId = :tenantId " +
            "AND LOWER(e.searchText) LIKE LOWER(CONCAT(:textSearch, '%')) " +
            "AND e.id > :idOffset ORDER BY e.id")
    List<EntityViewEntity> findByTenantId(@Param("tenantId") String tenantId,
                                      @Param("textSearch") String textSearch,
                                      @Param("idOffset") String idOffset,
                                      Pageable pageable);

    @Query("SELECT e FROM EntityViewEntity e WHERE e.tenantId = :tenantId " +
            "AND e.entityId = :entityId " +
            "AND LOWER(e.searchText) LIKE LOWER(CONCAT(:textSearch, '%')) " +
            "AND e.id > :idOffset ORDER BY e.id")
    List<EntityViewEntity> findByTenantIdAndEntityId(@Param("tenantId") String tenantId,
                                             @Param("entityId") String entityId,
                                             @Param("textSearch") String textSearch,
                                             @Param("idOffset") String idOffset,
                                             Pageable pageable);

    @Query("SELECT e FROM EntityViewEntity e WHERE e.tenantId = :tenantId " +
            "AND e.customerId = :customerId " +
            "AND LOWER(e.searchText) LIKE LOWER(CONCAT(:searchText, '%')) " +
            "AND e.id > :idOffset ORDER BY e.id")
    List<EntityViewEntity> findByTenantIdAndCustomerId(@Param("tenantId") String tenantId,
                                                   @Param("customerId") String customerId,
                                                   @Param("searchText") String searchText,
                                                   @Param("idOffset") String idOffset,
                                                   Pageable pageable);

    @Query("SELECT e FROM EntityViewEntity e WHERE e.tenantId = :tenantId " +
            "AND e.customerId = :customerId " +
            "AND e.entityId = :entityId " +
            "AND LOWER(e.searchText) LIKE LOWER(CONCAT(:textSearch, '%')) " +
            "AND e.id > :idOffset ORDER BY e.id")
    List<EntityViewEntity> findByTenantIdAndCustomerIdAndEntityId(@Param("tenantId") String tenantId,
                                                          @Param("customerId") String customerId,
                                                          @Param("entityId") String entityId,
                                                          @Param("textSearch") String textSearch,
                                                          @Param("idOffset") String idOffset,
                                                          Pageable pageable);

    EntityViewEntity findByTenantIdAndName(String tenantId, String name);

    List<EntityViewEntity> findAllByTenantIdAndCustomerIdAndIdIn(String tenantId,
                                                                 String customerId,
                                                                 List<String> entityViewsIds);

    List<EntityViewEntity> findAllByTenantIdAndIdIn(String tenantId, List<String> entityViewsIds);
}
