// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.nosql;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by ashvayka on 24.10.18.
 */
public class TbResultSetFuture implements ListenableFuture<TbResultSet> {

    private final SettableFuture<TbResultSet> mainFuture;

    public TbResultSetFuture(SettableFuture<TbResultSet> mainFuture) {
        this.mainFuture = mainFuture;
    }

    public TbResultSet getUninterruptibly() {
        return getSafe();
    }

    public TbResultSet getUninterruptibly(long timeout, TimeUnit unit) throws TimeoutException {
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
    public TbResultSet get() throws InterruptedException, ExecutionException {
        return mainFuture.get();
    }

    @Override
    public TbResultSet get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return mainFuture.get(timeout, unit);
    }

    @Override
    public void addListener(Runnable listener, Executor executor) {
        mainFuture.addListener(listener, executor);
    }

    private TbResultSet getSafe() {
        try {
            return mainFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }

    private TbResultSet getSafe(long timeout, TimeUnit unit) throws TimeoutException {
        try {
            return mainFuture.get(timeout, unit);
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }

}
