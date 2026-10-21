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
package org.thingsboard.server.service.trace;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.dao.model.sql.trace.TraceProjection;

@Schema(description = "Trace entity with first span name")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Trace {
    @Schema(description = "Trace ID", example = "550e8400e29b41d4a716446655440000")
    private String traceId;

    @Schema(description = "Service name", example = "thingsboard-rule-engine")
    private String serviceName;

    @Schema(description = "Start time in milliseconds", example = "1699123456789")
    private long startTime;

    @Schema(description = "End time in milliseconds", example = "1699123456790")
    private Long endTime;

    @Schema(description = "Trace duration in milliseconds (wall-clock from first span start to last span end)", example = "1000")
    private Long duration;

    @Schema(description = "Total number of spans", example = "10")
    private Integer spanCount;

    @Schema(description = "Number of error spans", example = "0")
    private Integer errorCount;

    @Schema(description = "Created time in milliseconds", example = "1699123456789")
    private long createdTime;

    @Schema(description = "Name of the first span in the trace (ordered by start time)", example = "[Rule Chain] [Transform] [Node Name]")
    private String firstSpanName;

    @Schema(description = "In-queue time in milliseconds: part of trace duration when no span was executing (gaps between/within span execution). Duration = active time + in-queue time.", example = "5")
    private Long inQueueTime;

    @Schema(description = "Total span time in milliseconds (sum of all individual span durations). Can exceed duration when spans run in parallel (e.g. switch branches).", example = "12")
    private Long totalSpanTime;

    @Schema(description = "Root queue id (UUID)")
    private String rootQueueId;
    @Schema(description = "Root queue name")
    private String queueName;
    @Schema(description = "Root message type")
    private String messageType;
    @Schema(description = "Root rule chain id (UUID), name resolved by the UI")
    private String rootRuleChainId;
    @Schema(description = "Distinct rule nodes touched")
    private Integer ruleNodeCount;
    @Schema(description = "Whether the trace had any error span")
    private Boolean hasErrors;
    @Schema(description = "Whether the trace had any queue timeout")
    private Boolean hasQueueTimeouts;

    public static Trace fromEntity(TraceProjection entity) {
        return fromEntity(entity, null);
    }

    public static Trace fromEntity(TraceProjection entity, String rootQueueName) {
        return Trace.builder()
                .rootQueueId(entity.getRootQueueId())
                .queueName(rootQueueName)
                .messageType(entity.getMessageType())
                .rootRuleChainId(entity.getRootRuleChainId())
                .ruleNodeCount(entity.getRuleNodeCount())
                .hasErrors(entity.getHasErrors())
                .hasQueueTimeouts(entity.getHasQueueTimeouts())
                .traceId(entity.getTraceId())
                .serviceName(entity.getServiceName())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .duration(entity.getDuration())
                .spanCount(entity.getSpanCount())
                .errorCount(entity.getErrorCount())
                .createdTime(entity.getCreatedTime())
                .firstSpanName(entity.getFirstSpanName())
                .inQueueTime(entity.getInQueueTime())
                .totalSpanTime(entity.getTotalSpanTime())
                .build();
    }
}
