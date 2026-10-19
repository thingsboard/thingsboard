// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.agent;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Schema
public class BulkOperationPreview {

    @Schema(description = "Total number of apps targeted by the bulk operation")
    private int total;

    @Schema(description = "Number of apps that would be submitted (total minus skipped)")
    private int eligible;

    @Schema(description = "Skip count per reason for the targeted apps")
    private Map<BulkOperationResult.SkipReason, Integer> skippedCountsByReason;

    @Schema(description = "Sample of skipped apps, capped per reason. Use the run history to inspect the full list.")
    private List<BulkOperationResult.SkippedApp> skippedSample;
}
