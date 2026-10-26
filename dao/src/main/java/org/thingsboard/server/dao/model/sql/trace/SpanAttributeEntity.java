// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.model.sql.trace;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.dao.model.ModelConstants;

@Entity
@Table(name = ModelConstants.SPAN_ATTRIBUTE_TABLE_NAME)
@IdClass(SpanAttributeEntityId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpanAttributeEntity {
    @Id
    @Column(name = "span_id", nullable = false)
    private String spanId;

    @Id
    @Column(name = "trace_id", nullable = false)
    private String traceId;

    @Id
    @Column(name = "attribute_key", nullable = false, length = 500)
    private String attributeKey;

    @Column(name = "attribute_value_type", nullable = false, length = 50)
    private String attributeValueType;

    @Column(name = "attribute_value_string", length = 10000)
    private String attributeValueString;

    @Column(name = "attribute_value_number")
    private Double attributeValueNumber;

    @Column(name = "attribute_value_boolean")
    private Boolean attributeValueBoolean;

    @Column(name = "created_time", nullable = false)
    private long createdTime;
}
