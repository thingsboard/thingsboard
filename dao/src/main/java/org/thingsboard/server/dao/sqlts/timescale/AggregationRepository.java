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

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;
import org.thingsboard.server.dao.model.sqlts.timescale.ts.TimescaleTsKvEntity;
import org.thingsboard.server.dao.util.TimescaleDBTsOrTsLatestDao;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Repository
@TimescaleDBTsOrTsLatestDao
public class AggregationRepository {

    public static final String FIND_AVG = "findAvg";
    public static final String FIND_MAX = "findMax";
    public static final String FIND_MIN = "findMin";
    public static final String FIND_SUM = "findSum";
    public static final String FIND_COUNT = "findCount";

    public static final String FROM_WHERE_CLAUSE = "FROM ts_kv tskv WHERE tskv.entity_id = cast(:entityId AS uuid) AND tskv.key= cast(:entityKey AS int) AND tskv.ts > :startTs AND tskv.ts <= :endTs GROUP BY tskv.entity_id, tskv.key, tsBucket ORDER BY tskv.entity_id, tskv.key, tsBucket";

    public static final String FIND_AVG_QUERY = "SELECT time_bucket(:timeBucket, tskv.ts) AS tsBucket, :timeBucket AS interval, SUM(COALESCE(tskv.long_v, 0)) AS longValue, SUM(COALESCE(tskv.dbl_v, 0.0)) AS doubleValue, SUM(CASE WHEN tskv.long_v IS NULL THEN 0 ELSE 1 END) AS longCountValue, SUM(CASE WHEN tskv.dbl_v IS NULL THEN 0 ELSE 1 END) AS doubleCountValue, null AS strValue, 'AVG' AS aggType ";

    public static final String FIND_MAX_QUERY = "SELECT time_bucket(:timeBucket, tskv.ts) AS tsBucket, :timeBucket AS interval, MAX(COALESCE(tskv.long_v, -9223372036854775807)) AS longValue, MAX(COALESCE(tskv.dbl_v, -1.79769E+308)) as doubleValue, SUM(CASE WHEN tskv.long_v IS NULL THEN 0 ELSE 1 END) AS longCountValue, SUM(CASE WHEN tskv.dbl_v IS NULL THEN 0 ELSE 1 END) AS doubleCountValue, MAX(tskv.str_v) AS strValue, 'MAX' AS aggType ";

    public static final String FIND_MIN_QUERY = "SELECT time_bucket(:timeBucket, tskv.ts) AS tsBucket, :timeBucket AS interval, MIN(COALESCE(tskv.long_v, 9223372036854775807)) AS longValue, MIN(COALESCE(tskv.dbl_v, 1.79769E+308)) as doubleValue, SUM(CASE WHEN tskv.long_v IS NULL THEN 0 ELSE 1 END) AS longCountValue, SUM(CASE WHEN tskv.dbl_v IS NULL THEN 0 ELSE 1 END) AS doubleCountValue, MIN(tskv.str_v) AS strValue, 'MIN' AS aggType ";

    public static final String FIND_SUM_QUERY = "SELECT time_bucket(:timeBucket, tskv.ts) AS tsBucket, :timeBucket AS interval, SUM(COALESCE(tskv.long_v, 0)) AS longValue, SUM(COALESCE(tskv.dbl_v, 0.0)) AS doubleValue, SUM(CASE WHEN tskv.long_v IS NULL THEN 0 ELSE 1 END) AS longCountValue, SUM(CASE WHEN tskv.dbl_v IS NULL THEN 0 ELSE 1 END) AS doubleCountValue, null AS strValue, null AS jsonValue, 'SUM' AS aggType ";

    public static final String FIND_COUNT_QUERY = "SELECT time_bucket(:timeBucket, tskv.ts) AS tsBucket, :timeBucket AS interval, SUM(CASE WHEN tskv.bool_v IS NULL THEN 0 ELSE 1 END) AS booleanValueCount, SUM(CASE WHEN tskv.str_v IS NULL THEN 0 ELSE 1 END) AS strValueCount, SUM(CASE WHEN tskv.long_v IS NULL THEN 0 ELSE 1 END) AS longValueCount, SUM(CASE WHEN tskv.dbl_v IS NULL THEN 0 ELSE 1 END) AS doubleValueCount, SUM(CASE WHEN tskv.json_v IS NULL THEN 0 ELSE 1 END) AS jsonValueCount ";

    @PersistenceContext
    private EntityManager entityManager;

    @Async
    public CompletableFuture<List<TimescaleTsKvEntity>> findAvg(UUID entityId, int entityKey, long timeBucket, long startTs, long endTs) {
        @SuppressWarnings("unchecked")
        List<TimescaleTsKvEntity> resultList = getResultList(entityId, entityKey, timeBucket, startTs, endTs, FIND_AVG);
        return CompletableFuture.supplyAsync(() -> resultList);
    }

    @Async
    public CompletableFuture<List<TimescaleTsKvEntity>> findMax(UUID entityId, int entityKey, long timeBucket, long startTs, long endTs) {
        @SuppressWarnings("unchecked")
        List<TimescaleTsKvEntity> resultList = getResultList(entityId, entityKey, timeBucket, startTs, endTs, FIND_MAX);
        return CompletableFuture.supplyAsync(() -> resultList);
    }

    @Async
    public CompletableFuture<List<TimescaleTsKvEntity>> findMin(UUID entityId, int entityKey, long timeBucket, long startTs, long endTs) {
        @SuppressWarnings("unchecked")
        List<TimescaleTsKvEntity> resultList = getResultList(entityId, entityKey, timeBucket, startTs, endTs, FIND_MIN);
        return CompletableFuture.supplyAsync(() -> resultList);
    }

    @Async
    public CompletableFuture<List<TimescaleTsKvEntity>> findSum(UUID entityId, int entityKey, long timeBucket, long startTs, long endTs) {
        @SuppressWarnings("unchecked")
        List<TimescaleTsKvEntity> resultList = getResultList(entityId, entityKey, timeBucket, startTs, endTs, FIND_SUM);
        return CompletableFuture.supplyAsync(() -> resultList);
    }

    @Async
    public CompletableFuture<List<TimescaleTsKvEntity>> findCount(UUID entityId, int entityKey, long timeBucket, long startTs, long endTs) {
        @SuppressWarnings("unchecked")
        List<TimescaleTsKvEntity> resultList = getResultList(entityId, entityKey, timeBucket, startTs, endTs, FIND_COUNT);
        return CompletableFuture.supplyAsync(() -> resultList);
    }

    private List getResultList(UUID entityId, int entityKey, long timeBucket, long startTs, long endTs, String query) {
        return entityManager.createNamedQuery(query)
                .setParameter("entityId", entityId)
                .setParameter("entityKey", entityKey)
                .setParameter("timeBucket", timeBucket)
                .setParameter("startTs", startTs)
                .setParameter("endTs", endTs)
                .getResultList();
    }


}
