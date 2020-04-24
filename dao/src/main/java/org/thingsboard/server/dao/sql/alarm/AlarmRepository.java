/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.dao.sql.alarm;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.dao.model.sql.AlarmEntity;
import org.thingsboard.server.dao.model.sql.AlarmInfoEntity;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;

/**
 * Created by Valerii Sosliuk on 5/21/2017.
 */
@SqlDao
public interface AlarmRepository extends CrudRepository<AlarmEntity, String> {

    @Query("SELECT a FROM AlarmEntity a WHERE a.originatorId = :originatorId AND a.type = :alarmType ORDER BY a.startTs DESC")
    List<AlarmEntity> findLatestByOriginatorAndType(@Param("originatorId") String originatorId,
                                                    @Param("alarmType") String alarmType,
                                                    Pageable pageable);

    @Query("SELECT new org.thingsboard.server.dao.model.sql.AlarmInfoEntity(a) FROM AlarmEntity a, " +
            "RelationEntity re " +
            "WHERE a.tenantId = :tenantId " +
            "AND a.id = re.toId AND re.toType = 'ALARM' " +
            "AND re.relationTypeGroup = 'ALARM' " +
            "AND re.relationType = :relationType " +
            "AND re.fromId = :affectedEntityId " +
            "AND re.fromType = :affectedEntityType " +
            "AND (:startId IS NULL OR a.id >= :startId) " +
            "AND (:endId IS NULL OR a.id <= :endId) " +
            "AND (:idOffset IS NULL OR a.id < :idOffset) " +
            "AND (LOWER(a.type) LIKE LOWER(CONCAT(:searchText, '%'))" +
            "OR LOWER(a.severity) LIKE LOWER(CONCAT(:searchText, '%'))" +
            "OR LOWER(a.status) LIKE LOWER(CONCAT(:searchText, '%')))")
    Page<AlarmInfoEntity> findAlarms(@Param("tenantId") String tenantId,
                                     @Param("affectedEntityId") String affectedEntityId,
                                     @Param("affectedEntityType") String affectedEntityType,
                                     @Param("relationType") String relationType,
                                     @Param("startId") String startId,
                                     @Param("endId") String endId,
                                     @Param("idOffset") String idOffset,
                                     @Param("searchText") String searchText,
                                     Pageable pageable);
}
