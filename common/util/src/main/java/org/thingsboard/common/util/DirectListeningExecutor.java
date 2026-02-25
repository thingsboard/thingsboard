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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Callable;

public enum DirectListeningExecutor implements ListeningExecutor {

    INSTANCE;

    @Override
    public <T> ListenableFuture<T> executeAsync(Callable<T> task) {
        try {
            return Futures.immediateFuture(task.call());
        } catch (Exception e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    @Override
    public void execute(Runnable command) {
        command.run();
    }

}
