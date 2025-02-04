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

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public abstract class BaseCalculatedFieldState implements CalculatedFieldState {

    protected List<String> requiredArguments;
    protected Map<String, ArgumentEntry> arguments;

    public BaseCalculatedFieldState(List<String> requiredArguments) {
        this.requiredArguments = requiredArguments;
        this.arguments = new HashMap<>();
    }

    public BaseCalculatedFieldState() {
        this.requiredArguments = new ArrayList<>();
        this.arguments = new HashMap<>();
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

            if (existingEntry == null) {
                validateNewEntry(newEntry);
                arguments.put(key, newEntry);
                stateUpdated = true;
            } else {
                stateUpdated = existingEntry.updateEntry(newEntry);
            }
        }

        return stateUpdated;
    }

    @Override
    public boolean isReady() {
        return arguments.keySet().containsAll(requiredArguments) &&
                !arguments.containsValue(SingleValueArgumentEntry.EMPTY) &&
                !arguments.containsValue(TsRollingArgumentEntry.EMPTY);
    }

    protected abstract void validateNewEntry(ArgumentEntry newEntry);

}
