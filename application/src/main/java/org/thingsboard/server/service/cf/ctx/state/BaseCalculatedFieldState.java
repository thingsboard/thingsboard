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

import lombok.AllArgsConstructor;
import lombok.Data;
import org.thingsboard.server.service.cf.ctx.CalculatedFieldEntityCtxId;
import org.thingsboard.server.utils.CalculatedFieldUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public abstract class BaseCalculatedFieldState implements CalculatedFieldState {

    protected List<String> requiredArguments;
    protected Map<String, ArgumentEntry> arguments;
    protected boolean sizeExceedsLimit;

    protected long latestTimestamp = -1;

    public BaseCalculatedFieldState(List<String> requiredArguments) {
        this.requiredArguments = requiredArguments;
        this.arguments = new HashMap<>();
    }

    public BaseCalculatedFieldState() {
        this(new ArrayList<>(), new HashMap<>(), false, -1);
    }

    @Override
    public boolean updateState(CalculatedFieldCtx ctx, Map<String, ArgumentEntry> argumentValues) {
        if (arguments == null) {
            arguments = new HashMap<>();
        }

        boolean stateUpdated = false;

        for (Map.Entry<String, ArgumentEntry> entry : argumentValues.entrySet()) {
            String key = entry.getKey();
            ArgumentEntry newEntry = entry.getValue();

            checkArgumentSize(key, newEntry, ctx);

            ArgumentEntry existingEntry = arguments.get(key);
            boolean entryUpdated;

            if (existingEntry == null || newEntry.isForceResetPrevious()) {
                validateNewEntry(key, newEntry);
                arguments.put(key, newEntry);
                entryUpdated = true;
            } else {
                entryUpdated = existingEntry.updateEntry(newEntry);
            }

            if (entryUpdated) {
                stateUpdated = true;
                updateLastUpdateTimestamp(newEntry);
            }

        }

        return stateUpdated;
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

    protected void validateNewEntry(String key, ArgumentEntry newEntry) {}

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

}
