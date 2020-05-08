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
import com.datastax.oss.driver.api.core.cql.Row;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

public class ResultSetUtils {

    public static ListenableFuture<List<Row>> allRows(AsyncResultSet resultSet, Executor executor) {
        List<ListenableFuture<AsyncResultSet>> futures = new ArrayList<>();
        futures.add(Futures.immediateFuture(resultSet));
        while (resultSet.hasMorePages()) {
            futures.add(toListenable(resultSet.fetchNextPage()));
        }
        return Futures.transform( Futures.allAsList(futures),
                resultSets -> resultSets.stream()
                        .map(rs -> loadRows(rs))
                        .flatMap(rows -> rows.stream())
                        .collect(Collectors.toList()),
                executor
        );
    }

    private static <T> ListenableFuture<T> toListenable(CompletionStage<T> completable) {
        SettableFuture<T> future = SettableFuture.create();
        completable.whenComplete(
                (r, ex) -> {
                    if (ex != null) {
                        future.setException(ex);
                    } else {
                        future.set(r);
                    }
                }
        );
        return future;
    }

    private static List<Row> loadRows(AsyncResultSet resultSet) {
        return Lists.newArrayList(resultSet.currentPage());
    }
}
