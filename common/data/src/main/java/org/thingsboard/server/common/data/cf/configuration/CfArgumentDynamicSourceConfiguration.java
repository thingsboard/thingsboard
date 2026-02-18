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

@Schema(
        description = "Configuration for calculated fields",
        discriminatorProperty = "type",
        discriminatorMapping = {
                @DiscriminatorMapping(value = "RELATION_PATH_QUERY", schema = RelationPathQueryDynamicSourceConfiguration.class),
                @DiscriminatorMapping(value = "CURRENT_OWNER", schema = CurrentOwnerDynamicSourceConfiguration.class)
        }
)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = RelationPathQueryDynamicSourceConfiguration.class, name = "RELATION_PATH_QUERY"),
        @JsonSubTypes.Type(value = CurrentOwnerDynamicSourceConfiguration.class, name = "CURRENT_OWNER")
})
@JsonIgnoreProperties(ignoreUnknown = true)
public interface CfArgumentDynamicSourceConfiguration {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonIgnore
    CFArgumentDynamicSourceType getType();

    default void validate() {}

}
