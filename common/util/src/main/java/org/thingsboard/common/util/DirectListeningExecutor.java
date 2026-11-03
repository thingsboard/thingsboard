// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
