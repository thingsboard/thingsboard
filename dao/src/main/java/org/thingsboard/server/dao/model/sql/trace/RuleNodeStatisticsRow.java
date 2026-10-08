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

public class RuleNodeStatisticsRow {
    public final String ruleChainId;
    public final String ruleNodeId;
    public final String ruleNodeType;
    public final Long spanCount;
    public final Long successCount;
    public final Long errorCount;
    public final Double avgDurationSeconds;
    public final Double p10;
    public final Double p50;
    public final Double p90;
    public final Double p95;
    public final Double p99;

    public RuleNodeStatisticsRow(String ruleChainId, String ruleNodeId, String ruleNodeType,
                                  Long spanCount, Long successCount, Long errorCount,
                                  Double avgDurationSeconds, Double p10, Double p50,
                                  Double p90, Double p95, Double p99) {
        this.ruleChainId = ruleChainId;
        this.ruleNodeId = ruleNodeId;
        this.ruleNodeType = ruleNodeType;
        this.spanCount = spanCount;
        this.successCount = successCount;
        this.errorCount = errorCount;
        this.avgDurationSeconds = avgDurationSeconds;
        this.p10 = p10;
        this.p50 = p50;
        this.p90 = p90;
        this.p95 = p95;
        this.p99 = p99;
    }
}
