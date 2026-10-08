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
package org.thingsboard.server.dao.model.sql.ruleenginemonitoring;

import jakarta.persistence.Column;
import jakarta.persistence.ColumnResult;
import jakarta.persistence.ConstructorResult;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.SqlResultSetMappings;
import jakarta.persistence.Table;

import java.util.UUID;

@SuppressWarnings("unused")

@Entity
@Table(name = "rule_node_exec_stats")
@IdClass(RuleNodeExecStatsId.class)
@SqlResultSetMappings({
        @SqlResultSetMapping(
                name = "RuleNodeTsKvEntry",
                classes = @ConstructorResult(
                        targetClass = RuleNodeTsKvEntry.class,
                        columns = {
                                @ColumnResult(name = "bucket_time", type = Long.class),
                                @ColumnResult(name = "exec_count", type = Long.class),
                                @ColumnResult(name = "error_count", type = Long.class),
                                @ColumnResult(name = "total_duration_ms", type = Long.class),
                                @ColumnResult(name = "avg_duration_ms", type = Long.class),
                                @ColumnResult(name = "max_duration_ms", type = Long.class),
                                @ColumnResult(name = "p95_duration_ms", type = Long.class),
                                @ColumnResult(name = "tenant_id", type = UUID.class),
                                @ColumnResult(name = "service_id", type = String.class),
                                @ColumnResult(name = "queue_id", type = UUID.class),
                                @ColumnResult(name = "rule_chain_id", type = UUID.class),
                                @ColumnResult(name = "rule_node_id", type = UUID.class),
                        }
                )
        ),
        @SqlResultSetMapping(
                name = "RuleNodeTableRow",
                classes = @ConstructorResult(
                        targetClass = RuleNodeTableRow.class,
                        columns = {
                                @ColumnResult(name = "exec_count", type = Long.class),
                                @ColumnResult(name = "error_count", type = Long.class),
                                @ColumnResult(name = "total_duration_ms", type = Long.class),
                                @ColumnResult(name = "avg_duration_ms", type = Long.class),
                                @ColumnResult(name = "max_duration_ms", type = Long.class),
                                @ColumnResult(name = "p95_duration_ms", type = Long.class),
                                @ColumnResult(name = "tenant_id", type = UUID.class),
                                @ColumnResult(name = "service_id", type = String.class),
                                @ColumnResult(name = "queue_id", type = UUID.class),
                                @ColumnResult(name = "rule_chain_id", type = UUID.class),
                                @ColumnResult(name = "rule_node_id", type = UUID.class),
                        }
                )
        ),
        @SqlResultSetMapping(
                name = "QueueTimeseriesEntry",
                classes = @ConstructorResult(
                        targetClass = QueueTimeseriesEntry.class,
                        columns = {
                                @ColumnResult(name = "bucket_time", type = Long.class),
                                @ColumnResult(name = "timeout_count", type = Long.class),
                                @ColumnResult(name = "failure_count", type = Long.class),
                                @ColumnResult(name = "success_count", type = Long.class),
                                @ColumnResult(name = "tenant_id", type = UUID.class),
                                @ColumnResult(name = "service_id", type = String.class),
                                @ColumnResult(name = "queue_id", type = UUID.class),
                                @ColumnResult(name = "rule_chain_id", type = UUID.class),
                                @ColumnResult(name = "rule_node_id", type = UUID.class),
                                @ColumnResult(name = "queue_tenant_id", type = UUID.class),
                        }
                )
        ),
        @SqlResultSetMapping(
                name = "QueueTableEntry",
                classes = @ConstructorResult(
                        targetClass = QueueTableEntry.class,
                        columns = {
                                @ColumnResult(name = "timeout_count", type = Long.class),
                                @ColumnResult(name = "failure_count", type = Long.class),
                                @ColumnResult(name = "success_count", type = Long.class),
                                @ColumnResult(name = "tenant_id", type = UUID.class),
                                @ColumnResult(name = "service_id", type = String.class),
                                @ColumnResult(name = "queue_id", type = UUID.class),
                                @ColumnResult(name = "rule_chain_id", type = UUID.class),
                                @ColumnResult(name = "rule_node_id", type = UUID.class),
                                @ColumnResult(name = "queue_tenant_id", type = UUID.class),
                        }
                )
        ),
        @SqlResultSetMapping(
                name = "QueueLagTimeseriesEntry",
                classes = @ConstructorResult(
                        targetClass = QueueLagTimeseriesEntry.class,
                        columns = {
                                @ColumnResult(name = "bucket_time", type = Long.class),
                                @ColumnResult(name = "lag", type = Long.class),
                                @ColumnResult(name = "queue_tenant_id", type = UUID.class),
                                @ColumnResult(name = "queue_id", type = UUID.class),
                        }
                )
        ),
        @SqlResultSetMapping(
                name = "QueueLagTableEntry",
                classes = @ConstructorResult(
                        targetClass = QueueLagTableEntry.class,
                        columns = {
                                @ColumnResult(name = "lag", type = Long.class),
                                @ColumnResult(name = "queue_tenant_id", type = UUID.class),
                                @ColumnResult(name = "queue_id", type = UUID.class),
                        }
                )
        ),
})
public class RuleNodeExecStatsEntity {
    @Id
    @Column(name = "bucket_time")
    private Long bucketTime;

    @Id
    @Column(name = "tenant_id")
    private UUID tenantId;

    @Id
    @Column(name = "queue_id")
    private UUID queueId;

    @Id
    @Column(name = "rule_chain_id")
    private UUID ruleChainId;

    @Id
    @Column(name = "rule_node_id")
    private UUID ruleNodeId;

    @Id
    @Column(name = "service_id")
    private String serviceId;
}
