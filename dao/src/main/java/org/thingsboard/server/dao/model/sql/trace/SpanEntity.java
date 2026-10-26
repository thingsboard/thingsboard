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

import java.util.UUID;

@Entity
@Table(name = ModelConstants.SPAN_TABLE_NAME)
@IdClass(SpanEntityId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpanEntity {
    @Id
    @Column(name = "span_id", nullable = false)
    private String spanId;

    @Id
    @Column(name = "trace_id", nullable = false)
    private String traceId;

    @Column(name = "tenant_id", columnDefinition = "uuid")
    private UUID tenantId;

    @Column(name = "parent_span_id")
    private String parentSpanId;

    @Column(name = "name", nullable = false, length = 1000)
    private String name;

    @Column(name = "kind", length = 50)
    private String kind;

    @Column(name = "service_name")
    private String serviceName;

    @Column(name = "attributes_jsonb", columnDefinition = "jsonb")
    private String attributesJsonb;

    @Column(name = "events_jsonb", columnDefinition = "jsonb")
    private String eventsJsonb;

    @Column(name = "links_jsonb", columnDefinition = "jsonb")
    private String linksJsonb;

    @Column(name = "start_time", nullable = false)
    private long startTime;

    @Column(name = "end_time")
    private Long endTime;

    @Column(name = "duration")
    private Long duration;

    @Column(name = "status_code", length = 20)
    private String statusCode;

    @Column(name = "status_message", length = 5000)
    private String statusMessage;

    @Column(name = "created_time", nullable = false)
    private long createdTime;
}
