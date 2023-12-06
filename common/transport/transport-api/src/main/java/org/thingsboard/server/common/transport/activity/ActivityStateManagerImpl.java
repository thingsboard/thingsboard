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

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.data.util.Pair;
import org.thingsboard.common.util.ThingsBoardThreadFactory;

import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
public final class ActivityStateManagerImpl<Key, State extends ActivityState> implements ActivityStateManager<Key, State> {

    private final ConcurrentMap<Key, ActivityStateWrapper> activityStates = new ConcurrentHashMap<>();

    @Data
    private static class ActivityStateWrapper {

        volatile ActivityState activityState;
        volatile boolean alreadyBeenReported;

    }

    private ScheduledExecutorService scheduler;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final AtomicInteger currentPeriodId = new AtomicInteger();
    private final AsyncActivityStateReporter<Key, State> reporter;
    private final long reportingPeriodDurationMillis;
    private final String name;
    private boolean initialized;

    public ActivityStateManagerImpl(AsyncActivityStateReporter<Key, State> reporter, long reportingPeriodDurationMillis, String name) {
        this.reporter = Objects.requireNonNull(reporter, "Failed to initialize activity manager: provided reporter is null.");
        this.reportingPeriodDurationMillis = reportingPeriodDurationMillis; // TODO: add min/max duration validation
        this.name = name == null ? "activity-state-manager" : name;
    }

    @Override
    public synchronized void init() {
        if (!initialized) {
            initialized = true;
            scheduler = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName(name));
            scheduler.scheduleAtFixedRate(this::reportLastEventAndStartNewPeriod, new Random().nextInt((int) reportingPeriodDurationMillis), reportingPeriodDurationMillis, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void recordActivity(Key activityKey, Supplier<State> newActivityStateSupplier) {
        long newLastRecordedTime = System.currentTimeMillis();
        int capturedPeriodId = currentPeriodId.get();
        lock.readLock().lock();
        SettableFuture<Pair<Key, Long>> reportCompletedFuture = SettableFuture.create();
        try {
            activityStates.compute(activityKey, (key, activityStateWrapper) -> {
                if (activityStateWrapper == null) {
                    State activityState = newActivityStateSupplier.get();
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
                    reporter.reportAsync(key, (State) activityState, new ActivityStateReportCallback<>() {
                        @Override
                        public void onSuccess(Key key, long reportedTime) {
                            reportCompletedFuture.set(Pair.of(key, reportedTime));
                        }

                        @Override
                        public void onFailure(Key key, Throwable t) {
                            reportCompletedFuture.setException(t);
                        }

                        @Override
                        public void onRemove(Key key) {
                            lock.readLock().lock();
                            try {
                                activityStates.remove(key);
                            } finally {
                                lock.readLock().unlock();
                            }
                        }
                    });
                }
                activityStateWrapper.setAlreadyBeenReported(true);
                return activityStateWrapper;
            });
        } finally {
            lock.readLock().unlock();
        }
        Futures.addCallback(reportCompletedFuture, new FutureCallback<>() {
            @Override
            public void onSuccess(Pair<Key, Long> reportResult) {
                lock.readLock().lock();
                try {
                    updateLastReportedTime(reportResult.getFirst(), reportResult.getSecond());
                } finally {
                    lock.readLock().unlock();
                }
            }

            @Override
            public void onFailure(@NonNull Throwable t) { // TODO: add failure logging
                lock.readLock().lock();
                try {
                    rollbackReportedStatus(activityKey, capturedPeriodId, newLastRecordedTime);
                } finally {
                    lock.readLock().unlock();
                }
            }
        }, MoreExecutors.directExecutor());
    }

    private void rollbackReportedStatus(Key key, int capturedPeriodId, long newLastRecordedTime) {
        activityStates.computeIfPresent(key, (__, activityStateWrapper) -> {
            if (capturedPeriodId == currentPeriodId.get() && activityStateWrapper.getActivityState().getLastReportedTime() < newLastRecordedTime) {
                activityStateWrapper.setAlreadyBeenReported(false);
            }
            return activityStateWrapper;
        });
    }

    private void reportLastEventAndStartNewPeriod() {
        lock.writeLock().lock();
        try {
            Map<Key, State> activityStateMap = activityStates.entrySet().stream()
                    .peek(entry -> entry.getValue().setAlreadyBeenReported(false))
                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> (State) entry.getValue().getActivityState()));
            reporter.reportAsync(activityStateMap, new ActivityStateReportCallback<>() {
                @Override
                public void onSuccess(Key key, long reportedTime) {
                    lock.readLock().lock();
                    try {
                        updateLastReportedTime(key, reportedTime);
                    } finally {
                        lock.readLock().unlock();
                    }
                }

                @Override
                public void onFailure(Key key, Throwable t) {
                    log.debug("Failed to report activity state for key [{}]!", key, t);
                }

                @Override
                public void onRemove(Key key) {
                    lock.readLock().lock();
                    try {
                        activityStates.remove(key);
                    } finally {
                        lock.readLock().unlock();
                    }
                }
            });
        } finally {
            currentPeriodId.incrementAndGet();
            lock.writeLock().unlock();
        }
    }

    private void updateLastReportedTime(Key key, long newLastReportedTime) {
        activityStates.computeIfPresent(key, (__, activityStateWrapper) -> {
            var activityState = activityStateWrapper.getActivityState();
            activityState.setLastReportedTime(Math.max(activityState.getLastReportedTime(), newLastReportedTime));
            return activityStateWrapper;
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
