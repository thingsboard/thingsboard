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
@Schema(description = "Trace statistics for a time range")
public class TraceStatisticsDTO {
    @Schema(description = "Total number of traces in the time range", example = "1543")
    private long totalTraces;

    @Schema(description = "Number of successful traces (error_count = 0)", example = "1498")
    private long successTraces;

    @Schema(description = "Number of failed traces (error_count > 0)", example = "45")
    private long failedTraces;

    @Schema(description = "Number of timed-out traces", example = "12")
    private long timeoutTraces;

    @Schema(description = "Success rate as percentage (excludes both failures and timeouts)", example = "97.08")
    private double successRate;

    @Schema(description = "Duration percentiles in seconds")
    private DurationPercentilesDTO durationPercentilesSeconds;

    @Schema(description = "Time range in milliseconds")
    private TimeRangeDTO timeRangeMs;

    @Schema(description = "bucket_time of the last aggregation row used (epoch ms)")
    private Long lastBucketTime;

    @Schema(description = "Granularity of the table that produced lastBucketTime")
    private BucketGranularity lastBucketInterval;

    public static double calculateSuccessRate(long totalTraces, long failedTraces, long timeoutTraces) {
        if (totalTraces == 0) {
            return 0.0;
        }
        return (double) (totalTraces - failedTraces - timeoutTraces) / totalTraces * 100.0;
    }
}
