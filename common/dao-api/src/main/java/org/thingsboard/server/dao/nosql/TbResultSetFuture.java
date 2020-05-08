/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.dao.nosql;

import com.datastax.oss.driver.api.core.cql.AsyncResultSet;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by ashvayka on 24.10.18.
 */
public class TbResultSetFuture implements ListenableFuture<AsyncResultSet> {

    private final SettableFuture<AsyncResultSet> mainFuture;

    public TbResultSetFuture(SettableFuture<AsyncResultSet> mainFuture) {
        this.mainFuture = mainFuture;
    }

    public AsyncResultSet getUninterruptibly() {
        return getSafe();
    }

    public AsyncResultSet getUninterruptibly(long timeout, TimeUnit unit) throws TimeoutException {
        return getSafe(timeout, unit);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return mainFuture.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return mainFuture.isCancelled();
    }

    @Override
    public boolean isDone() {
        return mainFuture.isDone();
    }

    @Override
    public AsyncResultSet get() throws InterruptedException, ExecutionException {
        return mainFuture.get();
    }

    @Override
    public AsyncResultSet get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return mainFuture.get(timeout, unit);
    }

    @Override
    public void addListener(Runnable listener, Executor executor) {
        mainFuture.addListener(listener, executor);
    }

    private AsyncResultSet getSafe() {
        try {
            return mainFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }

    private AsyncResultSet getSafe(long timeout, TimeUnit unit) throws TimeoutException {
        try {
            return mainFuture.get(timeout, unit);
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }

}
