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
@Schema(description = "Rule Node statistics")
public class RuleNodeStatisticsDTO {
    @Schema(description = "Rule Chain name", example = "Root Rule Chain")
    private String ruleChainName;

    @Schema(description = "Rule Chain ID (UUID), nullable; available when span attributes include rule.chain.id", example = "123e4567-e89b-12d3-a456-426614174000")
    private String ruleChainId;

    @Schema(description = "Rule Node name (class and user-specified name)", example = "[Transform] Parse JSON")
    private String ruleNodeName;

    @Schema(description = "Rule Node ID (UUID), nullable; available when span attributes include rule.node.id", example = "987fcdeb-51a2-43d7-9f8e-123456789abc")
    private String ruleNodeId;

    @Schema(description = "Rule Node type/class", example = "org.thingsboard.rule.engine.transform.TbTransformNode")
    private String ruleNodeType;

    @Schema(description = "Total count of spans for this Rule Node", example = "1234")
    private long spanCount;

    @Schema(description = "Count of successful spans (status_code = 'OK')", example = "1200")
    private long successCount;

    @Schema(description = "Count of failed spans (status_code = 'ERROR')", example = "34")
    private long errorCount;

    @Schema(description = "Success rate as percentage", example = "97.24")
    private double successRate;

    @Schema(description = "Average duration in seconds", example = "0.015")
    private double avgDurationSeconds;

    @Schema(description = "Duration percentiles in seconds")
    private DurationPercentilesDTO durationPercentilesSeconds;

    @Schema(description = "bucket_time of the last aggregation row used (epoch ms)")
    private Long lastBucketTime;

    @Schema(description = "Granularity of the table that produced lastBucketTime")
    private BucketGranularity lastBucketInterval;

    public static double calculateSuccessRate(long successCount, long spanCount) {
        if (spanCount == 0) {
            return 0.0;
        }
        return (double) successCount / spanCount * 100.0;
    }
}
