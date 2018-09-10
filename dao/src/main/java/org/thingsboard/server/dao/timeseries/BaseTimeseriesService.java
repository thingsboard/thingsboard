/**
 * Copyright © 2016-2018 The Thingsboard Authors
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

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.kv.BaseTsKvQuery;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.kv.TsKvQuery;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.service.Validator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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

    @Autowired
    private EntityViewService entityViewService;

    @Override
    public ListenableFuture<List<TsKvEntry>> findAll(EntityId entityId, List<TsKvQuery> queries) {
        validate(entityId);
        queries.forEach(query -> validate(query));
        if (entityId.getEntityType().equals(EntityType.ENTITY_VIEW)) {
            EntityView entityView = entityViewService.findEntityViewById((EntityViewId) entityId);
            return timeseriesDao.findAllAsync(entityView.getEntityId(), updateQueriesForEntityView(entityView, queries));
        }
        return timeseriesDao.findAllAsync(entityId, queries);
    }

    @Override
    public ListenableFuture<List<TsKvEntry>> findLatest(EntityId entityId, Collection<String> keys) {
        validate(entityId);
        List<ListenableFuture<TsKvEntry>> futures = Lists.newArrayListWithExpectedSize(keys.size());
        keys.forEach(key -> Validator.validateString(key, "Incorrect key " + key));
        if (false/*entityId.getEntityType().equals(EntityType.ENTITY_VIEW)*/) {
            EntityView entityView = entityViewService.findEntityViewById((EntityViewId) entityId);
            Collection<String> newKeys = chooseKeysForEntityView(entityView, keys);
            newKeys.forEach(newKey -> futures.add(timeseriesDao.findLatest(entityView.getEntityId(), newKey)));
        } else {
            keys.forEach(key -> futures.add(timeseriesDao.findLatest(entityId, key)));
        }
        return Futures.allAsList(futures);
    }

    @Override
    public ListenableFuture<List<TsKvEntry>> findAllLatest(EntityId entityId) {
        validate(entityId);
        return timeseriesDao.findAllLatest(entityId);
    }

    @Override
    public ListenableFuture<List<Void>> save(EntityId entityId, TsKvEntry tsKvEntry) {
        validate(entityId);
        try {
            checkForNonEntityView(entityId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (tsKvEntry == null) {
            throw new IncorrectParameterException("Key value entry can't be null");
        }
        List<ListenableFuture<Void>> futures = Lists.newArrayListWithExpectedSize(INSERTS_PER_ENTRY);
        saveAndRegisterFutures(futures, entityId, tsKvEntry, 0L);
        return Futures.allAsList(futures);
    }

    @Override
    public ListenableFuture<List<Void>> save(EntityId entityId, List<TsKvEntry> tsKvEntries, long ttl) {
        try {
            checkForNonEntityView(entityId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<ListenableFuture<Void>> futures = Lists.newArrayListWithExpectedSize(tsKvEntries.size() * INSERTS_PER_ENTRY);
        for (TsKvEntry tsKvEntry : tsKvEntries) {
            if (tsKvEntry == null) {
                throw new IncorrectParameterException("Key value entry can't be null");
            }
            saveAndRegisterFutures(futures, entityId, tsKvEntry, ttl);
        }
        return Futures.allAsList(futures);
    }

    private void saveAndRegisterFutures(List<ListenableFuture<Void>> futures, EntityId entityId, TsKvEntry tsKvEntry, long ttl) {
        try {
            checkForNonEntityView(entityId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        futures.add(timeseriesDao.savePartition(entityId, tsKvEntry.getTs(), tsKvEntry.getKey(), ttl));
        futures.add(timeseriesDao.saveLatest(entityId, tsKvEntry));
        futures.add(timeseriesDao.save(entityId, tsKvEntry, ttl));
    }

    private List<TsKvQuery> updateQueriesForEntityView(EntityView entityView, List<TsKvQuery> queries) {
        List<TsKvQuery> newQueries = new ArrayList<>();
        entityView.getKeys().getTimeseries()
                .forEach(viewKey -> queries
                        .forEach(query -> {
                            if (query.getKey().equals(viewKey)) {
                                if (entityView.getStartTs() == 0 && entityView.getEndTs() == 0) {
                                    newQueries.add(updateQuery(query.getStartTs(), query.getEndTs(), viewKey, query));
                                } else if (entityView.getStartTs() == 0 && entityView.getEndTs() != 0) {
                                    newQueries.add(updateQuery(query.getStartTs(), entityView.getEndTs(), viewKey, query));
                                } else if (entityView.getStartTs() != 0 && entityView.getEndTs() == 0) {
                                    newQueries.add(updateQuery(entityView.getStartTs(), query.getEndTs(), viewKey, query));
                                } else {
                                    newQueries.add(updateQuery(entityView.getStartTs(), entityView.getEndTs(), viewKey, query));
                                }
                            }}));
        return newQueries;
    }

    @Deprecated /*Will be a modified*/
    private Collection<String> chooseKeysForEntityView(EntityView entityView, Collection<String> keys) {
        Collection<String> newKeys = new ArrayList<>();
        entityView.getKeys().getTimeseries()
                .forEach(viewKey -> keys
                        .forEach(key -> {
                            if (key.equals(viewKey)) {
                                newKeys.add(key);
                            }}));
        return newKeys;
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

    private static TsKvQuery updateQuery(Long startTs, Long endTs, String viewKey, TsKvQuery query) {
        return startTs <= query.getStartTs() && endTs >= query.getEndTs() ? query :
                new BaseTsKvQuery(viewKey, startTs, endTs, query.getInterval(), query.getLimit(), query.getAggregation());
    }

    private static void checkForNonEntityView(EntityId entityId) throws Exception {
        if (entityId.getEntityType().equals(EntityType.ENTITY_VIEW)) {
            throw new Exception("Entity-views were read only");
        }
    }
}
