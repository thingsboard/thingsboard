/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.integration.activity;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.transport.activity.AbstractActivityManager;
import org.thingsboard.server.common.transport.activity.ActivityReportCallback;
import org.thingsboard.server.common.transport.activity.ActivityState;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "integrations.activity", value = "reporting_strategy", havingValue = "last")
public class LastOnlyIntegrationActivityManager extends AbstractActivityManager<IntegrationActivityKey, ActivityState> {

    private final ConcurrentMap<IntegrationActivityKey, ActivityState> states = new ConcurrentHashMap<>();

    @Override
    protected void doOnActivity(IntegrationActivityKey activityKey, Supplier<ActivityState> newStateSupplier) {
        long newLastRecordedTime = System.currentTimeMillis();
        states.compute(activityKey, (__, activityState) -> {
            if (activityState == null) {
                activityState = newStateSupplier.get();
            }
            if (activityState.getLastRecordedTime() < newLastRecordedTime) {
                activityState.setLastRecordedTime(newLastRecordedTime);
            }
            return activityState;
        });
    }

    @Override
    public void onReportingPeriodEnd() {
        long expirationTime = System.currentTimeMillis() - reportingPeriodMillis;
        for (Map.Entry<IntegrationActivityKey, ActivityState> entry : states.entrySet()) {
            var activityKey = entry.getKey();
            var activityState = entry.getValue();
            long lastRecordedTime = activityState.getLastRecordedTime();
            // if there were no activities during the reporting period, we should remove the entry to prevent memory leaks
            if (lastRecordedTime < expirationTime) {
                states.remove(activityKey);
            }
            if (activityState.getLastReportedTime() < lastRecordedTime) {
                reporter.report(activityKey, lastRecordedTime, activityState, new ActivityReportCallback<>() {
                    @Override
                    public void onSuccess(IntegrationActivityKey key, long reportedTime) {
                        updateLastReportedTime(key, reportedTime);
                    }

                    @Override
                    public void onFailure(IntegrationActivityKey key, Throwable t) {
                        log.debug("[{}] Failed to report last activity event in a period for device with id: [{}].", activityKey.getTenantId().getId(), activityKey.getDeviceId().getId());
                    }
                });
            }
        }
    }

    private void updateLastReportedTime(IntegrationActivityKey key, long newLastReportedTime) {
        states.computeIfPresent(key, (__, activityState) -> {
            activityState.setLastReportedTime(Math.max(activityState.getLastReportedTime(), newLastReportedTime));
            return activityState;
        });
    }

}
