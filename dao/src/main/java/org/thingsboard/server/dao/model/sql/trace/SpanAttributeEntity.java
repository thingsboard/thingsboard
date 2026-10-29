// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.model.sql.trace;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.common.util.JacksonUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * One span attribute, built in memory from {@code span.attributes_jsonb}; there is no attribute table.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpanAttributeEntity {
    private String spanId;
    private String traceId;
    private String attributeKey;
    private String attributeValueType;
    private String attributeValueString;
    private Double attributeValueNumber;
    private Boolean attributeValueBoolean;
    private long createdTime;

    /**
     * The attributes of a span, parsed from its {@code attributes_jsonb}.
     */
    public static List<SpanAttributeEntity> fromSpan(SpanEntity span) {
        Map<String, Object> attributes = parseJsonObject(span.getAttributesJsonb());
        if (attributes.isEmpty()) {
            return Collections.emptyList();
        }

        List<SpanAttributeEntity> result = new ArrayList<>(attributes.size());
        attributes.forEach((key, value) -> {
            SpanAttributeEntity attr = new SpanAttributeEntity();
            attr.setSpanId(span.getSpanId());
            attr.setTraceId(span.getTraceId());
            attr.setAttributeKey(key);
            attr.setCreatedTime(span.getCreatedTime());
            if (value instanceof Boolean boolValue) {
                attr.setAttributeValueType("BOOLEAN");
                attr.setAttributeValueBoolean(boolValue);
            } else if (value instanceof Number numberValue) {
                attr.setAttributeValueType("DOUBLE");
                attr.setAttributeValueNumber(numberValue.doubleValue());
            } else if (value != null) {
                attr.setAttributeValueType("STRING");
                if (value instanceof Map<?, ?> || value instanceof List<?>) {
                    attr.setAttributeValueString(JacksonUtil.toString(value));
                } else {
                    attr.setAttributeValueString(String.valueOf(value));
                }
            } else {
                attr.setAttributeValueType("STRING");
                attr.setAttributeValueString(null);
            }
            result.add(attr);
        });
        return result;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseJsonObject(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            Object value = JacksonUtil.OBJECT_MAPPER.readValue(json, Map.class);
            return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Collections.emptyMap();
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }
}
