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
package org.thingsboard.server.dao.timeseries;

import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Row;
import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.kv.TsKvQuery;

import java.util.List;

/**
 * @author Andrew Shvayka
 */
public interface TimeseriesDao {

    long toPartitionTs(long ts);

    ListenableFuture<List<TsKvEntry>> findAllAsync(EntityId entityId, List<TsKvQuery> queries);

    ResultSetFuture findLatest(EntityId entityId, String key);

    ResultSetFuture findAllLatest(EntityId entityId);

    ResultSetFuture save(EntityId entityId, long partition, TsKvEntry tsKvEntry, long ttl);

    ResultSetFuture savePartition(EntityId entityId, long partition, String key, long ttl);

    ResultSetFuture saveLatest(EntityId entityId, TsKvEntry tsKvEntry);

    TsKvEntry convertResultToTsKvEntry(Row row);

    List<TsKvEntry> convertResultToTsKvEntryList(List<Row> rows);

}
