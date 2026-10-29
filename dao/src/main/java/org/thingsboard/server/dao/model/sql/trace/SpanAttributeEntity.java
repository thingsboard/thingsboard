// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.model.sql.trace;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
}
