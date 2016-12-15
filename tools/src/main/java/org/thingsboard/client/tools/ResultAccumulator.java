package org.thingsboard.client.tools; /**
 * Copyright © 2016 The Thingsboard Authors
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
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Andrew Shvayka
 */
@Slf4j
public class ResultAccumulator {

    private AtomicLong minTime = new AtomicLong(Long.MAX_VALUE);
    private AtomicLong maxTime = new AtomicLong(Long.MIN_VALUE);
    private AtomicLong timeSpentCount = new AtomicLong();
    private AtomicInteger successCount = new AtomicInteger();
    private AtomicInteger errorCount = new AtomicInteger();

    public void onResult(boolean success, long timeSpent) {
        if (success) {
            successCount.incrementAndGet();
        } else {
            errorCount.incrementAndGet();
        }
        timeSpentCount.addAndGet(timeSpent);

        while (!setMax(timeSpent)) ;
        while (!setMin(timeSpent)) ;
    }

    private boolean setMax(long timeSpent) {
        long curMax = maxTime.get();
        long newMax = Math.max(curMax, timeSpent);
        return maxTime.compareAndSet(curMax, newMax);
    }

    private boolean setMin(long timeSpent) {
        long curMin = minTime.get();
        long newMin = Math.min(curMin, timeSpent);
        return minTime.compareAndSet(curMin, newMin);
    }


    public int getSuccessCount() {
        return successCount.get();
    }

    public int getErrorCount() {
        return errorCount.get();
    }

    public long getTimeSpent() {
        return timeSpentCount.get();
    }

    public double getAvgTimeSpent() {
        return ((double) getTimeSpent()) / (getSuccessCount() + getErrorCount());
    }

    @Override
    public String toString() {
        return "Result {" +
                "successCount=" + getSuccessCount() +
                ", errorCount=" + getErrorCount() +
                ", totalTime=" + getTimeSpent() +
                ", avgTime=" + getAvgTimeSpent() +
                ", minTime=" + minTime.get() +
                ", maxTime=" + maxTime.get() +
                '}';
    }
}
