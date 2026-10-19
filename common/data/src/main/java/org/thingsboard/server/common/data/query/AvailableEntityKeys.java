// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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

        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                description = "List of unique time series key names available on the matched entities."
        )
        @ArraySchema(
                schema = @Schema(implementation = String.class, example = "temperature"),
                uniqueItems = true
        )
        List<String> timeseries,

        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                description = "List of unique attribute key names available on the matched entities."
        )
        @ArraySchema(
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
