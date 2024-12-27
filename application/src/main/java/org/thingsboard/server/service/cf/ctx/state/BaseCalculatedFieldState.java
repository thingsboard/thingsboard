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
package org.thingsboard.server.service.cf.ctx.state;

import java.util.HashMap;
import java.util.Map;

public abstract class BaseCalculatedFieldState implements CalculatedFieldState {

    protected Map<String, ArgumentEntry> arguments;

    public BaseCalculatedFieldState() {
        arguments = new HashMap<>();
    }

    @Override
    public Map<String, ArgumentEntry> getArguments() {
        return this.arguments;
    }

    @Override
    public boolean updateState(Map<String, ArgumentEntry> argumentValues) {
        if (arguments == null) {
            arguments = new HashMap<>();
        }

        boolean stateUpdated = false;

        for (Map.Entry<String, ArgumentEntry> entry : argumentValues.entrySet()) {
            String key = entry.getKey();
            ArgumentEntry newEntry = entry.getValue();
            ArgumentEntry existingEntry = arguments.get(key);

            if (existingEntry == null || existingEntry.hasUpdatedValue(newEntry)) {
                if (existingEntry instanceof TsRollingArgumentEntry existingTsRollingEntry && newEntry instanceof TsRollingArgumentEntry newTsRollingEntry) {
                    existingTsRollingEntry.addAllTsRecords(newTsRollingEntry.getTsRecords());
                } else if (existingEntry instanceof TsRollingArgumentEntry existingTsRollingEntry && newEntry instanceof SingleValueArgumentEntry singleValueEntry) {
                    existingTsRollingEntry.addTsRecord(singleValueEntry.getTs(), singleValueEntry.getValue());
                } else if (existingEntry instanceof SingleValueArgumentEntry existingSingleValueEntry && newEntry instanceof SingleValueArgumentEntry singleValueEntry) {
//                    Long existingVersion = existingSingleValueEntry.getVersion();
//                    Long newVersion = singleValueEntry.getVersion();
//                    if (newVersion != null && (existingVersion == null || newVersion > existingVersion)) {
//                        arguments.put(key, newEntry.copy());
//                    }
                    arguments.put(key, newEntry.copy());
                } else {
                    arguments.put(key, newEntry.copy());
                }
                stateUpdated = true;
            }
        }

        return stateUpdated;
    }

}
