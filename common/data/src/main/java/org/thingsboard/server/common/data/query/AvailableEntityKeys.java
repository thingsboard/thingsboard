/**
 * Copyright © 2016-2025 The Thingsboard Authors
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

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import org.thingsboard.server.common.data.EntityType;

import java.util.List;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Objects.requireNonNullElse;

@Schema(
        description = "Contains unique time series and attribute key names discovered from entities matching a query. Used primarily for UI hints such as autocomplete suggestions."
)
public record AvailableEntityKeys(
        @Schema(
                description = "Set of entity types found among the matched entities.",
                example = "[\"DEVICE\", \"ASSET\"]",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Set<EntityType> entityTypes,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        @ArraySchema(
                arraySchema = @Schema(description = "List of unique time series key names available on the matched entities."),
                schema = @Schema(implementation = String.class, example = "temperature"),
                uniqueItems = true
        )
        List<String> timeseries,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        @ArraySchema(
                arraySchema = @Schema(description = "List of unique attribute key names available on the matched entities."),
                schema = @Schema(implementation = String.class, example = "serialNumber"),
                uniqueItems = true
        )
        List<String> attribute
) {

    public AvailableEntityKeys {
        entityTypes = requireNonNullElse(entityTypes, emptySet());
        timeseries = requireNonNullElse(timeseries, emptyList());
        attribute = requireNonNullElse(attribute, emptyList());
    }

    public static AvailableEntityKeys none() {
        return new AvailableEntityKeys(emptySet(), emptyList(), emptyList());
    }

}
