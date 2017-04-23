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
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.kv.TsKvQuery;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.service.Validator;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * @author Andrew Shvayka
 */
@Service
@Slf4j
public class BaseTimeseriesService implements TimeseriesService {

    public static final int INSERTS_PER_ENTRY = 3;

    @Autowired
    private TimeseriesDao timeseriesDao;

    @Override
    public ListenableFuture<List<TsKvEntry>> findAll(String entityType, UUIDBased entityId, List<TsKvQuery> queries) {
        validate(entityType, entityId);
        queries.forEach(query -> validate(query));
        return timeseriesDao.findAllAsync(entityType, entityId.getId(), queries);
    }

    @Override
    public ListenableFuture<List<ResultSet>> findLatest(String entityType, UUIDBased entityId, Collection<String> keys) {
        validate(entityType, entityId);
        List<ResultSetFuture> futures = Lists.newArrayListWithExpectedSize(keys.size());
        keys.forEach(key -> Validator.validateString(key, "Incorrect key " + key));
        keys.forEach(key -> futures.add(timeseriesDao.findLatest(entityType, entityId.getId(), key)));
        return Futures.allAsList(futures);
    }

    @Override
    public ResultSetFuture findAllLatest(String entityType, UUIDBased entityId) {
        validate(entityType, entityId);
        return timeseriesDao.findAllLatest(entityType, entityId.getId());
    }

    @Override
    public ListenableFuture<List<ResultSet>> save(String entityType, UUIDBased entityId, TsKvEntry tsKvEntry) {
        validate(entityType, entityId);
        if (tsKvEntry == null) {
            throw new IncorrectParameterException("Key value entry can't be null");
        }
        UUID uid = entityId.getId();
        long partitionTs = timeseriesDao.toPartitionTs(tsKvEntry.getTs());

        List<ResultSetFuture> futures = Lists.newArrayListWithExpectedSize(INSERTS_PER_ENTRY);
        saveAndRegisterFutures(futures, entityType, tsKvEntry, uid, partitionTs);
        return Futures.allAsList(futures);
    }

    @Override
    public ListenableFuture<List<ResultSet>> save(String entityType, UUIDBased entityId, List<TsKvEntry> tsKvEntries) {
        validate(entityType, entityId);
        List<ResultSetFuture> futures = Lists.newArrayListWithExpectedSize(tsKvEntries.size() * INSERTS_PER_ENTRY);
        for (TsKvEntry tsKvEntry : tsKvEntries) {
            if (tsKvEntry == null) {
                throw new IncorrectParameterException("Key value entry can't be null");
            }
            UUID uid = entityId.getId();
            long partitionTs = timeseriesDao.toPartitionTs(tsKvEntry.getTs());
            saveAndRegisterFutures(futures, entityType, tsKvEntry, uid, partitionTs);
        }
        return Futures.allAsList(futures);
    }

    @Override
    public TsKvEntry convertResultToTsKvEntry(Row row) {
        return timeseriesDao.convertResultToTsKvEntry(row);
    }

    @Override
    public List<TsKvEntry> convertResultSetToTsKvEntryList(ResultSet rs) {
        return timeseriesDao.convertResultToTsKvEntryList(rs.all());
    }

    private void saveAndRegisterFutures(List<ResultSetFuture> futures, String entityType, TsKvEntry tsKvEntry, UUID uid, long partitionTs) {
        futures.add(timeseriesDao.savePartition(entityType, uid, partitionTs, tsKvEntry.getKey()));
        futures.add(timeseriesDao.saveLatest(entityType, uid, tsKvEntry));
        futures.add(timeseriesDao.save(entityType, uid, partitionTs, tsKvEntry));
    }

    private static void validate(String entityType, UUIDBased entityId) {
        Validator.validateString(entityType, "Incorrect entityType " + entityType);
        Validator.validateId(entityId, "Incorrect entityId " + entityId);
    }

    private static void validate(TsKvQuery query) {
        if (query == null) {
            throw new IncorrectParameterException("TsKvQuery can't be null");
        } else if (isBlank(query.getKey())) {
            throw new IncorrectParameterException("Incorrect TsKvQuery. Key can't be empty");
        } else if (query.getAggregation() == null) {
            throw new IncorrectParameterException("Incorrect TsKvQuery. Aggregation can't be empty");
        }
    }
}
