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
import jakarta.persistence.ColumnResult;
import jakarta.persistence.ConstructorResult;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.SqlResultSetMappings;
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
@SqlResultSetMappings({
        @SqlResultSetMapping(
                name = "TraceMapping",
                classes = @ConstructorResult(
                        targetClass = TraceProjection.class,
                        columns = {
                                @ColumnResult(name = "trace_id", type = String.class),
                                @ColumnResult(name = "service_name", type = String.class),
                                @ColumnResult(name = "start_time", type = Long.class),
                                @ColumnResult(name = "end_time", type = Long.class),
                                @ColumnResult(name = "duration", type = Long.class),
                                @ColumnResult(name = "span_count", type = Integer.class),
                                @ColumnResult(name = "error_count", type = Integer.class),
                                @ColumnResult(name = "created_time", type = Long.class),
                                @ColumnResult(name = "first_span_name", type = String.class),
                                @ColumnResult(name = "total_span_time", type = Long.class),
                                @ColumnResult(name = "in_queue_time", type = Long.class),
                                @ColumnResult(name = "root_queue_id", type = String.class),
                                @ColumnResult(name = "message_type", type = String.class),
                                @ColumnResult(name = "root_rule_chain_id", type = String.class),
                                @ColumnResult(name = "rule_node_count", type = Integer.class),
                                @ColumnResult(name = "has_errors", type = Boolean.class),
                                @ColumnResult(name = "has_queue_timeouts", type = Boolean.class),
                        }
                )
        ),
        @SqlResultSetMapping(
                name = "TraceStatisticsRow",
                classes = @ConstructorResult(
                        targetClass = TraceStatisticsRow.class,
                        columns = {
                                @ColumnResult(name = "total_traces", type = Long.class),
                                @ColumnResult(name = "success_traces", type = Long.class),
                                @ColumnResult(name = "failed_traces", type = Long.class),
                                @ColumnResult(name = "timeout_traces", type = Long.class),
                                @ColumnResult(name = "p10", type = Double.class),
                                @ColumnResult(name = "p50", type = Double.class),
                                @ColumnResult(name = "p90", type = Double.class),
                                @ColumnResult(name = "p95", type = Double.class),
                                @ColumnResult(name = "p99", type = Double.class),
                        }
                )
        ),
        @SqlResultSetMapping(
                name = "TraceTimeBucketRow",
                classes = @ConstructorResult(
                        targetClass = TraceTimeBucketRow.class,
                        columns = {
                                @ColumnResult(name = "bucket_time", type = Long.class),
                                @ColumnResult(name = "total_traces", type = Long.class),
                                @ColumnResult(name = "success_traces", type = Long.class),
                                @ColumnResult(name = "failed_traces", type = Long.class),
                        }
                )
        ),
        @SqlResultSetMapping(
                name = "FailureRateBucketRow",
                classes = @ConstructorResult(
                        targetClass = FailureRateBucketRow.class,
                        columns = {
                                @ColumnResult(name = "bucket_time", type = Long.class),
                                @ColumnResult(name = "total_traces", type = Long.class),
                                @ColumnResult(name = "failed_traces", type = Long.class),
                                @ColumnResult(name = "failure_rate_pct", type = Double.class),
                        }
                )
        ),
        @SqlResultSetMapping(
                name = "RuleChainStatisticsRow",
                classes = @ConstructorResult(
                        targetClass = RuleChainStatisticsRow.class,
                        columns = {
                                @ColumnResult(name = "rule_chain_id", type = String.class),
                                @ColumnResult(name = "span_count", type = Long.class),
                                @ColumnResult(name = "success_count", type = Long.class),
                                @ColumnResult(name = "error_count", type = Long.class),
                                @ColumnResult(name = "avg_duration_seconds", type = Double.class),
                                @ColumnResult(name = "p10", type = Double.class),
                                @ColumnResult(name = "p50", type = Double.class),
                                @ColumnResult(name = "p90", type = Double.class),
                                @ColumnResult(name = "p95", type = Double.class),
                                @ColumnResult(name = "p99", type = Double.class),
                        }
                )
        ),
        @SqlResultSetMapping(
                name = "RuleNodeStatisticsRow",
                classes = @ConstructorResult(
                        targetClass = RuleNodeStatisticsRow.class,
                        columns = {
                                @ColumnResult(name = "rule_chain_id", type = String.class),
                                @ColumnResult(name = "rule_node_id", type = String.class),
                                @ColumnResult(name = "rule_node_type", type = String.class),
                                @ColumnResult(name = "span_count", type = Long.class),
                                @ColumnResult(name = "success_count", type = Long.class),
                                @ColumnResult(name = "error_count", type = Long.class),
                                @ColumnResult(name = "avg_duration_seconds", type = Double.class),
                                @ColumnResult(name = "p10", type = Double.class),
                                @ColumnResult(name = "p50", type = Double.class),
                                @ColumnResult(name = "p90", type = Double.class),
                                @ColumnResult(name = "p95", type = Double.class),
                                @ColumnResult(name = "p99", type = Double.class),
                        }
                )
        ),
        @SqlResultSetMapping(
                name = "SpanTrendBucketRow",
                classes = @ConstructorResult(
                        targetClass = SpanTrendBucketRow.class,
                        columns = {
                                @ColumnResult(name = "bucket_time", type = Long.class),
                                @ColumnResult(name = "span_count", type = Long.class),
                                @ColumnResult(name = "success_count", type = Long.class),
                                @ColumnResult(name = "error_count", type = Long.class),
                                @ColumnResult(name = "avg_duration_seconds", type = Double.class),
                                @ColumnResult(name = "p50", type = Double.class),
                                @ColumnResult(name = "p90", type = Double.class),
                                @ColumnResult(name = "p95", type = Double.class),
                                @ColumnResult(name = "p99", type = Double.class),
                        }
                )
        ),
        @SqlResultSetMapping(
                name = "TraceTrendBucketRow",
                classes = @ConstructorResult(
                        targetClass = TraceTrendBucketRow.class,
                        columns = {
                                @ColumnResult(name = "bucket_time", type = Long.class),
                                @ColumnResult(name = "total_traces", type = Long.class),
                                @ColumnResult(name = "failed_traces", type = Long.class),
                                @ColumnResult(name = "timeout_count", type = Long.class),
                                @ColumnResult(name = "failure_rate_pct", type = Double.class),
                                @ColumnResult(name = "avg_duration", type = Double.class),
                                @ColumnResult(name = "avg_in_queue_time", type = Double.class),
                                @ColumnResult(name = "avg_total_span_time", type = Double.class),
                                @ColumnResult(name = "p50_duration", type = Double.class),
                                @ColumnResult(name = "p95_duration", type = Double.class),
                                @ColumnResult(name = "p99_duration", type = Double.class),
                        }
                )
        ),
        @SqlResultSetMapping(
                name = "RuleNodeFailureRow",
                classes = @ConstructorResult(
                        targetClass = RuleNodeFailureRow.class,
                        columns = {
                                @ColumnResult(name = "rule_chain_name", type = String.class),
                                @ColumnResult(name = "rule_chain_id", type = String.class),
                                @ColumnResult(name = "rule_node_name", type = String.class),
                                @ColumnResult(name = "rule_node_id", type = String.class),
                                @ColumnResult(name = "rule_node_type", type = String.class),
                                @ColumnResult(name = "total_span_count", type = Long.class),
                                @ColumnResult(name = "error_span_count", type = Long.class),
                        }
                )
        ),
})
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
