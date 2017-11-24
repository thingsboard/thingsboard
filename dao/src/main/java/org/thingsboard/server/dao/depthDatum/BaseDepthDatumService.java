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
package org.thingsboard.server.dao.depthDatum;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.DsKvEntry;
import org.thingsboard.server.common.data.kv.DsKvQuery;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.service.Validator;

import java.util.Collection;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * @author Andrew Shvayka
 */
@Service
@Slf4j
public class BaseDepthDatumService implements DepthDatumService {

    public static final int INSERTS_PER_ENTRY = 3;

    @Autowired
    private DepthDatumDao depthDatumDao;

    @Override
    public ListenableFuture<List<DsKvEntry>> findAll(EntityId entityId, List<DsKvQuery> queries) {
        validate(entityId);
        queries.forEach(query -> validate(query));
        return depthDatumDao.findAllAsync(entityId, queries);
    }

    @Override
    public ListenableFuture<List<DsKvEntry>> findLatest(EntityId entityId, Collection<String> keys) {
        validate(entityId);
        List<ListenableFuture<DsKvEntry>> futures = Lists.newArrayListWithExpectedSize(keys.size());
        keys.forEach(key -> Validator.validateString(key, "Incorrect key " + key));
        keys.forEach(key -> futures.add(depthDatumDao.findLatest(entityId, key)));
        return Futures.allAsList(futures);
    }

    @Override
    public ListenableFuture<List<DsKvEntry>> findAllLatest(EntityId entityId) {
        validate(entityId);
        return depthDatumDao.findAllLatest(entityId);
    }

    @Override
    public ListenableFuture<List<Void>> save(EntityId entityId, DsKvEntry dsKvEntry) {
        validate(entityId);
        if (dsKvEntry == null) {
            throw new IncorrectParameterException("Key value entry can't be null");
        }
        List<ListenableFuture<Void>> futures = Lists.newArrayListWithExpectedSize(INSERTS_PER_ENTRY);
        saveAndRegisterFutures(futures, entityId, dsKvEntry, 0L);
        return Futures.allAsList(futures);
    }

    @Override
    public ListenableFuture<List<Void>> save(EntityId entityId, List<DsKvEntry> dsKvEntries, long ttl) {
        List<ListenableFuture<Void>> futures = Lists.newArrayListWithExpectedSize(dsKvEntries.size() * INSERTS_PER_ENTRY);
        for (DsKvEntry dsKvEntry : dsKvEntries) {
            if (dsKvEntry == null) {
                throw new IncorrectParameterException("Key value entry can't be null");
            }
            saveAndRegisterFutures(futures, entityId, dsKvEntry, ttl);
        }
        return Futures.allAsList(futures);
    }

    private void saveAndRegisterFutures(List<ListenableFuture<Void>> futures, EntityId entityId, DsKvEntry dsKvEntry, long ttl) {
        futures.add(depthDatumDao.savePartition(entityId, dsKvEntry.getDs(), dsKvEntry.getKey(), ttl));
        futures.add(depthDatumDao.saveLatest(entityId, dsKvEntry));
        futures.add(depthDatumDao.save(entityId, dsKvEntry, ttl));
    }

    private static void validate(EntityId entityId) {
        Validator.validateEntityId(entityId, "Incorrect entityId " + entityId);
    }

    private static void validate(DsKvQuery query) {
        if (query == null) {
            throw new IncorrectParameterException("TsKvQuery can't be null");
        } else if (isBlank(query.getKey())) {
            throw new IncorrectParameterException("Incorrect TsKvQuery. Key can't be empty");
        } else if (query.getDepthAggregation() == null) {
            throw new IncorrectParameterException("Incorrect TsKvQuery. Aggregation can't be empty");
        }
    }
}
