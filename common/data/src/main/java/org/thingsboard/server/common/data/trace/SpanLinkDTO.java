// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.trace;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Schema(description = "Span link data transfer object")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpanLinkDTO {
    @Schema(description = "Link ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    @Schema(description = "Span ID", example = "a1b2c3d4e5f6g7h8")
    private String spanId;

    @Schema(description = "Trace ID", example = "550e8400e29b41d4a716446655440000")
    private String traceId;

    @Schema(description = "Linked trace ID", example = "660e8400e29b41d4a716446655440001")
    private String linkedTraceId;

    @Schema(description = "Linked span ID", example = "b1c2d3e4f5g6h7i8")
    private String linkedSpanId;

    @Schema(description = "Link attributes JSON", example = "{\"link.type\":\"follows_from\"}")
    private String attributesJson;

    @Schema(description = "Created time in milliseconds", example = "1699123456789")
    private long createdTime;
}
