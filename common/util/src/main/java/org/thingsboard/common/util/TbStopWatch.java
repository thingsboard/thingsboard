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
package org.thingsboard.common.util;

import org.springframework.util.StopWatch;

/**
 * Utility method that extends Spring Framework StopWatch
 * It is a MONOTONIC time stopwatch.
 * It is a replacement for any measurements with a wall-clock like System.currentTimeMillis()
 * It is not affected by leap second, day-light saving and wall-clock adjustments by manual or network time synchronization
 * The main features is a single call for common use cases:
 *  - create and start: TbStopWatch sw = TbStopWatch.startNew()
 *  - stop and get: sw.stopAndGetTotalTimeMillis() or sw.stopAndGetLastTaskTimeMillis()
 * */
public class TbStopWatch extends StopWatch {

    public static TbStopWatch create(){
        TbStopWatch stopWatch = new TbStopWatch();
        stopWatch.start();
        return stopWatch;
    }

    public static TbStopWatch create(String taskName){
        TbStopWatch stopWatch = new TbStopWatch();
        stopWatch.start(taskName);
        return stopWatch;
    }

    public void startNew(String taskName){
        stop();
        start(taskName);
    }

    public long stopAndGetTotalTimeMillis(){
        stop();
        return getTotalTimeMillis();
    }

    public long stopAndGetTotalTimeNanos(){
        stop();
        return getLastTaskTimeNanos();
    }

    public long stopAndGetLastTaskTimeMillis(){
        stop();
        return getLastTaskTimeMillis();
    }

    public long stopAndGetLastTaskTimeNanos(){
        stop();
        return getLastTaskTimeNanos();
    }

}
