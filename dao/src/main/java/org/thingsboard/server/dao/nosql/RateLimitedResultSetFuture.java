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

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Uninterruptibles;
import org.thingsboard.server.dao.exception.BufferLimitException;
import org.thingsboard.server.dao.util.AsyncRateLimiter;

import javax.annotation.Nullable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class RateLimitedResultSetFuture implements ResultSetFuture {

    private final ListenableFuture<ResultSetFuture> originalFuture;
    private final ListenableFuture<Void> rateLimitFuture;

    public RateLimitedResultSetFuture(Session session, AsyncRateLimiter rateLimiter, Statement statement) {
        this.rateLimitFuture = Futures.catchingAsync(rateLimiter.acquireAsync(), Throwable.class, t -> {
            if (!(t instanceof BufferLimitException)) {
                rateLimiter.release();
            }
            return Futures.immediateFailedFuture(t);
        }, MoreExecutors.directExecutor());
        this.originalFuture = Futures.transform(rateLimitFuture,
                i -> executeAsyncWithRelease(rateLimiter, session, statement), MoreExecutors.directExecutor());

    }

    @Override
    public ResultSet getUninterruptibly() {
        return safeGet().getUninterruptibly();
    }

    @Override
    public ResultSet getUninterruptibly(long timeout, TimeUnit unit) throws TimeoutException {
        long rateLimitStart = System.nanoTime();
        ResultSetFuture resultSetFuture = null;
        try {
            resultSetFuture = originalFuture.get(timeout, unit);
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException(e);
        }
        long rateLimitDurationNano = System.nanoTime() - rateLimitStart;
        long innerTimeoutNano = unit.toNanos(timeout) - rateLimitDurationNano;
        if (innerTimeoutNano > 0) {
            return resultSetFuture.getUninterruptibly(innerTimeoutNano, TimeUnit.NANOSECONDS);
        }
        throw new TimeoutException("Timeout waiting for task.");
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (originalFuture.isDone()) {
            return safeGet().cancel(mayInterruptIfRunning);
        } else {
            return originalFuture.cancel(mayInterruptIfRunning);
        }
    }

    @Override
    public boolean isCancelled() {
        if (originalFuture.isDone()) {
            return safeGet().isCancelled();
        }

        return originalFuture.isCancelled();
    }

    @Override
    public boolean isDone() {
        return originalFuture.isDone() && safeGet().isDone();
    }

    @Override
    public ResultSet get() throws InterruptedException, ExecutionException {
        return safeGet().get();
    }

    @Override
    public ResultSet get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        long rateLimitStart = System.nanoTime();
        ResultSetFuture resultSetFuture = originalFuture.get(timeout, unit);
        long rateLimitDurationNano = System.nanoTime() - rateLimitStart;
        long innerTimeoutNano = unit.toNanos(timeout) - rateLimitDurationNano;
        if (innerTimeoutNano > 0) {
            return resultSetFuture.get(innerTimeoutNano, TimeUnit.NANOSECONDS);
        }
        throw new TimeoutException("Timeout waiting for task.");
    }

    @Override
    public void addListener(Runnable listener, Executor executor) {
        originalFuture.addListener(() -> {
            try {
                ResultSetFuture resultSetFuture = Uninterruptibles.getUninterruptibly(originalFuture);
                resultSetFuture.addListener(listener, executor);
            } catch (CancellationException | ExecutionException e) {
                Futures.immediateFailedFuture(e).addListener(listener, executor);
            }
        }, executor);
    }

    private ResultSetFuture safeGet() {
        try {
            return originalFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }

    private ResultSetFuture executeAsyncWithRelease(AsyncRateLimiter rateLimiter, Session session, Statement statement) {
        try {
            ResultSetFuture resultSetFuture = session.executeAsync(statement);
            Futures.addCallback(resultSetFuture, new FutureCallback<ResultSet>() {
                @Override
                public void onSuccess(@Nullable ResultSet result) {
                    rateLimiter.release();
                }

                @Override
                public void onFailure(Throwable t) {
                    rateLimiter.release();
                }
            }, MoreExecutors.directExecutor());
            return resultSetFuture;
        } catch (RuntimeException re) {
            rateLimiter.release();
            throw re;
        }
    }
}
