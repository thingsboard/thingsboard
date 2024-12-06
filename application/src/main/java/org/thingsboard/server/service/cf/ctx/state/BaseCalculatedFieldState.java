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
import java.util.stream.Collectors;

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
        arguments.putAll(
                argumentValues.entrySet().stream()
                        .peek(entry -> {
                            if (entry.getValue() instanceof LastRecordsArgumentEntry) {
                                throw new IllegalArgumentException("Last records argument entry is not allowed for single calculated field state");
                            }
                        })
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
        );
    }

}
