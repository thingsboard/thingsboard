/**
 * Copyright © 2016-2025 The Thingsboard Authors
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

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

public interface ListeningExecutor extends Executor {

    <T> ListenableFuture<T> executeAsync(Callable<T> task);

    default ListenableFuture<?> executeAsync(Runnable task) {
        return executeAsync(() -> {
            task.run();
            return null;
        });
    }

    default <T> ListenableFuture<T> submit(Callable<T> task) {
        return executeAsync(task);
    }

    default ListenableFuture<?> submit(Runnable task) {
        return executeAsync(task);
    }

}
