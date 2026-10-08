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
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Time bucket with aggregated trace metrics for trend visualization")
public class TraceTrendBucketDTO {
    @Schema(description = "Bucket start time in milliseconds (epoch)", example = "1706000000000")
    private long timestamp;

    @Schema(description = "Total number of traces in this bucket", example = "150")
    private long totalTraces;

    @Schema(description = "Number of failed traces (error_count > 0) in this bucket", example = "8")
    private long failedTraces;

    @Schema(description = "Number of timed-out traces in this bucket", example = "3")
    private long timeoutCount;

    @Schema(description = "Failure rate percentage: (failedTraces / totalTraces) * 100. Null when totalTraces = 0", example = "5.33")
    private Double failureRatePct;

    @Schema(description = "Average end-to-end trace duration in seconds. Null when totalTraces = 0", example = "0.042")
    private Double avgDurationSeconds;

    @Schema(description = "Average in-queue time in seconds (null when totalTraces = 0 or all traces have fewer than 2 spans)", example = "0.005")
    private Double avgInQueueTimeSeconds;

    @Schema(description = "Average total span time per trace in seconds (sum of all span durations averaged). Null when totalTraces = 0", example = "0.038")
    private Double avgTotalSpanTimeSeconds;

    @Schema(description = "P50 (median) trace duration in seconds. Null when totalTraces = 0", example = "0.025")
    private Double p50DurationSeconds;

    @Schema(description = "P95 trace duration in seconds. Null when totalTraces = 0", example = "0.12")
    private Double p95DurationSeconds;

    @Schema(description = "P99 trace duration in seconds. Null when totalTraces = 0", example = "0.22")
    private Double p99DurationSeconds;
}
