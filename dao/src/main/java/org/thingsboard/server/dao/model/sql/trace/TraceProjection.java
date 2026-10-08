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

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TraceProjection {
    private String traceId;
    private String serviceName;
    private long startTime;
    private Long endTime;
    private Long duration;
    private Integer spanCount;
    private Integer errorCount;
    private long createdTime;
    private String firstSpanName;
    private Long inQueueTime;
    private Long totalSpanTime;

    private String rootQueueId;
    private String messageType;
    private String rootRuleChainId;
    private Integer ruleNodeCount;
    private Boolean hasErrors;
    private Boolean hasQueueTimeouts;

    public TraceProjection(String traceId, String serviceName, Long startTime, Long endTime, Long duration,
                           Integer spanCount, Integer errorCount, Long createdTime, String firstSpanName,
                           Long totalSpanTime, Long inQueueTime,
                           String rootQueueId, String messageType, String rootRuleChainId,
                           Integer ruleNodeCount,
                           Boolean hasErrors, Boolean hasQueueTimeouts) {
        this.traceId = traceId;
        this.serviceName = serviceName;
        this.startTime = startTime != null ? startTime : 0L;
        this.endTime = endTime;
        this.duration = duration;
        this.spanCount = spanCount;
        this.errorCount = errorCount;
        this.createdTime = createdTime != null ? createdTime : 0L;
        this.firstSpanName = firstSpanName;
        this.totalSpanTime = totalSpanTime;
        this.inQueueTime = inQueueTime;
        this.rootQueueId = rootQueueId;
        this.messageType = messageType;
        this.rootRuleChainId = rootRuleChainId;
        this.ruleNodeCount = ruleNodeCount;
        this.hasErrors = hasErrors;
        this.hasQueueTimeouts = hasQueueTimeouts;
    }
}
