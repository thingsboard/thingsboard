// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.sql;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

public interface TbSqlQueue<E, R> {

    void init(ScheduledLogExecutorComponent logExecutor, Function<List<E>, List<R>> saveFunction, Comparator<E> batchUpdateComparator, Function<List<TbSqlQueueElement<E, R>>, List<TbSqlQueueElement<E, R>>> filter, int queueIndex);

    void destroy();

    ListenableFuture<R> add(E element);
}
