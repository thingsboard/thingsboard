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
package org.thingsboard.server.dao.sqlts.timescale;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.dao.model.sqlts.timescale.ts.TimescaleTsKvCompositeKey;
import org.thingsboard.server.dao.model.sqlts.timescale.ts.TimescaleTsKvEntity;
import org.thingsboard.server.dao.util.TimescaleDBTsOrTsLatestDao;

import java.util.List;
import java.util.UUID;

@TimescaleDBTsOrTsLatestDao
public interface TsKvTimescaleRepository extends CrudRepository<TimescaleTsKvEntity, TimescaleTsKvCompositeKey> {

    @Query("SELECT tskv FROM TimescaleTsKvEntity tskv WHERE tskv.entityId = :entityId " +
            "AND tskv.key = :entityKey " +
            "AND tskv.ts > :startTs AND tskv.ts <= :endTs")
    List<TimescaleTsKvEntity> findAllWithLimit(
            @Param("entityId") UUID entityId,
            @Param("entityKey") int key,
            @Param("startTs") long startTs,
            @Param("endTs") long endTs, Pageable pageable);

    @Transactional
    @Modifying
    @Query("DELETE FROM TimescaleTsKvEntity tskv WHERE tskv.entityId = :entityId " +
            "AND tskv.key = :entityKey " +
            "AND tskv.ts > :startTs AND tskv.ts <= :endTs")
    void delete(@Param("entityId") UUID entityId,
                @Param("entityKey") int key,
                @Param("startTs") long startTs,
                @Param("endTs") long endTs);

}
