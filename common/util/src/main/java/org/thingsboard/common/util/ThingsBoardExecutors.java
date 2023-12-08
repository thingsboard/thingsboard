/**
 * Copyright © 2016-2023 The Thingsboard Authors
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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;

public class ThingsBoardExecutors {

    /**
     * Method forked from ExecutorService to provide thread poll name
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

}
