/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.rule.engine.cache;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNodeCacheService;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.queue.PartitionChangeMsg;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

@Slf4j
public abstract class TbAbstractCacheBasedRuleNode<C, K> implements TbNode {

    protected static final int DEFAULT_PARTITION = -999999;

    protected C config;

    protected final Map<Integer, Set<EntityId>> partitionsEntityIdsMap;
    protected final Map<EntityId, K> entityIdValuesMap;

    protected TbAbstractCacheBasedRuleNode() {
        partitionsEntityIdsMap = new HashMap<>();
        entityIdValuesMap = new HashMap<>();
    }

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = loadRuleNodeConfiguration(ctx, configuration);
        getValuesFromCacheAndSchedule(ctx);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        if (msg.isTypeOf(getTickMsgType())) {
            processOnTickMsg(ctx, msg);
        } else {
            processOnRegularMsg(ctx, msg);
        }
    }

    @Override
    public void destroy(TbContext ctx, ComponentLifecycleEvent reason) {
        if (ComponentLifecycleEvent.DELETED.equals(reason)) {
            if (!partitionsEntityIdsMap.isEmpty()) {
                partitionsEntityIdsMap.forEach((partition, entityIds) ->
                        entityIds.forEach(id ->
                                getCacheIfPresentAndExecute(ctx, cache -> cache.evictTbMsgs(id, partition))));
            }
            getCacheIfPresentAndExecute(ctx, cache -> cache.evict(getEntityIdsCacheKey()));
        }
        partitionsEntityIdsMap.clear();
        entityIdValuesMap.clear();
    }

    @Override
    public void onPartitionChangeMsg(TbContext ctx, PartitionChangeMsg msg) {
        Set<Map.Entry<Integer, Set<EntityId>>> currentPartitions = partitionsEntityIdsMap.entrySet();
        RuleNodeId ruleNodeId = ctx.getSelfId();
        log.trace("[{}] On partition change msg: {}, current partitions: {}", ruleNodeId, msg, currentPartitions);
        Set<Integer> newPartitions = msg.getPartitions();
        currentPartitions.removeIf(entry -> {
            Integer partition = entry.getKey();
            boolean remove = !newPartitions.contains(partition);
            if (remove) {
                log.trace("[{}] Removed old partition: [{}] from the partitions map!", ruleNodeId, partition);
                Set<EntityId> entityIds = entry.getValue();
                entityIdValuesMap.keySet().removeAll(entityIds);
                if (log.isTraceEnabled()) {
                    entityIds.forEach(entityId -> log.trace("[{}] Removed non-local entity: [{}] from the entityId values map!", ruleNodeId, entityId));
                }
            }
            return remove;
        });
        boolean checkCache = newPartitions.stream().anyMatch(newPartition -> !partitionsEntityIdsMap.containsKey(newPartition));
        if (checkCache) {
            getValuesFromCacheAndSchedule(ctx);
        }
    }

    protected void getCacheIfPresentAndExecute(TbContext ctx, Consumer<RuleNodeCacheService> cacheOperation) {
        ctx.getRuleNodeCacheService().ifPresent(cacheOperation);
    }

    protected void getValuesFromCacheAndSchedule(TbContext ctx) {
        log.trace("[{}] Going to fetch values from cache ...", ctx.getSelfId());
        getCacheIfPresentAndExecute(ctx, cache -> {
            Set<EntityId> entityIds = cache.getEntityIds(getEntityIdsCacheKey());
            if (entityIds.isEmpty()) {
                return;
            }
            entityIds.forEach(id -> {
                TopicPartitionInfo tpi = ctx.getTopicPartitionInfo(id);
                if (!tpi.isMyPartition()) {
                    log.trace("[{}][{}][{}] Ignore entity that doesn't belong to my partition!", ctx.getSelfId(), tpi.getFullTopicName(), id);
                } else {
                    Integer partition = tpi.getPartition().orElse(DEFAULT_PARTITION);
                    Set<EntityId> partitionEntityIds = partitionsEntityIdsMap.computeIfAbsent(partition, k -> new HashSet<>());
                    boolean added = partitionEntityIds.add(id);
                    if (added) {
                        getValuesFromCacheAndSchedule(ctx, cache, partition, id);
                    }
                }
            });
        });
    }

    protected abstract C loadRuleNodeConfiguration(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException;

    protected abstract void getValuesFromCacheAndSchedule(TbContext ctx, RuleNodeCacheService cache, Integer partition, EntityId id);

    protected abstract void processOnRegularMsg(TbContext ctx, TbMsg msg);

    protected abstract void processOnTickMsg(TbContext ctx, TbMsg msg);

    protected abstract TbMsgType getTickMsgType();

    protected abstract String getEntityIdsCacheKey();

}
