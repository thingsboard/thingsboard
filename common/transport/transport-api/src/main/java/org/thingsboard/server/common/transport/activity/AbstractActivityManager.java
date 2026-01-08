/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.common.transport.activity;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.transport.activity.strategy.ActivityStrategy;
import org.thingsboard.server.queue.scheduler.SchedulerComponent;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public abstract class AbstractActivityManager<Key, Metadata> implements ActivityManager<Key, Metadata> {

    private final ConcurrentMap<Key, ActivityStateWrapper> states = new ConcurrentHashMap<>();

    @Autowired
    protected SchedulerComponent scheduler;

    @Data
    private class ActivityStateWrapper {

        private volatile ActivityState<Metadata> state;
        private volatile long lastReportedTime;
        private volatile ActivityStrategy strategy;

    }

    protected void init() {
        var reportingPeriodMillis = getReportingPeriodMillis();
        scheduler.scheduleAtFixedRate(this::onReportingPeriodEnd, new Random().nextInt((int) reportingPeriodMillis), reportingPeriodMillis, TimeUnit.MILLISECONDS);
    }

    protected abstract long getReportingPeriodMillis();

    protected abstract ActivityStrategy getStrategy();

    protected abstract ActivityState<Metadata> updateState(Key key, ActivityState<Metadata> state);

    protected abstract boolean hasExpired(long lastRecordedTime);

    protected abstract void onStateExpiry(Key key, Metadata metadata);

    protected abstract void reportActivity(Key key, Metadata metadata, long timeToReport, ActivityReportCallback<Key> callback);

    @Override
    public void onActivity(Key key, Metadata metadata, long newLastRecordedTime) {
        if (key == null) {
            log.error("Failed to process activity event: provided activity key is null.");
            return;
        }
        log.debug("Received activity event for key: [{}]. Event time: [{}].", key, newLastRecordedTime);

        var shouldReport = new AtomicBoolean(false);
        var lastRecordedTime = new AtomicLong();
        var lastReportedTime = new AtomicLong();

        states.compute(key, (__, stateWrapper) -> {
            if (stateWrapper == null) {
                ActivityState<Metadata> newState = new ActivityState<>();
                stateWrapper = new ActivityStateWrapper();
                stateWrapper.setState(newState);
                stateWrapper.setStrategy(getStrategy());
            }
            var state = stateWrapper.getState();
            state.setMetadata(metadata);
            if (state.getLastRecordedTime() < newLastRecordedTime) {
                state.setLastRecordedTime(newLastRecordedTime);
            }
            shouldReport.set(stateWrapper.getStrategy().onActivity());
            lastRecordedTime.set(state.getLastRecordedTime());
            lastReportedTime.set(stateWrapper.getLastReportedTime());
            return stateWrapper;
        });

        if (shouldReport.get() && lastReportedTime.get() < lastRecordedTime.get()) {
            log.debug("Going to report first activity event for key: [{}]. Event time: [{}].", key, lastRecordedTime.get());
            reportActivity(key, metadata, lastRecordedTime.get(), new ActivityReportCallback<>() {
                @Override
                public void onSuccess(Key key, long reportedTime) {
                    updateLastReportedTime(key, reportedTime);
                }

                @Override
                public void onFailure(Key key, Throwable t) {
                    log.debug("Failed to report first activity event for key: [{}]. Event time: [{}].", key, lastRecordedTime.get(), t);
                }
            });
        }
    }

    @Override
    public void onReportingPeriodEnd() {
        log.debug("Going to end reporting period.");
        for (Map.Entry<Key, ActivityStateWrapper> entry : states.entrySet()) {
            Key key = entry.getKey();
            ActivityStateWrapper stateWrapper = entry.getValue();
            try {
                reportLastEvent(key, stateWrapper);
            } catch (Exception e) {
                log.error("Failed to report last activity event on reporting period end for key: [{}]. State: [{}].", key, stateWrapper, e);
            }
        }
    }

    private void reportLastEvent(Key key, ActivityStateWrapper stateWrapper) {
        var currentState = stateWrapper.getState();

        long lastRecordedTime = currentState.getLastRecordedTime();
        long lastReportedTime = stateWrapper.getLastReportedTime();
        var metadata = currentState.getMetadata();

        boolean hasExpired;
        boolean shouldReport;

        var updatedState = updateState(key, currentState);
        if (updatedState != null) {
            stateWrapper.setState(updatedState);
            lastRecordedTime = updatedState.getLastRecordedTime();
            metadata = updatedState.getMetadata();
            hasExpired = hasExpired(lastRecordedTime);
            shouldReport = stateWrapper.getStrategy().onReportingPeriodEnd();
        } else {
            states.remove(key);
            hasExpired = false;
            shouldReport = true;
        }

        if (hasExpired) {
            states.remove(key);
            onStateExpiry(key, metadata);
            shouldReport = true;
        }

        if (shouldReport && lastReportedTime < lastRecordedTime) {
            long timeToReport = lastRecordedTime;
            log.debug("Going to report last activity event for key: [{}]. Event time: [{}].", key, timeToReport);
            reportActivity(key, metadata, timeToReport, new ActivityReportCallback<>() {
                @Override
                public void onSuccess(Key key, long reportedTime) {
                    updateLastReportedTime(key, reportedTime);
                }

                @Override
                public void onFailure(Key key, Throwable t) {
                    log.debug("Failed to report last activity event for key: [{}]. Event time: [{}].", key, timeToReport, t);
                }
            });
        }
    }

    @Override
    public long getLastRecordedTime(Key key) {
        ActivityStateWrapper stateWrapper = states.get(key);
        return stateWrapper == null ? 0L : stateWrapper.getState().getLastRecordedTime();
    }

    private void updateLastReportedTime(Key key, long newLastReportedTime) {
        states.computeIfPresent(key, (__, stateWrapper) -> {
            stateWrapper.setLastReportedTime(Math.max(stateWrapper.getLastReportedTime(), newLastReportedTime));
            return stateWrapper;
        });
    }

}
