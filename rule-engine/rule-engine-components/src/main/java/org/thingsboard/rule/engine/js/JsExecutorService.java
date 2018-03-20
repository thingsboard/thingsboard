package org.thingsboard.rule.engine.js;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.thingsboard.rule.engine.api.ListeningExecutor;

import javax.annotation.PreDestroy;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

public class JsExecutorService implements ListeningExecutor{

    private final ListeningExecutorService service;

    public JsExecutorService(int threadCount) {
        this.service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(threadCount));
    }

    @Override
    public <T> ListenableFuture<T> executeAsync(Callable<T> task) {
        return service.submit(task);
    }

    @PreDestroy
    @Override
    public void onDestroy() {
        service.shutdown();
    }
}
