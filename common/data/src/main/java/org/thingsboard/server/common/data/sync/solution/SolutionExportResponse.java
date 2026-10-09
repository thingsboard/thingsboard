// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.sync.solution;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "Solution export response containing the exported solution data and any dependency warnings.")
@Data
public class SolutionExportResponse {

    @Schema(description = "The exported solution data containing all requested entities grouped by type.")
    private SolutionData solution;
    @Schema(description = "List of dependency warnings. Generated when exported entities reference other entities " +
                          "that are not included in the export (e.g. a device profile references a rule chain that was not selected for export).",
            example = "[\"DEVICE_PROFILE 'My DP' references RuleChain (784f394c-42b6-435a-983c-b7beff2784f9) which is not in the export selection\"]")
    private List<String> warnings;

}
