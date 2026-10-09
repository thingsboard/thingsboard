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
