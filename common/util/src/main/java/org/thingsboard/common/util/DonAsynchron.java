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

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public class DonAsynchron {

    public static <T> void withCallback(ListenableFuture<T> future, Consumer<T> onSuccess,
                                        Consumer<Throwable> onFailure) {
        withCallback(future, onSuccess, onFailure, null);
    }

    public static <T> void withCallback(ListenableFuture<T> future, Consumer<T> onSuccess,
                                        Consumer<Throwable> onFailure, Executor executor) {
        FutureCallback<T> callback = new FutureCallback<T>() {
            @Override
            public void onSuccess(T result) {
                if (onSuccess == null) {
                    return;
                }
                try {
                    onSuccess.accept(result);
                } catch (Throwable th) {
                    onFailure(th);
                }
            }

            @Override
            public void onFailure(Throwable t) {
                if (onFailure == null) {
                    return;
                }
                onFailure.accept(t);
            }
        };
        if (executor != null) {
            Futures.addCallback(future, callback, executor);
        } else {
            Futures.addCallback(future, callback, MoreExecutors.directExecutor());
        }
    }

    public static <T> ListenableFuture<T> submit(Callable<T> task, Consumer<T> onSuccess, Consumer<Throwable> onFailure, Executor executor) {
        return submit(task, onSuccess, onFailure, executor, null);
    }

    public static <T> ListenableFuture<T> submit(Callable<T> task, Consumer<T> onSuccess, Consumer<Throwable> onFailure, Executor executor, Executor callbackExecutor) {
        ListenableFuture<T> future = Futures.submit(task, executor);
        withCallback(future, onSuccess, onFailure, callbackExecutor);
        return future;
    }

    public static <T> FluentFuture<T> toFluentFuture(CompletableFuture<T> completable) {
        SettableFuture<T> future = SettableFuture.create();
        completable.whenComplete((result, exception) -> {
            if (exception != null) {
                future.setException(exception);
            } else {
                future.set(result);
            }
        });
        return FluentFuture.from(future);
    }

}
