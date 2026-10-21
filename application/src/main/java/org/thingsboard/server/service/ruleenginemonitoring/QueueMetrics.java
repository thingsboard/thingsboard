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
package org.thingsboard.server.service.ruleenginemonitoring;

/**
 * Per-key outcome counter that lives in the accumulator across flushes; same drain/retire contract as
 * {@link RuleNodeExecMetrics}.
 */
public class QueueMetrics {
    private long count;
    private int idleFlushes;
    private boolean retired;

    /**
     * @return false if the entry has retired; the caller must re-fetch the entry from the accumulator and retry
     */
    public synchronized boolean add(long delta) {
        if (retired) {
            return false;
        }
        count += delta;
        return true;
    }

    /**
     * Returns the count since the previous drain and resets it.
     */
    public synchronized long drain(int maxIdleFlushes) {
        long drained = count;
        count = 0;
        if (drained == 0) {
            if (++idleFlushes >= maxIdleFlushes) {
                retired = true;
            }
        } else {
            idleFlushes = 0;
        }
        return drained;
    }

    public synchronized boolean isRetired() {
        return retired;
    }
}
