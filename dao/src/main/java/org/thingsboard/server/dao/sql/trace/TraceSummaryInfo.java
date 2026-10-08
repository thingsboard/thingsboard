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
package org.thingsboard.server.dao.sql.trace;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public record TraceSummaryInfo(String traceId, UUID tenantId, List<String> serviceNames, long startTime,
                               long endTime, long duration, int spanCount, int errorCount, long createdTime,
                               String firstSpanName, long totalSpanTime, long inQueueTime, UUID rootQueueId,
                               String rootMessageType, String rootRuleChainId, List<String> originatorIds,
                               List<String> queueNames, List<String> ruleChainIds, List<String> ruleNodeIds,
                               List<String> messageTypes, List<String> messageIds, boolean hasErrors,
                               boolean hasQueueTimeouts) {
    public TraceSummaryInfo {
        serviceNames = safeList(serviceNames);
        rootMessageType = safeString(rootMessageType);
        rootRuleChainId = safeString(rootRuleChainId);
        originatorIds = safeList(originatorIds);
        queueNames = safeList(queueNames);
        ruleChainIds = safeList(ruleChainIds);
        ruleNodeIds = safeList(ruleNodeIds);
        messageTypes = safeList(messageTypes);
        messageIds = safeList(messageIds);
    }

    private static List<String> safeList(List<String> values) {
        return values == null ? Collections.emptyList() : List.copyOf(values);
    }

    private static String safeString(String value) {
        return value == null ? "" : value;
    }
}
