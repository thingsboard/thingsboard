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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonNode;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.cf.CalculatedFieldLinkConfiguration;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.id.EntityId;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = SimpleCalculatedFieldConfiguration.class, name = "SIMPLE"),
        @JsonSubTypes.Type(value = ScriptCalculatedFieldConfiguration.class, name = "SCRIPT"),
        @JsonSubTypes.Type(value = LastRecordsCalculatedFieldConfiguration.class, name = "LAST_RECORDS")
})
public interface CalculatedFieldConfiguration {

    @JsonIgnore
    CalculatedFieldType getType();

    Map<String, Argument> getArguments();

    String getExpression();

    Output getOutput();

    @JsonIgnore
    List<EntityId> getReferencedEntities();

    @JsonIgnore
    CalculatedFieldLinkConfiguration getReferencedEntityConfig(EntityId entityId);

    @JsonIgnore
    JsonNode calculatedFieldConfigToJson(EntityType entityType, UUID entityId);

}
