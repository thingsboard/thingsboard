/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2026 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
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
