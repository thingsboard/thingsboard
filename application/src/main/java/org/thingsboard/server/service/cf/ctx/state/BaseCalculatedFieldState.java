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
    }

    @Override
    public Map<String, ArgumentEntry> getArguments() {
        return this.arguments;
    }

    @Override
    public void initState(Map<String, ArgumentEntry> argumentValues) {
        if (arguments == null) {
            arguments = new HashMap<>();
        }
        argumentValues.forEach((key, argumentEntry) -> {
            ArgumentEntry existingArgumentEntry = arguments.get(key);
            if (existingArgumentEntry != null) {
                if (existingArgumentEntry instanceof SingleValueArgumentEntry) {
                    arguments.put(key, argumentEntry);
                } else if (existingArgumentEntry instanceof TsRollingArgumentEntry existingTsRollingArgumentEntry) {
                    if (argumentEntry instanceof TsRollingArgumentEntry tsRollingArgumentEntry) {
                        existingTsRollingArgumentEntry.getTsRecords().putAll(tsRollingArgumentEntry.getTsRecords());
                    } else if (argumentEntry instanceof SingleValueArgumentEntry singleValueArgumentEntry) {
                        existingTsRollingArgumentEntry.getTsRecords().put(singleValueArgumentEntry.getTs(), singleValueArgumentEntry.getValue());
                    }
                }
            } else {
                arguments.put(key, argumentEntry);
            }
        });
    }

}
