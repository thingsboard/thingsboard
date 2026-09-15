// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
        return lastTaskInfo().getTimeNanos();
    }

    public long stopAndGetLastTaskTimeMillis(){
        stop();
        return lastTaskInfo().getTimeMillis();
    }

    public long stopAndGetLastTaskTimeNanos(){
        stop();
        return lastTaskInfo().getTimeNanos();
    }

}
