// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
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
