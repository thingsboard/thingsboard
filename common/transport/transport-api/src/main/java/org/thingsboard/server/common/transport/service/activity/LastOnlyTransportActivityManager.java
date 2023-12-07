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
package org.thingsboard.server.common.transport.service.activity;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.transport.activity.ActivityManager;
import org.thingsboard.server.common.transport.activity.ActivityStateReportCallback;
import org.thingsboard.server.common.transport.activity.ActivityStateReporter;
import org.thingsboard.server.common.transport.service.TransportActivityState;

import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
public class LastOnlyTransportActivityManager implements ActivityManager<UUID, TransportActivityState> {

    private final ConcurrentMap<UUID, TransportActivityState> activityStates = new ConcurrentHashMap<>();

    private ScheduledExecutorService scheduler;
    private final ActivityStateReporter<UUID, TransportActivityState> reporter;
    private final long reportingPeriodDurationMillis;
    private final String name;
    private boolean initialized;

    public LastOnlyTransportActivityManager(ActivityStateReporter<UUID, TransportActivityState> reporter, long reportingPeriodDurationMillis, String name) {
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
    public void onActivity(UUID activityKey, Supplier<TransportActivityState> newStateSupplier) {
        long newLastRecordedTime = System.currentTimeMillis();
        activityStates.compute(activityKey, (key, activityState) -> {
            if (activityState == null) {
                log.info("Creating new activity state.");
                activityState = newStateSupplier.get();
                activityState.setLastRecordedTime(newLastRecordedTime);
                activityState.setLastReportedTime(0L);
            } else {
                activityState.setLastRecordedTime(newLastRecordedTime);
            }
            return activityState;
        });
    }

    private void onReportingPeriodEnd() {
        reporter.report(activityStates, new ActivityStateReportCallback<>() {
            @Override
            public void onSuccess(UUID key, long reportedTime) {
                updateLastReportedTime(key, reportedTime);
            }

            @Override
            public void onFailure(UUID uuid, Throwable t) {
                // TODO: log
            }
        });
    }

    private void updateLastReportedTime(UUID key, long newLastReportedTime) {
        activityStates.computeIfPresent(key, (__, activityState) -> {
            activityState.setLastReportedTime(Math.max(activityState.getLastReportedTime(), newLastReportedTime));
            return activityState;
        });
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
