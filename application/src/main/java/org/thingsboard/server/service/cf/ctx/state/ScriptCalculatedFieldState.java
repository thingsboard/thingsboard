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
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.CalculatedFieldConfiguration;
import org.thingsboard.server.service.cf.CalculatedFieldResult;

import java.util.HashMap;
import java.util.Map;

@Data
@Slf4j
public class ScriptCalculatedFieldState implements CalculatedFieldState {

    private Map<String, String> arguments = new HashMap<>();

    public ScriptCalculatedFieldState() {
    }

    @Override
    public CalculatedFieldType getType() {
        return CalculatedFieldType.SCRIPT;
    }

    @Override
    public void initState(Map<String, String> argumentValues) {
        if (arguments == null) {
            this.arguments = new HashMap<>();
        }
        this.arguments.putAll(argumentValues);
    }

    @Override
    public CalculatedFieldResult performCalculation(CalculatedFieldConfiguration calculatedFieldConfiguration) {
        // TODO: implement
        return null;
    }

}
