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
import org.thingsboard.script.api.tbel.TbUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.actors.TbActorRef;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.service.cf.ctx.CalculatedFieldEntityCtxId;
import org.thingsboard.server.service.cf.ctx.state.aggregation.AggSingleEntityArgumentEntry;
import org.thingsboard.server.utils.CalculatedFieldUtils;

import java.io.Closeable;
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
    }

    @Override
    public void init() {
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

            if (existingEntry == null || !(newEntry instanceof AggSingleEntityArgumentEntry) && newEntry.isForceResetPrevious()) {
                validateNewEntry(key, newEntry);
                arguments.put(key, newEntry);
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
            updatedArguments = Collections.emptyMap();
        }
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
        return arguments.keySet().containsAll(requiredArguments) &&
               arguments.values().stream().noneMatch(ArgumentEntry::isEmpty);
    }

    @Override
    public void checkStateSize(CalculatedFieldEntityCtxId ctxId, long maxStateSize) {
        if (!sizeExceedsLimit && maxStateSize > 0 && CalculatedFieldUtils.toProto(ctxId, this).getSerializedSize() > maxStateSize) {
            arguments.clear();
            sizeExceedsLimit = true;
        }
    }

    @Override
    public void close() {}

    protected void validateNewEntry(String key, ArgumentEntry newEntry) {}

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
        }
        this.latestTimestamp = Math.max(this.latestTimestamp, newTs);
    }

    protected Object formatResult(double result, Integer decimals) {
        if (decimals == null) {
            return result;
        }
        if (decimals.equals(0)) {
            return TbUtils.toInt(result);
        }
        return TbUtils.toFixed(result, decimals);
    }

}
