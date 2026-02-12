/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import org.thingsboard.server.common.data.cf.CalculatedFieldLink;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.List;
import java.util.Map;

@Schema(
        description = "Configuration for calculated fields",
        discriminatorProperty = "type",
        discriminatorMapping = {
                @DiscriminatorMapping(value = "SIMPLE", schema = SimpleCalculatedFieldConfiguration.class),
                @DiscriminatorMapping(value = "SCRIPT", schema = ScriptCalculatedFieldConfiguration.class)
        }
)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = SimpleCalculatedFieldConfiguration.class, name = "SIMPLE"),
        @JsonSubTypes.Type(value = ScriptCalculatedFieldConfiguration.class, name = "SCRIPT")
})
@JsonIgnoreProperties(ignoreUnknown = true)
public interface CalculatedFieldConfiguration {

    @Schema(
            description = "Type of calculated field configuration",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @JsonIgnore
    CalculatedFieldType getType();

    @Schema(
            description = "Arguments/parameters for the calculated field",
            example = "{\"param1\": {\"type\": \"STRING\", \"value\": \"example\"}}"
    )
    Map<String, Argument> getArguments();

    @Schema(
            description = "Expression or formula for calculating the field value",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "value1 + value2"
    )
    String getExpression();

    void setExpression(String expression);

    @Schema(
            description = "Output configuration for the calculated field result"
    )
    Output getOutput();

    @JsonIgnore
    List<EntityId> getReferencedEntities();

    List<CalculatedFieldLink> buildCalculatedFieldLinks(TenantId tenantId, EntityId cfEntityId, CalculatedFieldId calculatedFieldId);

    CalculatedFieldLink buildCalculatedFieldLink(TenantId tenantId, EntityId referencedEntityId, CalculatedFieldId calculatedFieldId);

}
