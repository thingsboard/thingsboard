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
package org.thingsboard.server.common.data.calculated_field;

import lombok.Data;
import org.thingsboard.server.common.data.id.EntityId;

import java.util.Map;

@Data
public class CalculatedFieldConfig {

    private Map<String, Argument> arguments;
    private Output output;

    @Data
    public static class Argument {
        private EntityId entityId;
        private String key;
        private String type;
        private int defaultValue;
    }

    @Data
    public static class Output {
        private String type;
        private String expression;
    }

}
