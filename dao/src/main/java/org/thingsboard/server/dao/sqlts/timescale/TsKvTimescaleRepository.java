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
package org.thingsboard.server.dao.sqlts.timescale;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.dao.model.sqlts.timescale.TimescaleTsKvCompositeKey;
import org.thingsboard.server.dao.model.sqlts.timescale.TimescaleTsKvEntity;
import org.thingsboard.server.dao.util.TimescaleDBTsDao;

import java.util.List;

@TimescaleDBTsDao
public interface TsKvTimescaleRepository extends CrudRepository<TimescaleTsKvEntity, TimescaleTsKvCompositeKey> {

    @Query("SELECT tskv FROM TimescaleTsKvEntity tskv WHERE tskv.tenantId = :tenantId " +
            "AND tskv.entityId = :entityId " +
            "AND tskv.key = :entityKey " +
            "AND tskv.ts > :startTs AND tskv.ts <= :endTs")
    List<TimescaleTsKvEntity> findAllWithLimit(
            @Param("tenantId") String tenantId,
            @Param("entityId") String entityId,
            @Param("entityKey") String key,
            @Param("startTs") long startTs,
            @Param("endTs") long endTs, Pageable pageable);

    @Query(value = "SELECT tskv.tenant_id as tenant_id, tskv.entity_id as entity_id, tskv.key as key, last(tskv.ts,tskv.ts) as ts," +
            " last(tskv.bool_v, tskv.ts) as bool_v, last(tskv.str_v, tskv.ts) as str_v," +
            " last(tskv.long_v, tskv.ts) as long_v, last(tskv.dbl_v, tskv.ts) as dbl_v" +
            " FROM tenant_ts_kv tskv WHERE tskv.tenant_id = cast(:tenantId AS varchar) " +
            "AND tskv.entity_id = cast(:entityId AS varchar) " +
            "GROUP BY tskv.tenant_id, tskv.entity_id, tskv.key", nativeQuery = true)
    List<TimescaleTsKvEntity> findAllLatestValues(
            @Param("tenantId") String tenantId,
            @Param("entityId") String entityId);

    @Transactional
    @Modifying
    @Query("DELETE FROM TimescaleTsKvEntity tskv WHERE tskv.tenantId = :tenantId " +
            "AND tskv.entityId = :entityId " +
            "AND tskv.key = :entityKey " +
            "AND tskv.ts > :startTs AND tskv.ts <= :endTs")
    void delete(@Param("tenantId") String tenantId,
                @Param("entityId") String entityId,
                @Param("entityKey") String key,
                @Param("startTs") long startTs,
                @Param("endTs") long endTs);

}