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
package org.thingsboard.server.common.transport.activity;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.transport.activity.strategy.ActivityStrategy;
import org.thingsboard.server.queue.scheduler.SchedulerComponent;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public abstract class AbstractActivityManager<Key, Metadata> implements ActivityManager<Key> {

    private final ConcurrentMap<Key, ActivityState<Metadata>> states = new ConcurrentHashMap<>();

    @Autowired
    protected SchedulerComponent scheduler;

    protected String name;
    private boolean initialized;

    @Override
    public synchronized void init(String name, long reportingPeriodMillis) {
        if (!initialized) {
            this.name = StringUtils.notBlankOrDefault(name, "activity-manager");
            log.info("Activity manager with name [{}] is initializing.", this.name);
            if (reportingPeriodMillis <= 0) {
                reportingPeriodMillis = 3000;
                log.error("[{}] Negative or zero reporting period millisecond was provided. Going to use reporting period value of 3 seconds.", this.name);
            }
            scheduler.scheduleAtFixedRate(this::onReportingPeriodEnd, new Random().nextInt((int) reportingPeriodMillis), reportingPeriodMillis, TimeUnit.MILLISECONDS);
            initialized = true;
            log.info("Activity manager with name [{}] is initialized.", this.name);
        }
    }

    protected abstract ActivityState<Metadata> createNewState(Key key);

    protected abstract ActivityStrategy getStrategy();

    protected abstract ActivityState<Metadata> updateState(Key key, ActivityState<Metadata> state);

    protected abstract boolean hasExpired(Key key, ActivityState<Metadata> state);

    protected abstract void onStateExpire(Key key, Metadata metadata);

    protected abstract void reportActivity(Key key, Metadata metadata, long timeToReport, ActivityReportCallback<Key> callback);

    @Override
    public void onActivity(Key key) {
        if (!initialized) {
            log.error("[{}] Failed to process activity event: activity manager is not initialized.", name);
            return;
        }
        if (key == null) {
            log.error("[{}] Failed to process activity event: provided activity key is null.", name);
            return;
        }
        log.debug("[{}] Received activity event for key: [{}]", name, key);

        long newLastRecordedTime = System.currentTimeMillis();
        var shouldReport = new AtomicBoolean(false);
        var activityState = states.compute(key, (__, state) -> {
            if (state == null) {
                var newState = createNewState(key);
                if (newState == null) {
                    return null;
                }
                state = newState;
                state.setStrategy(getStrategy());
            }
            if (state.getLastRecordedTime() < newLastRecordedTime) {
                state.setLastRecordedTime(newLastRecordedTime);
            }
            shouldReport.set(state.getStrategy().onActivity());
            return state;
        });

        if (activityState == null) {
            return;
        }

        long lastRecordedTime = activityState.getLastRecordedTime();
        long lastReportedTime = activityState.getLastReportedTime();
        if (shouldReport.get() && lastReportedTime < lastRecordedTime) {
            log.debug("[{}] Going to report first activity event for key: [{}].", name, key);
            reportActivity(key, activityState.getMetadata(), lastRecordedTime, new ActivityReportCallback<>() {
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

    protected long getLastRecordedTime(Key key) {
        ActivityState<Metadata> state = states.get(key);
        return state == null ? 0L : state.getLastRecordedTime();
    }

    private void onReportingPeriodEnd() {
        log.debug("[{}] Going to end reporting period.", name);
        for (Map.Entry<Key, ActivityState<Metadata>> entry : states.entrySet()) {
            var key = entry.getKey();
            var currentState = entry.getValue();

            long lastRecordedTime = currentState.getLastRecordedTime();
            long lastReportedTime = currentState.getLastReportedTime();
            var metadata = currentState.getMetadata();

            boolean hasExpired;
            boolean shouldReport;

            var updatedState = updateState(key, currentState);
            if (updatedState != null) {
                lastRecordedTime = updatedState.getLastRecordedTime();
                lastReportedTime = updatedState.getLastReportedTime();
                metadata = updatedState.getMetadata();
                hasExpired = hasExpired(key, updatedState);
                shouldReport = updatedState.getStrategy().onReportingPeriodEnd();
            } else {
                states.remove(key);
                hasExpired = false;
                shouldReport = true;
            }

            if (hasExpired) {
                states.remove(key);
                onStateExpire(key, metadata);
            }

            if (shouldReport && lastReportedTime < lastRecordedTime) {
                log.debug("[{}] Going to report last activity event for key: [{}].", name, key);
                reportActivity(key, metadata, lastRecordedTime, new ActivityReportCallback<>() {
                    @Override
                    public void onSuccess(Key key, long reportedTime) {
                        updateLastReportedTime(key, reportedTime);
                    }

                    @Override
                    public void onFailure(Key key, Throwable t) {
                        log.debug("[{}] Failed to report last activity event for key: [{}].", name, key, t);
                    }
                });
            }
        }
    }

    private void updateLastReportedTime(Key key, long newLastReportedTime) {
        states.computeIfPresent(key, (__, state) -> {
            state.setLastReportedTime(Math.max(state.getLastReportedTime(), newLastReportedTime));
            return state;
        });
    }

}
