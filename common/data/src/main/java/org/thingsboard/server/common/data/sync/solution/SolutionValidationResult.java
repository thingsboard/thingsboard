// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.sync.solution;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.thingsboard.server.common.data.EntityType;

import java.util.List;
import java.util.Map;

@Schema(description = "Result of a solution validation (dry-run). Checks structural validity " +
                      "and dependency references without modifying any data.")
@Data
public class SolutionValidationResult {

    @Schema(description = "'true' if the solution can be imported without errors. " +
                          "'false' if there are structural issues (empty entities, unsupported types, malformed data).", example = "true")
    private boolean valid;
    @Schema(description = "Number of entities per type found in the solution file.",
            example = "{\"RULE_CHAIN\": 2, \"DEVICE_PROFILE\": 1}")
    private Map<EntityType, Integer> entitySummary;
    @Schema(description = "List of blocking issues that would prevent import (e.g. unsupported entity types, missing or malformed entity data).",
            example = "[\"Unsupported entity types: UNKNOWN_TYPE\"]")
    private List<String> conflicts;
    @Schema(description = "List of non-blocking warnings (e.g. missing dependency references).",
            example = "[\"DEVICE_PROFILE 'My DP' references rule chain (uuid) which is not included in the solution file\"]")
    private List<String> warnings;

}
