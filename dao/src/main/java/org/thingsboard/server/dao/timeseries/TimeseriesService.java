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

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Row;
import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.kv.TsKvQuery;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author Andrew Shvayka
 */
public interface TimeseriesService {

    ListenableFuture<List<TsKvEntry>> findAll(EntityId entityId, List<TsKvQuery> queries);

    ListenableFuture<List<ResultSet>> findLatest(EntityId entityId, Collection<String> keys);

    ResultSetFuture findAllLatest(EntityId entityId);

    ListenableFuture<List<ResultSet>> save(EntityId entityId, TsKvEntry tsKvEntry);

    ListenableFuture<List<ResultSet>> save(EntityId entityId, List<TsKvEntry> tsKvEntry);

    ListenableFuture<List<ResultSet>> save(EntityId entityId, List<TsKvEntry> tsKvEntry, long ttl);

    TsKvEntry convertResultToTsKvEntry(Row row);

    List<TsKvEntry> convertResultSetToTsKvEntryList(ResultSet rs);

}
