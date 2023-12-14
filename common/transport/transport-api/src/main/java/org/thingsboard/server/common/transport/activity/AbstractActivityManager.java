/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 * <p>
 * Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
 * <p>
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * <p>
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 * <p>
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 * <p>
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
package org.thingsboard.server.common.transport.activity;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.transport.activity.strategy.ActivityStrategy;

import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

@Slf4j
public abstract class AbstractActivityManager<Key, Metadata> implements ActivityManager<Key, ActivityState<Metadata>> {

    private final ConcurrentMap<Key, ActivityState<Metadata>> states = new ConcurrentHashMap<>();
    private String name;
    private long reportingPeriodMillis;
    private ActivityStateReporter<Key, ActivityState<Metadata>> reporter;
    private ScheduledExecutorService scheduler;
    private boolean initialized;

    @Override
    public synchronized void init(String name, long reportingPeriodMillis, ActivityStateReporter<Key, ActivityState<Metadata>> reporter) {
        if (!initialized) {
            this.name = StringUtils.notBlankOrDefault(name, "activity-manager");
            log.info("[{}] initializing.", this.name);
            this.reporter = Objects.requireNonNull(reporter, "Failed to initialize activity manager: provided activity reporter is null.");
            if (reportingPeriodMillis <= 0) {
                reportingPeriodMillis = 3000;
                log.error("[{}] Negative or zero reporting period millisecond was provided. Going to use reporting period value of 3 seconds.", this.name);
            }
            this.reportingPeriodMillis = reportingPeriodMillis;
            scheduler = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName(this.name));
            scheduler.scheduleAtFixedRate(this::onReportingPeriodEnd, new Random().nextInt((int) reportingPeriodMillis), reportingPeriodMillis, TimeUnit.MILLISECONDS);
            initialized = true;
            log.info("[{}] initialized.", this.name);
        }
    }

    @Override
    public void onActivity(Key key, Supplier<ActivityState<Metadata>> newStateSupplier) {
        if (!initialized) {
            log.error("[{}] Failed to process activity event: activity manager is not initialized.", name);
            return;
        }
        if (key == null) {
            log.error("[{}] Failed to process activity event: provided activity key is null.", name);
            return;
        }
        if (newStateSupplier == null) {
            log.error("[{}] Failed to process activity event: provided new activity state supplier is null.", name);
            return;
        }
        log.debug("[{}] Received activity event for key: [{}]", name, key);
        doOnActivity(key, newStateSupplier);
    }

    private boolean validate(Key key, Supplier<ActivityState<Metadata>> newStateSupplier) {

    }

    protected abstract ActivityStrategy getStrategy();

    private void doOnActivity(Key key, Supplier<ActivityState<Metadata>> newStateSupplier) {
        long newLastRecordedTime = System.currentTimeMillis();

        var shouldReport = new AtomicBoolean(false);
        var activityState = states.compute(key, (__, state) -> {
            if (state == null) {
                state = newStateSupplier.get();
                state.setStrategy(getStrategy());
            }
            if (state.getLastRecordedTime() < newLastRecordedTime) {
                state.setLastRecordedTime(newLastRecordedTime);
            }
            shouldReport.set(state.getStrategy().onActivity(state));
            return state;
        });

        if (shouldReport.get()) {
            log.debug("[{}] Going to report first activity event for key: [{}].", name, key);
            reporter.report(key, activityState.getLastRecordedTime(), activityState, new ActivityReportCallback<>() {
                @Override
                public void onSuccess(Key key, long reportedTime) {
                    updateLastReportedTime(key, reportedTime);
                }

                @Override
                public void onFailure(Key key, Throwable t) {
                    log.debug("[{}] Failed to report first activity event for key: [{}].", name, key, t);
                }
            });
        }
    }

    private void onReportingPeriodEnd() {
        log.debug("[{}] Going to end reporting period.", name);
        for (Map.Entry<Key, ActivityState<Metadata>> entry : states.entrySet()) {
            var key = entry.getKey();
            var state = entry.getValue();
            long lastRecordedTime = state.getLastRecordedTime();

            boolean hasExpired = false; // TODO: implement state expiration
            boolean shouldReport;
            if (hasExpired) {
                states.remove(key);
                shouldReport = true;
            } else {
                shouldReport = state.getStrategy().onReportingPeriodEnd(state);
            }

            if (shouldReport) {
                log.debug("[{}] Going to report last activity event for key: [{}].", name, key);
                reporter.report(key, lastRecordedTime, state, new ActivityReportCallback<>() {
                    @Override
                    public void onSuccess(Key key, long newLastReportedTime) {
                        updateLastReportedTime(key, newLastReportedTime);
                    }

                    @Override
                    public void onFailure(Key key, Throwable t) {
                        log.debug("[{}] Failed to report last activity event in a period for key: [{}].", name, key, t);
                    }
                });
            }
        }
    }

    private void updateLastReportedTime(Key key, long newLastReportedTime) {
        states.computeIfPresent(key, (__, activityState) -> {
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
