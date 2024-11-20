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
package org.thingsboard.server.common.data.cf.configuration;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;

import java.util.UUID;

@Data
public class ScriptCalculatedFieldConfiguration extends BaseCalculatedFieldConfiguration implements CalculatedFieldConfiguration {

    public ScriptCalculatedFieldConfiguration() {
        super();
    }

    public ScriptCalculatedFieldConfiguration(JsonNode config, EntityType entityType, UUID entityId) {
        super(config, entityType, entityId);
    }

    @Override
    public CalculatedFieldType getType() {
        return CalculatedFieldType.SCRIPT;
    }
}
