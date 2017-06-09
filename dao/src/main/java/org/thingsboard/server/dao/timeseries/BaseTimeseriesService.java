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
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.kv.BaseTsKvQuery;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.kv.TsKvQuery;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.server.dao.service.Validator;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

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
    public ListenableFuture<List<TsKvEntry>> findAll(EntityId entityId, List<TsKvQuery> queries) {
        validate(entityId);
        queries.forEach(query -> validate(query));
        return timeseriesDao.findAllAsync(entityId, queries);
    }

    @Override
    public ListenableFuture<List<ResultSet>> findLatest(EntityId entityId, Collection<String> keys) {
        validate(entityId);
        List<ResultSetFuture> futures = Lists.newArrayListWithExpectedSize(keys.size());
        keys.forEach(key -> Validator.validateString(key, "Incorrect key " + key));
        keys.forEach(key -> futures.add(timeseriesDao.findLatest(entityId, key)));
        return Futures.allAsList(futures);
    }

    @Override
    public ResultSetFuture findAllLatest(EntityId entityId) {
        validate(entityId);
        return timeseriesDao.findAllLatest(entityId);
    }

    @Override
    public ListenableFuture<List<ResultSet>> save(EntityId entityId, TsKvEntry tsKvEntry) {
        validate(entityId);
        if (tsKvEntry == null) {
            throw new IncorrectParameterException("Key value entry can't be null");
        }
        long partitionTs = timeseriesDao.toPartitionTs(tsKvEntry.getTs());

        List<ResultSetFuture> futures = Lists.newArrayListWithExpectedSize(INSERTS_PER_ENTRY);
        saveAndRegisterFutures(futures, entityId, tsKvEntry, partitionTs, 0L);
        return Futures.allAsList(futures);
    }

    @Override
    public ListenableFuture<List<ResultSet>> save(EntityId entityId, List<TsKvEntry> tsKvEntries) {
        return save(entityId, tsKvEntries, 0L);
    }

    @Override
    public ListenableFuture<List<ResultSet>> save(EntityId entityId, List<TsKvEntry> tsKvEntries, long ttl) {
        validate(entityId);
        List<ResultSetFuture> futures = Lists.newArrayListWithExpectedSize(tsKvEntries.size() * INSERTS_PER_ENTRY);
        for (TsKvEntry tsKvEntry : tsKvEntries) {
            if (tsKvEntry == null) {
                throw new IncorrectParameterException("Key value entry can't be null");
            }
            long partitionTs = timeseriesDao.toPartitionTs(tsKvEntry.getTs());
            saveAndRegisterFutures(futures, entityId, tsKvEntry, partitionTs, ttl);
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

    private void saveAndRegisterFutures(List<ResultSetFuture> futures, EntityId entityId, TsKvEntry tsKvEntry, long partitionTs, long ttl) {
        futures.add(timeseriesDao.savePartition(entityId, partitionTs, tsKvEntry.getKey(), ttl));
        futures.add(timeseriesDao.saveLatest(entityId, tsKvEntry));
        futures.add(timeseriesDao.save(entityId, partitionTs, tsKvEntry, ttl));
    }

    private static void validate(EntityId entityId) {
        Validator.validateEntityId(entityId, "Incorrect entityId " + entityId);
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
