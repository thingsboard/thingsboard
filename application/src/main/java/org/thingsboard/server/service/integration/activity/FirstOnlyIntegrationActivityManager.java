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
import org.springframework.data.util.Pair;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.transport.activity.ActivityManager;
import org.thingsboard.server.common.transport.activity.ActivityState;
import org.thingsboard.server.common.transport.activity.ActivityStateReportCallback;
import org.thingsboard.server.common.transport.activity.ActivityStateReporter;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
public class FirstOnlyIntegrationActivityManager implements ActivityManager<IntegrationActivityKey, ActivityState> {

    private final ConcurrentMap<IntegrationActivityKey, ActivityStateWrapper> activityStates = new ConcurrentHashMap<>();

    @Data
    private static class ActivityStateWrapper {

        volatile ActivityState activityState;
        volatile boolean alreadyBeenReported;

    }

    private ScheduledExecutorService scheduler;
    private final ActivityStateReporter<IntegrationActivityKey, ActivityState> reporter;
    private final long reportingPeriodDurationMillis;
    private final String name;
    private boolean initialized;

    public FirstOnlyIntegrationActivityManager(ActivityStateReporter<IntegrationActivityKey, ActivityState> reporter, long reportingPeriodDurationMillis, String name) {
        this.reporter = Objects.requireNonNull(reporter, "Failed to create activity manager: provided reporter is null.");
        this.reportingPeriodDurationMillis = reportingPeriodDurationMillis; // TODO: add min/max duration validation
        this.name = name == null ? "activity-state-manager" : name;
    }

    @Override
    public synchronized void init() {
        if (!initialized) {
            scheduler = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName(name));
            scheduler.scheduleAtFixedRate(this::onReportingPeriodEnd, new Random().nextInt((int) reportingPeriodDurationMillis), reportingPeriodDurationMillis, TimeUnit.MILLISECONDS);
            initialized = true;
        }
    }

    @Override
    public void onActivity(IntegrationActivityKey activityKey, Supplier<ActivityState> newStateSupplier) {
        long newLastRecordedTime = System.currentTimeMillis();
        SettableFuture<Pair<IntegrationActivityKey, Long>> reportCompletedFuture = SettableFuture.create();
        activityStates.compute(activityKey, (key, activityStateWrapper) -> {
            if (activityStateWrapper == null) {
                ActivityState activityState = newStateSupplier.get();
                activityState.setLastRecordedTime(newLastRecordedTime);
                activityState.setLastReportedTime(0L);
                activityStateWrapper = new ActivityStateWrapper();
                activityStateWrapper.setActivityState(activityState);
                activityStateWrapper.setAlreadyBeenReported(false);
            } else {
                activityStateWrapper.getActivityState().setLastRecordedTime(newLastRecordedTime);
            }
            if (activityStateWrapper.isAlreadyBeenReported()) {
                return activityStateWrapper;
            }
            var activityState = activityStateWrapper.getActivityState();
            if (activityState.getLastReportedTime() < activityState.getLastRecordedTime()) {
                reporter.report(key, activityState, new ActivityStateReportCallback<>() {
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
                // TODO: log
            }
        }, MoreExecutors.directExecutor());
    }

    private void updateLastReportedTime(IntegrationActivityKey key, long newLastReportedTime) {
        activityStates.computeIfPresent(key, (__, activityStateWrapper) -> {
            var activityState = activityStateWrapper.getActivityState();
            activityState.setLastReportedTime(Math.max(activityState.getLastReportedTime(), newLastReportedTime));
            return activityStateWrapper;
        });
    }

    private void onReportingPeriodEnd() {
        Map<IntegrationActivityKey, ActivityState> statesToRemoveAndReport = new HashMap<>();
        Set<Map.Entry<IntegrationActivityKey, ActivityStateWrapper>> entries = activityStates.entrySet();
        for (Map.Entry<IntegrationActivityKey, ActivityStateWrapper> entry : entries) {
            var activityKey = entry.getKey();
            var activityStateWrapper = entry.getValue();
            var activityState = activityStateWrapper.getActivityState();
            if (!activityStateWrapper.isAlreadyBeenReported() && activityState.getLastReportedTime() < activityState.getLastRecordedTime()) {
                statesToRemoveAndReport.put(activityKey, activityState);
            }
            activityStateWrapper.setAlreadyBeenReported(false);
        }
        statesToRemoveAndReport.forEach((key, state) -> reporter.report(key, state, new ActivityStateReportCallback<>() {
            @Override
            public void onSuccess(IntegrationActivityKey key, long reportedTime) {
                activityStates.remove(key);
            }

            @Override
            public void onFailure(IntegrationActivityKey key, Throwable t) {
                // TODO: log
            }
        }));
    }

    @Override
    public synchronized void destroy() {
        if (initialized) {
            initialized = false;
            if (scheduler != null) {
                scheduler.shutdown();
                try {
                    if (scheduler.awaitTermination(10L, TimeUnit.SECONDS)) {
                        scheduler.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    scheduler.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

}
