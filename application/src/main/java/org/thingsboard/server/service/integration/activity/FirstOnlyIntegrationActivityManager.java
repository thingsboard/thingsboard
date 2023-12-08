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

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.util.Pair;
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
@ConditionalOnProperty(prefix = "integrations.activity", value = "reporting_strategy", havingValue = "first")
public class FirstOnlyIntegrationActivityManager extends AbstractActivityManager<IntegrationActivityKey, ActivityState> {

    private final ConcurrentMap<IntegrationActivityKey, ActivityStateWrapper> states = new ConcurrentHashMap<>();

    @Data
    private static class ActivityStateWrapper {

        volatile ActivityState state;
        volatile boolean alreadyBeenReported;

    }

    @Override
    protected void doOnActivity(IntegrationActivityKey activityKey, Supplier<ActivityState> newStateSupplier) {
        long newLastRecordedTime = System.currentTimeMillis();
        SettableFuture<Pair<IntegrationActivityKey, Long>> reportCompletedFuture = SettableFuture.create();
        states.compute(activityKey, (key, activityStateWrapper) -> {
            if (activityStateWrapper == null) {
                activityStateWrapper = new ActivityStateWrapper();
                activityStateWrapper.setState(newStateSupplier.get());
            }
            var activityState = activityStateWrapper.getState();
            if (activityState.getLastRecordedTime() < newLastRecordedTime) {
                activityState.setLastRecordedTime(newLastRecordedTime);
            }
            if (activityStateWrapper.isAlreadyBeenReported()) {
                return activityStateWrapper;
            }
            if (activityState.getLastReportedTime() < activityState.getLastRecordedTime()) {
                reporter.report(key, activityState.getLastRecordedTime(), activityState, new ActivityReportCallback<>() {
                    @Override
                    public void onSuccess(IntegrationActivityKey key, long reportedTime) {
                        reportCompletedFuture.set(Pair.of(key, reportedTime));
                    }

                    @Override
                    public void onFailure(IntegrationActivityKey key, Throwable t) {
                        reportCompletedFuture.setException(t);
                    }
                });
            }
            activityStateWrapper.setAlreadyBeenReported(true);
            return activityStateWrapper;
        });
        Futures.addCallback(reportCompletedFuture, new FutureCallback<>() {
            @Override
            public void onSuccess(Pair<IntegrationActivityKey, Long> reportResult) {
                updateLastReportedTime(reportResult.getFirst(), reportResult.getSecond());
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                log.debug("[{}] Failed to report first activity event in a period for device with id: [{}].", activityKey.getTenantId().getId(), activityKey.getDeviceId().getId());
            }
        }, MoreExecutors.directExecutor());
    }

    @Override
    protected void onReportingPeriodEnd() {
        for (Map.Entry<IntegrationActivityKey, ActivityStateWrapper> entry : states.entrySet()) {
            var activityKey = entry.getKey();
            var activityStateWrapper = entry.getValue();
            var activityState = activityStateWrapper.getState();
            long lastRecordedTime = activityState.getLastRecordedTime();
            // if there were no activities during the reporting period, we should remove the entry to prevent memory leaks
            if (!activityStateWrapper.isAlreadyBeenReported()) {
                states.remove(activityKey);
                // report leftover events
                if (activityState.getLastReportedTime() < lastRecordedTime) {
                    reporter.report(activityKey, lastRecordedTime, activityState, new ActivityReportCallback<>() {
                        @Override
                        public void onSuccess(IntegrationActivityKey key, long reportedTime) {
                            updateLastReportedTime(key, reportedTime); // just in case the same key was added again
                        }

                        @Override
                        public void onFailure(IntegrationActivityKey key, Throwable t) {
                            log.debug("[{}] Failed to report last activity event in a period for device with id: [{}].", activityKey.getTenantId().getId(), activityKey.getDeviceId().getId());
                        }
                    });
                }
            }
            activityStateWrapper.setAlreadyBeenReported(false);
        }
    }

    private void updateLastReportedTime(IntegrationActivityKey key, long newLastReportedTime) {
        states.computeIfPresent(key, (__, activityStateWrapper) -> {
            var activityState = activityStateWrapper.getState();
            activityState.setLastReportedTime(Math.max(activityState.getLastReportedTime(), newLastReportedTime));
            return activityStateWrapper;
        });
    }

}
