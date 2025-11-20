/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.service.cf.ctx.state;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.Setter;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.actors.TbActorRef;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.service.cf.ctx.CalculatedFieldEntityCtxId;
import org.thingsboard.server.service.cf.ctx.state.aggregation.RelatedEntitiesArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.aggregation.single.EntityAggregationArgumentEntry;
import org.thingsboard.server.utils.CalculatedFieldUtils;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public abstract class BaseCalculatedFieldState implements CalculatedFieldState, Closeable {

    protected final EntityId entityId;
    protected CalculatedFieldCtx ctx;
    protected TbActorRef actorCtx;
    protected List<String> requiredArguments;

    protected Map<String, ArgumentEntry> arguments = new HashMap<>();
    protected boolean sizeExceedsLimit;
    protected long latestTimestamp = -1;
    protected ReadinessStatus readinessStatus;

    @Setter
    private TopicPartitionInfo partition;

    public BaseCalculatedFieldState(EntityId entityId) {
        this.entityId = entityId;
    }

    @Override
    public void setCtx(CalculatedFieldCtx ctx, TbActorRef actorCtx) {
        this.ctx = ctx;
        this.actorCtx = actorCtx;
        this.requiredArguments = ctx.getArgNames();
        this.readinessStatus = checkReadiness(requiredArguments, arguments);
    }

    @Override
    public void init(boolean restored) {
    }

    @Override
    public Map<String, ArgumentEntry> update(Map<String, ArgumentEntry> argumentValues, CalculatedFieldCtx ctx) {
        Map<String, ArgumentEntry> updatedArguments = null;

        for (Map.Entry<String, ArgumentEntry> entry : argumentValues.entrySet()) {
            String key = entry.getKey();
            ArgumentEntry newEntry = entry.getValue();

            checkArgumentSize(key, newEntry, ctx);

            ArgumentEntry existingEntry = arguments.get(key);
            boolean entryUpdated;

            if (existingEntry == null || newEntry.isForceResetPrevious()) {
                validateNewEntry(key, newEntry);
                if (existingEntry instanceof RelatedEntitiesArgumentEntry relatedEntitiesArgumentEntry) {
                    relatedEntitiesArgumentEntry.updateEntry(newEntry);
                } else if (existingEntry instanceof EntityAggregationArgumentEntry entityAggArgumentEntry) {
                    entityAggArgumentEntry.updateEntry(newEntry);
                } else {
                    arguments.put(key, newEntry);
                }
                entryUpdated = true;
            } else {
                entryUpdated = existingEntry.updateEntry(newEntry);
            }

            if (entryUpdated) {
                if (updatedArguments == null) {
                    updatedArguments = new HashMap<>(argumentValues.size());
                }
                updatedArguments.put(key, newEntry);
                updateLastUpdateTimestamp(newEntry);
            }

        }

        if (updatedArguments == null) {
            return Collections.emptyMap();
        }
        readinessStatus = checkReadiness(requiredArguments, arguments);
        return updatedArguments;
    }

    @Override
    public void reset() { // must reset everything dependent on arguments
        requiredArguments = null;
        arguments.clear();
        sizeExceedsLimit = false;
        latestTimestamp = -1;
    }

    @Override
    public boolean isReady() {
        return readinessStatus.ready();
    }

    @Override
    public void checkStateSize(CalculatedFieldEntityCtxId ctxId, long maxStateSize) {
        if (!sizeExceedsLimit && maxStateSize > 0 && CalculatedFieldUtils.toProto(ctxId, this).getSerializedSize() > maxStateSize) {
            arguments.clear();
            sizeExceedsLimit = true;
        }
    }

    @Override
    public void close() {
    }

    protected void validateNewEntry(String key, ArgumentEntry newEntry) {
    }

    protected ObjectNode toSimpleResult(boolean useLatestTs, ObjectNode valuesNode) {
        if (!useLatestTs) {
            return valuesNode;
        }
        long latestTs = getLatestTimestamp();
        if (latestTs == -1) {
            return valuesNode;
        }
        ObjectNode resultNode = JacksonUtil.newObjectNode();
        resultNode.put("ts", latestTs);
        resultNode.set("values", valuesNode);
        return resultNode;
    }

    private void updateLastUpdateTimestamp(ArgumentEntry entry) {
        long newTs = this.latestTimestamp;
        if (entry instanceof SingleValueArgumentEntry singleValueArgumentEntry) {
            newTs = singleValueArgumentEntry.getTs();
        } else if (entry instanceof TsRollingArgumentEntry tsRollingArgumentEntry) {
            Map.Entry<Long, Double> lastEntry = tsRollingArgumentEntry.getTsRecords().lastEntry();
            newTs = (lastEntry != null) ? lastEntry.getKey() : System.currentTimeMillis();
        } else if (entry instanceof RelatedEntitiesArgumentEntry relatedEntitiesArgumentEntry) {
            newTs = relatedEntitiesArgumentEntry.getEntityInputs().values().stream()
                    .mapToLong(e -> (e instanceof SingleValueArgumentEntry s) ? s.getTs() : 0L)
                    .max()
                    .orElse(0L);
        }
        this.latestTimestamp = Math.max(this.latestTimestamp, newTs);
    }

    protected ReadinessStatus checkReadiness(List<String> requiredArguments, Map<String, ArgumentEntry> currentArguments) {
        if (currentArguments == null) {
            return ReadinessStatus.from(requiredArguments);
        }
        List<String> emptyArguments = null;
        for (String requiredArgumentKey : requiredArguments) {
            ArgumentEntry argumentEntry = currentArguments.get(requiredArgumentKey);
            if (argumentEntry == null || argumentEntry.isEmpty()) {
                if (emptyArguments == null) {
                    emptyArguments = new ArrayList<>();
                }
                emptyArguments.add(requiredArgumentKey);
            }
        }
        return ReadinessStatus.from(emptyArguments);
    }

}
