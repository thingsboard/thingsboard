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

import org.thingsboard.common.util.ThingsBoardThreadFactory;

import java.util.Objects;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public abstract class AbstractActivityManager<Key, State extends ActivityState> implements ActivityManager<Key, State> {

    private String name;
    protected long reportingPeriodMillis;
    protected ActivityStateReporter<Key, State> reporter;
    private ScheduledExecutorService scheduler;
    protected boolean initialized;

    @Override
    public synchronized void init() {
        Objects.requireNonNull(reporter, "Failed to initialize activity manager: provided activity reporter is null.");
        if (reportingPeriodMillis < 1000) {
            throw new IllegalArgumentException("Failed to initialize activity manager: provided reporting period duration is less that 1 second.");
        }
        if (!initialized) {
            scheduler = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName(name == null ? "activity-manager" : name));
            scheduler.scheduleAtFixedRate(this::onReportingPeriodEnd, new Random().nextInt((int) reportingPeriodMillis), reportingPeriodMillis, TimeUnit.MILLISECONDS);
            initialized = true;
        }
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void setReportingPeriod(long reportingPeriodMillis) {
        this.reportingPeriodMillis = reportingPeriodMillis;
    }

    @Override
    public void setActivityReporter(ActivityStateReporter<Key, State> activityReporter) {
        reporter = activityReporter;
    }

    @Override
    public void onActivity(Key key, Supplier<State> newStateSupplier) {
        if (!initialized) {
            throw new IllegalStateException(name + " is not initialized.");
        }
        if (key == null) {
            throw new IllegalArgumentException("Activity key can't be null.");
        }
        if (newStateSupplier == null) {
            throw new IllegalArgumentException("New activity state supplier can't be null.");
        }
        doOnActivity(key, newStateSupplier);
    }

    protected abstract void doOnActivity(Key key, Supplier<State> newStateSupplier);

    protected abstract void onReportingPeriodEnd();

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
