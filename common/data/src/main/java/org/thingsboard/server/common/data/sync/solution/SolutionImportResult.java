// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.sync.solution;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.thingsboard.server.common.data.EntityType;

import java.util.Map;
import java.util.UUID;

@Schema(description = "Result of a solution import operation.")
@Data
public class SolutionImportResult {

    @Schema(description = "'true' if all entities were imported successfully.", example = "true")
    private boolean success;
    @Schema(description = "Number of newly created entities per entity type. " +
                          "Entity types with zero created entities are omitted. " +
                          "Entity groups are reported under the ENTITY_GROUP key regardless of " +
                          "their inner type (e.g. a user group and a device group both contribute " +
                          "to ENTITY_GROUP).",
            example = "{\"RULE_CHAIN\": 2, \"DEVICE_PROFILE\": 1, \"DEVICE\": 3, \"ENTITY_GROUP\": 1}")
    private Map<EntityType, Integer> created;
    @Schema(description = "Mapping from external entity IDs (as they appear in the solution file) " +
                          "to the internal entity IDs assigned during import.")
    private Map<UUID, UUID> idMapping;

}
