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

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.dao.model.sql.trace.SpanEventEntity;

import java.util.UUID;

@Schema(description = "Span event data transfer object")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpanEventDTO {
    @Schema(description = "Event ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    @Schema(description = "Span ID", example = "a1b2c3d4e5f6g7h8")
    private String spanId;

    @Schema(description = "Trace ID", example = "550e8400e29b41d4a716446655440000")
    private String traceId;

    @Schema(description = "Event name", example = "exception")
    private String eventName;

    @Schema(description = "Event time in milliseconds", example = "1699123456790")
    private long eventTime;

    @Schema(description = "Event attributes JSON", example = "{\"exception.type\":\"java.lang.RuntimeException\"}")
    private String attributesJson;

    @Schema(description = "Created time in milliseconds", example = "1699123456789")
    private long createdTime;

    public static SpanEventDTO fromEntity(SpanEventEntity entity) {
        return SpanEventDTO.builder()
                .id(entity.getId())
                .spanId(entity.getSpanId())
                .traceId(entity.getTraceId())
                .eventName(entity.getEventName())
                .eventTime(entity.getEventTime())
                .attributesJson(entity.getAttributesJson())
                .createdTime(entity.getCreatedTime())
                .build();
    }
}
