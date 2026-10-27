// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.common.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThingsBoardExecutors {

    /** Cannot instantiate. */
    private ThingsBoardExecutors(){}

    /**
     * Method forked from ExecutorService to provide thread pool name
     *
     * Creates a thread pool that maintains enough threads to support
     * the given parallelism level, and may use multiple queues to
     * reduce contention. The parallelism level corresponds to the
     * maximum number of threads actively engaged in, or available to
     * engage in, task processing. The actual number of threads may
     * grow and shrink dynamically. A work-stealing pool makes no
     * guarantees about the order in which submitted tasks are
     * executed.
     *
     * @param parallelism the targeted parallelism level
     * @param namePrefix used to define thread name
     * @return the newly created thread pool
     * @throws IllegalArgumentException if {@code parallelism <= 0}
     * @since 1.8
     */
    public static ExecutorService newWorkStealingPool(int parallelism, String namePrefix) {
        return new ForkJoinPool(parallelism,
                new ThingsBoardForkJoinWorkerThreadFactory(namePrefix),
                null, true);
    }

    public static ExecutorService newWorkStealingPool(int parallelism, Class clazz) {
        return newWorkStealingPool(parallelism, clazz.getSimpleName());
    }

    /*
     * executor with limited tasks queue size
     * */
    public static ExecutorService newLimitedTasksExecutor(int threads, int maxQueueSize, String name) {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(threads, threads,
                60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(maxQueueSize),
                ThingsBoardThreadFactory.forName(name),
                new ThreadPoolExecutor.CallerRunsPolicy());
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }

    public static ScheduledExecutorService newSingleThreadScheduledExecutor(String name) {
        return Executors.unconfigurableScheduledExecutorService(new ThingsBoardScheduledThreadPoolExecutor(1, ThingsBoardThreadFactory.forName(name)));
    }

    public static ScheduledExecutorService newScheduledThreadPool(int corePoolSize, String name) {
        return new ThingsBoardScheduledThreadPoolExecutor(corePoolSize, ThingsBoardThreadFactory.forName(name));
    }

}
