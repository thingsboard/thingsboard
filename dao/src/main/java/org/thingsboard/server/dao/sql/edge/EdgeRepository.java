/**
 * Copyright © 2016-2019 The Thingsboard Authors
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
package org.thingsboard.server.dao.sql.edge;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.EdgeEntity;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;

@SqlDao
public interface EdgeRepository extends CrudRepository<EdgeEntity, String> {

    @Query("SELECT d FROM EdgeEntity d WHERE d.tenantId = :tenantId " +
            "AND d.customerId = :customerId " +
            "AND LOWER(d.searchText) LIKE LOWER(CONCAT(:searchText, '%')) " +
            "AND d.id > :idOffset ORDER BY d.id")
    List<EdgeEntity> findByTenantIdAndCustomerId(@Param("tenantId") String tenantId,
                                                 @Param("customerId") String customerId,
                                                 @Param("searchText") String searchText,
                                                 @Param("idOffset") String idOffset,
                                                 Pageable pageable);

    @Query("SELECT d FROM EdgeEntity d WHERE d.tenantId = :tenantId " +
            "AND LOWER(d.searchText) LIKE LOWER(CONCAT(:textSearch, '%')) " +
            "AND d.id > :idOffset ORDER BY d.id")
    List<EdgeEntity> findByTenantId(@Param("tenantId") String tenantId,
                                    @Param("textSearch") String textSearch,
                                    @Param("idOffset") String idOffset,
                                    Pageable pageable);

    @Query("SELECT d FROM EdgeEntity d WHERE d.tenantId = :tenantId " +
            "AND d.type = :type " +
            "AND LOWER(d.searchText) LIKE LOWER(CONCAT(:textSearch, '%')) " +
            "AND d.id > :idOffset ORDER BY d.id")
    List<EdgeEntity> findByTenantIdAndType(@Param("tenantId") String tenantId,
                                           @Param("type") String type,
                                           @Param("textSearch") String textSearch,
                                           @Param("idOffset") String idOffset,
                                           Pageable pageable);

    @Query("SELECT d FROM EdgeEntity d WHERE d.tenantId = :tenantId " +
            "AND d.customerId = :customerId " +
            "AND d.type = :type " +
            "AND LOWER(d.searchText) LIKE LOWER(CONCAT(:textSearch, '%')) " +
            "AND d.id > :idOffset ORDER BY d.id")
    List<EdgeEntity> findByTenantIdAndCustomerIdAndType(@Param("tenantId") String tenantId,
                                                        @Param("customerId") String customerId,
                                                        @Param("type") String type,
                                                        @Param("textSearch") String textSearch,
                                                        @Param("idOffset") String idOffset,
                                                        Pageable pageable);

    @Query("SELECT DISTINCT d.type FROM EdgeEntity d WHERE d.tenantId = :tenantId")
    List<String> findTenantEdgeTypes(@Param("tenantId") String tenantId);

    EdgeEntity findByTenantIdAndName(String tenantId, String name);

    List<EdgeEntity> findEdgesByTenantIdAndCustomerIdAndIdIn(String tenantId, String customerId, List<String> edgeIds);

    List<EdgeEntity> findEdgesByTenantIdAndIdIn(String tenantId, List<String> edgeIds);

}
