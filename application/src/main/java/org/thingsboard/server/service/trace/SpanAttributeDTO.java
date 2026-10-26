// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.trace;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.dao.model.sql.trace.SpanAttributeEntity;

@Schema(description = "Span attribute data transfer object")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SpanAttributeDTO {
    @Schema(description = "Trace ID this attribute belongs to", example = "4bf92f3577b34da6a3ce929d0e0e4736")
    private String traceId;

    @Schema(description = "Span ID this attribute belongs to", example = "00f067aa0ba902b7")
    private String spanId;

    @Schema(description = "Attribute key", example = "queue.name")
    private String attributeKey;

    @Schema(description = "Attribute value type", example = "STRING")
    private String attributeValueType;

    @Schema(description = "String attribute value", example = "Main")
    private String attributeValueString;

    @Schema(description = "Numeric attribute value", example = "42.0")
    private Double attributeValueNumber;

    @Schema(description = "Boolean attribute value", example = "true")
    private Boolean attributeValueBoolean;

    public static SpanAttributeDTO fromEntity(SpanAttributeEntity entity) {
        return SpanAttributeDTO.builder()
                .traceId(entity.getTraceId())
                .spanId(entity.getSpanId())
                .attributeKey(entity.getAttributeKey())
                .attributeValueType(entity.getAttributeValueType())
                .attributeValueString(entity.getAttributeValueString())
                .attributeValueNumber(entity.getAttributeValueNumber())
                .attributeValueBoolean(entity.getAttributeValueBoolean())
                .build();
    }
}
