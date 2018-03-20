package org.thingsboard.rule.engine.api;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Callable;

public interface ListeningExecutor {

    <T> ListenableFuture<T> executeAsync(Callable<T> task);

    void onDestroy();
}
