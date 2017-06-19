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
package org.thingsboard.server.dao.sql.timeseries;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.kv.TsKvQuery;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.annotation.SqlDao;
import org.thingsboard.server.dao.model.sql.TsKvEntity;
import org.thingsboard.server.dao.model.sql.TsKvLatestCompositeKey;
import org.thingsboard.server.dao.model.sql.TsKvLatestEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDaoListeningExecutorService;
import org.thingsboard.server.dao.timeseries.TimeseriesDao;

import java.util.List;

@Component
@Slf4j
@SqlDao
public class JpaTimeseriesDao extends JpaAbstractDaoListeningExecutorService implements TimeseriesDao {

    @Autowired
    private TsKvRepository tsKvRepository;

    @Autowired
    private TsKvLatestRepository tsKvLatestRepository;

    @Override
    public ListenableFuture<List<TsKvEntry>> findAllAsync(EntityId entityId, List<TsKvQuery> queries) {
        // TODO - Add implementation
        return service.submit(() -> null);
    }

    private ListenableFuture<List<TsKvEntry>> findAllAsync(EntityId entityId, TsKvQuery query) {
        return null;
    }

    @Override
    public ListenableFuture<TsKvEntry> findLatest(EntityId entityId, String key) {
        TsKvLatestCompositeKey compositeKey =
                new TsKvLatestCompositeKey(
                        entityId.getEntityType().name(),
                        entityId.getId(),
                        key);
        return service.submit(() ->
                DaoUtil.getData(tsKvLatestRepository.findOne(compositeKey)));
    }

    @Override
    public ListenableFuture<List<TsKvEntry>> findAllLatest(EntityId entityId) {
        return service.submit(() ->
                DaoUtil.convertDataList(Lists.newArrayList(
                        tsKvLatestRepository.findAllByEntityTypeAndEntityId(
                                entityId.getEntityType().name(),
                                entityId.getId()))));
    }

    @Override
    public ListenableFuture<Void> save(EntityId entityId, TsKvEntry tsKvEntry, long ttl) {
        TsKvEntity entity = new TsKvEntity();
        entity.setEntityType(entityId.getEntityType().name());
        entity.setEntityId(entityId.getId());
        entity.setTs(tsKvEntry.getTs());
        entity.setKey(tsKvEntry.getKey());
        entity.setStrValue(tsKvEntry.getStrValue().orElse(null));
        entity.setDoubleValue(tsKvEntry.getDoubleValue().orElse(null));
        entity.setLongValue(tsKvEntry.getLongValue().orElse(null));
        entity.setBooleanValue(tsKvEntry.getBooleanValue().orElse(null));
        return service.submit(() -> {
            tsKvRepository.save(entity);
            return null;
        });
    }

    @Override
    public ListenableFuture<Void> savePartition(EntityId entityId, long tsKvEntryTs, String key, long ttl) {
        return service.submit(() -> null);
    }

    @Override
    public ListenableFuture<Void> saveLatest(EntityId entityId, TsKvEntry tsKvEntry) {
        TsKvLatestEntity latestEntity = new TsKvLatestEntity();
        latestEntity.setEntityType(entityId.getEntityType().name());
        latestEntity.setEntityId(entityId.getId());
        latestEntity.setTs(tsKvEntry.getTs());
        latestEntity.setKey(tsKvEntry.getKey());
        latestEntity.setStrValue(tsKvEntry.getStrValue().orElse(null));
        latestEntity.setDoubleValue(tsKvEntry.getDoubleValue().orElse(null));
        latestEntity.setLongValue(tsKvEntry.getLongValue().orElse(null));
        latestEntity.setBooleanValue(tsKvEntry.getBooleanValue().orElse(null));
        return service.submit(() -> {
            tsKvLatestRepository.save(latestEntity);
            return null;
        });
    }

}
