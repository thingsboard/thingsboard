// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.cassandra.guava;

import com.datastax.oss.driver.api.core.session.Request;
import com.datastax.oss.driver.api.core.type.reflect.GenericType;
import com.datastax.oss.driver.internal.core.context.InternalDriverContext;
import com.datastax.oss.driver.internal.core.session.DefaultSession;
import com.datastax.oss.driver.internal.core.session.RequestProcessor;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import java.util.concurrent.CompletionStage;

/**
 * Wraps a {@link RequestProcessor} that returns {@link CompletionStage}s and converts them to a
 * {@link ListenableFuture}s.
 *
 * @param <T> The type of request
 * @param <U> The type of responses enclosed in the future response.
 */
public class GuavaRequestAsyncProcessor<T extends Request, U>
        implements RequestProcessor<T, ListenableFuture<U>> {

    private final RequestProcessor<T, CompletionStage<U>> subProcessor;

    private final GenericType resultType;

    private final Class<?> requestClass;

    GuavaRequestAsyncProcessor(
            RequestProcessor<T, CompletionStage<U>> subProcessor,
            Class<?> requestClass,
            GenericType resultType) {
        this.subProcessor = subProcessor;
        this.requestClass = requestClass;
        this.resultType = resultType;
    }

    @Override
    public boolean canProcess(Request request, GenericType resultType) {
        return requestClass.isInstance(request) && resultType.equals(this.resultType);
    }

    @Override
    public ListenableFuture<U> process(
            T request, DefaultSession session, InternalDriverContext context, String sessionLogPrefix) {
        SettableFuture<U> future = SettableFuture.create();
        subProcessor
                .process(request, session, context, sessionLogPrefix)
                .whenComplete(
                        (r, ex) -> {
                            if (ex != null) {
                                future.setException(ex);
                            } else {
                                future.set(r);
                            }
                        });
        return future;
    }

    @Override
    public ListenableFuture<U> newFailure(RuntimeException error) {
        return Futures.immediateFailedFuture(error);
    }
}
