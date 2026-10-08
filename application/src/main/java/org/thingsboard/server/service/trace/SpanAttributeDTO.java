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
