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

public class TraceTrendBucketRow {
    public final Long bucketTime;
    public final Long totalTraces;
    public final Long failedTraces;
    public final Long timeoutCount;
    public final Double failureRatePct;
    public final Double avgDuration;
    public final Double avgInQueueTime;
    public final Double avgTotalSpanTime;
    public final Double p50Duration;
    public final Double p95Duration;
    public final Double p99Duration;

    public TraceTrendBucketRow(Long bucketTime, Long totalTraces, Long failedTraces, Long timeoutCount,
                               Double failureRatePct, Double avgDuration,
                               Double avgInQueueTime, Double avgTotalSpanTime,
                               Double p50Duration, Double p95Duration, Double p99Duration) {
        this.bucketTime = bucketTime;
        this.totalTraces = totalTraces;
        this.failedTraces = failedTraces;
        this.timeoutCount = timeoutCount;
        this.failureRatePct = failureRatePct;
        this.avgDuration = avgDuration;
        this.avgInQueueTime = avgInQueueTime;
        this.avgTotalSpanTime = avgTotalSpanTime;
        this.p50Duration = p50Duration;
        this.p95Duration = p95Duration;
        this.p99Duration = p99Duration;
    }
}
