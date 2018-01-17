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
package org.thingsboard.server.dao.sql.application;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.ApplicationEntity;
import org.thingsboard.server.dao.util.SqlDao;
import org.springframework.data.domain.Pageable;
import java.util.List;

@SqlDao
public interface ApplicationRepository extends CrudRepository<ApplicationEntity, String> {

    @Query("SELECT a FROM ApplicationEntity a WHERE a.tenantId = :tenantId " +
            "AND LOWER(a.searchText) LIKE LOWER(CONCAT(:textSearch, '%')) " +
            "AND a.id > :idOffset ORDER BY a.id")
    List<ApplicationEntity> findByTenantId(@Param("tenantId") String tenantId,
                                           @Param("textSearch") String textSearch,
                                           @Param("idOffset") String idOffset,
                                           Pageable pageable);

    @Query("SELECT a FROM ApplicationEntity a WHERE :deviceType member a.deviceTypes AND a.tenantId = :tenantId ORDER BY a.id")
    List<ApplicationEntity> findByDeviceType(@Param("tenantId") String tenantId, @Param("deviceType") String deviceType);


    @Query("SELECT a FROM ApplicationEntity a WHERE :ruleId member a.rules AND a.tenantId = :tenantId ORDER BY a.id")
    List<ApplicationEntity> findByRuleId(@Param("tenantId") String tenantId, @Param("ruleId") String ruleId);


    @Query("SELECT a FROM ApplicationEntity a WHERE a.tenantId = :tenantId AND (a.dashboardId = :dashboardId OR a.miniDashboardId = :dashboardId) ORDER BY a.id")
    List<ApplicationEntity> findByDashboardId(@Param("tenantId") String tenantId, @Param("dashboardId") String dashboardId);


    ApplicationEntity findByTenantIdAndName(String tenantId, String name);
}
