// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.nosql;

import com.google.common.base.Function;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.thingsboard.common.util.ThingsBoardExecutors;

import java.util.concurrent.ExecutorService;

/**
 * Created by ashvayka on 21.02.17.
 */
public abstract class CassandraAbstractAsyncDao extends CassandraAbstractDao {

    protected ExecutorService readResultsProcessingExecutor;

    @Value("${cassandra.query.result_processing_threads:50}")
    private int threadPoolSize;

    @Value("${cassandra.query.max_result_set_size_in_bytes:52428800}")
    protected long maxResultSetSizeBytes;

    @PostConstruct
    public void startExecutor() {
        readResultsProcessingExecutor = ThingsBoardExecutors.newWorkStealingPool(threadPoolSize, "cassandra-callback");
    }

    @PreDestroy
    public void stopExecutor() {
        if (readResultsProcessingExecutor != null) {
            readResultsProcessingExecutor.shutdownNow();
        }
    }

    protected <T> ListenableFuture<T> getFuture(TbResultSetFuture future, java.util.function.Function<TbResultSet, T> transformer) {
        return Futures.transform(future, new Function<TbResultSet, T>() {
            @Nullable
            @Override
            public T apply(@Nullable TbResultSet input) {
                return transformer.apply(input);
            }
        }, readResultsProcessingExecutor);
    }

    protected <T> ListenableFuture<T> getFutureAsync(TbResultSetFuture future, com.google.common.util.concurrent.AsyncFunction<TbResultSet, T> transformer) {
        return Futures.transformAsync(future, new AsyncFunction<TbResultSet, T>() {
            @Nullable
            @Override
            public ListenableFuture<T> apply(@Nullable TbResultSet input) {
                try {
                    return transformer.apply(input);
                } catch (Exception e) {
                    return Futures.immediateFailedFuture(e);
                }
            }
        }, readResultsProcessingExecutor);
    }

}
