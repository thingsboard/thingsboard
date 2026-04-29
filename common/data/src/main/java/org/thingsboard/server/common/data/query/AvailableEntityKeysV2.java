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
package org.thingsboard.server.common.data.query;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.EntityType;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Schema(
        description = """
                Contains unique time series and attribute key names discovered from entities matching a query,
                optionally including a sample value for each key."""
)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AvailableEntityKeysV2(
        @Schema(
                description = "Set of entity types found among the matched entities.",
                example = "[\"DEVICE\", \"ASSET\"]",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Set<EntityType> entityTypes,

        @ArraySchema(
                arraySchema = @Schema(
                        description = """
                                List of unique time series keys available on the matched entities, sorted alphabetically.
                                Omitted when timeseries keys were not requested.""",
                        nullable = true
                ),
                schema = @Schema(implementation = KeyInfo.class)
        )
        @Nullable List<KeyInfo> timeseries,

        @Schema(
                description = """
                        Map of attribute scope to the list of unique attribute keys available on the matched entities.
                        Only scopes supported by the matched entity types are included.
                        Omitted when attribute keys were not requested or when none of the requested scopes apply to the matched entity types.""",
                nullable = true
        )
        @Nullable Map<AttributeScope, List<KeyInfo>> attributes
) {

    @Schema(description = "Key name with an optional sample value.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record KeyInfo(
            @Schema(
                    description = "Key name.",
                    example = "temperature",
                    requiredMode = Schema.RequiredMode.REQUIRED
            )
            String key,

            @Schema(
                    description = "Most recent sample value for this key across the matched entities. Omitted when samples were not requested.",
                    nullable = true
            )
            @Nullable KeySample sample
    ) {}

    @Schema(description = "Most recent value and its timestamp.")
    public record KeySample(
            @Schema(
                    description = "Timestamp in milliseconds since epoch.", example = "1707000000000",
                    requiredMode = Schema.RequiredMode.REQUIRED
            )
            long ts,

            @Schema(
                    description = "Sample value.",
                    example = "23.5",
                    requiredMode = Schema.RequiredMode.REQUIRED,
                    implementation = Object.class
            )
            JsonNode value
    ) {}

}
