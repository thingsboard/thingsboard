// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.trace;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Aggregated execution path")
public class TracePathDTO {
    @Schema(description = "Deterministic path id (hash of the path's structure)")
    private String pathId;

    @Schema(description = "Root queue id (UUID), nullable")
    private String rootQueueId;

    @Schema(description = "Root queue name, resolved server-side", example = "Main")
    private String rootQueueName;

    @Schema(description = "Root message type", example = "POST_TELEMETRY_REQUEST")
    private String rootMessageType;

    @Schema(description = "Root rule chain id (UUID), nullable")
    private String rootRuleChainId;

    @Schema(description = "Root rule chain name, resolved server-side", example = "Root Rule Chain")
    private String rootRuleChainName;

    @Schema(description = "Number of traces processed/aggregated for this path in the range")
    private long processedTraces;

    @Schema(description = "Number of processed traces that contain a Queue Message Processing Time Out span")
    private long processedTracesWithTimeout;

    @Schema(description = "Number of processed traces that contain at least one ERROR span")
    private long processedTracesWithError;

    @Schema(description = "Number of traces stored (sampled, with persisted spans) for this path in the range")
    private long storedTraces;

    @Schema(description = "Total trace duration in milliseconds across processed traces in the range")
    private long totalTime;

    @Schema(description = "Maximum trace duration in milliseconds for this path in the range")
    private long maxTime;

    @Schema(description = "Total in-queue time in milliseconds across processed traces in the range")
    private long totalInQueueTime;

    @Schema(description = "Total rule-node processing time in milliseconds across processed traces in the range")
    private long totalRuleNodeProcessingTime;

    @Schema(description = "Total rule-node errors across the path in the range")
    private long errorCount;

    @Schema(description = "Latest time this path was observed (epoch ms)")
    private long lastObserved;
}
