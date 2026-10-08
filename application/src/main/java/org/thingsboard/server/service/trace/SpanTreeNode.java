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
import org.thingsboard.server.dao.model.sql.trace.SpanAttributeEntity;
import org.thingsboard.server.dao.model.sql.trace.SpanEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Schema(description = "Span tree node with hierarchical structure")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SpanTreeNode {
    @Schema(description = "Span ID", example = "a1b2c3d4e5f6g7h8")
    private String spanId;

    @Schema(description = "Trace ID", example = "550e8400e29b41d4a716446655440000")
    private String traceId;

    @Schema(description = "Parent span ID (null for root spans)", example = "a1b2c3d4e5f6g7h8")
    private String parentSpanId;

    @Schema(description = "Span name", example = "[Rule Chain] [Transform] [Node Name]")
    private String name;

    @Schema(description = "Span kind", example = "INTERNAL")
    private String kind;

    @Schema(description = "Service name", example = "tb-rule-engine")
    private String serviceName;

    @Schema(description = "Start time in milliseconds", example = "1699123456789")
    private long startTime;

    @Schema(description = "End time in milliseconds", example = "1699123456790")
    private Long endTime;

    @Schema(description = "Duration in milliseconds", example = "1")
    private Long duration;

    @Schema(description = "Status code", example = "OK")
    private String statusCode;

    @Schema(description = "Status message", example = "Success")
    private String statusMessage;

    @Schema(description = "Created time in milliseconds", example = "1699123456789")
    private long createdTime;

    @Schema(description = "Child spans (sorted by start time)")
    @Builder.Default
    private List<SpanTreeNode> children = new ArrayList<>();

    @Schema(description = "Span attributes (key-value pairs)")
    @Builder.Default
    private List<SpanAttributeDTO> attributes = new ArrayList<>();

    public static SpanTreeNode fromEntity(SpanEntity entity) {
        return fromEntity(entity, null);
    }

    public static SpanTreeNode fromEntity(SpanEntity entity, List<SpanAttributeEntity> attributes) {
        List<SpanAttributeDTO> attributeDTOs = attributes != null
                ? attributes.stream().map(SpanAttributeDTO::fromEntity).collect(Collectors.toList())
                : new ArrayList<>();
        return SpanTreeNode.builder()
                .spanId(entity.getSpanId())
                .traceId(entity.getTraceId())
                .parentSpanId(entity.getParentSpanId())
                .name(entity.getName())
                .kind(entity.getKind())
                .serviceName(entity.getServiceName())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .duration(entity.getDuration())
                .statusCode(entity.getStatusCode())
                .statusMessage(entity.getStatusMessage())
                .createdTime(entity.getCreatedTime())
                .children(new ArrayList<>())
                .attributes(attributeDTOs)
                .build();
    }
}
