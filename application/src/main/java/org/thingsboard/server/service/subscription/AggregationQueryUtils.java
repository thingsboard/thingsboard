/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.service.subscription;

import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQueryResult;
import org.thingsboard.server.common.data.query.ComparisonTsValue;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.TsValue;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.AggKey;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared aggregated-history logic used by both the WebSocket {@code AggHistoryCmd} handler
 * ({@link DefaultTbEntityDataSubscriptionService}) and the REST {@code /find/aggHistory} endpoint.
 */
public class AggregationQueryUtils {

    /**
     * Builds one current (and, when requested, one previous) timeseries query per {@link AggKey}, keyed by query id.
     * A key with {@code previousValueOnly} skips the current window; a previous window is added only when both bounds
     * are present and {@code previousEndTs >= previousStartTs}.
     */
    public static Map<Integer, ReadTsKvQueryInfo> buildAggHistoryQueries(List<AggKey> keys, long startTs, long endTs) {
        Map<Integer, ReadTsKvQueryInfo> queries = new HashMap<>();
        for (AggKey key : keys) {
            if (key.getPreviousValueOnly() == null || !key.getPreviousValueOnly()) {
                var query = singleBucketQuery(key.getKey(), startTs, endTs, key.getAgg());
                queries.put(query.getId(), new ReadTsKvQueryInfo(key, query, false));
            }
            if (key.getPreviousStartTs() != null && key.getPreviousEndTs() != null && key.getPreviousEndTs() >= key.getPreviousStartTs()) {
                var query = singleBucketQuery(key.getKey(), key.getPreviousStartTs(), key.getPreviousEndTs(), key.getAgg());
                queries.put(query.getId(), new ReadTsKvQueryInfo(key, query, true));
            }
        }
        return queries;
    }

    /**
     * A single bucket spanning the whole {@code [startTs, endTs]} window (interval = the full window, limit = 1),
     * yielding exactly one aggregated point.
     */
    private static BaseReadTsKvQuery singleBucketQuery(String key, long startTs, long endTs, Aggregation agg) {
        return new BaseReadTsKvQuery(key, startTs, endTs, endTs - startTs, 1, agg);
    }

    /**
     * Maps the per-entity query results into {@code entityData.aggLatest} (current/previous per key) and fills any key
     * without data with {@link TsValue#EMPTY}. When {@code lastTsCollector} is non-null, the last entry ts of each
     * current value is recorded into it (used by the WS path to set up follow-up subscriptions).
     */
    public static void populateAggLatest(EntityData entityData, List<ReadTsKvQueryResult> queryResults,
                                         Map<Integer, ReadTsKvQueryInfo> queries, List<AggKey> keys,
                                         Map<String, Long> lastTsCollector) {
        if (queryResults != null) {
            for (ReadTsKvQueryResult queryResult : queryResults) {
                ReadTsKvQueryInfo queryInfo = queries.get(queryResult.getQueryId());
                ComparisonTsValue comparisonTsValue = entityData.getAggLatest()
                        .computeIfAbsent(queryInfo.getKey().getId(), agg -> new ComparisonTsValue());
                if (queryInfo.isPrevious()) {
                    comparisonTsValue.setPrevious(queryResult.toTsValue(queryInfo.getQuery()));
                } else {
                    comparisonTsValue.setCurrent(queryResult.toTsValue(queryInfo.getQuery()));
                    if (lastTsCollector != null) {
                        lastTsCollector.put(queryInfo.getQuery().getKey(), queryResult.getLastEntryTs());
                    }
                }
            }
        }
        keys.forEach(key -> entityData.getAggLatest().putIfAbsent(key.getId(), new ComparisonTsValue(TsValue.EMPTY, TsValue.EMPTY)));
    }

}
