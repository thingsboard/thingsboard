/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
import com.datastax.oss.driver.api.core.cql.ColumnDefinitions;
import com.datastax.oss.driver.api.core.cql.ExecutionInfo;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.Statement;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TbResultSetTest {

    @Test
    void allRows_withinLimit_returnsAllRows() throws Exception {
        Row row = mock(Row.class);
        AsyncResultSet asyncResultSet = createMockResultSet(List.of(row), false, 1000);
        Statement<?> statement = mock(Statement.class);

        TbResultSet tbResultSet = new TbResultSet(statement, asyncResultSet, s -> null);
        ListenableFuture<List<Row>> future = tbResultSet.allRows(MoreExecutors.directExecutor(), 5000);

        List<Row> result = future.get();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isSameAs(row);
    }

    @Test
    void allRows_exceedsLimitOnFirstPage_failsWithException() {
        Row row = mock(Row.class);
        AsyncResultSet asyncResultSet = createMockResultSet(List.of(row), false, 6000);
        Statement<?> statement = mock(Statement.class);

        TbResultSet tbResultSet = new TbResultSet(statement, asyncResultSet, s -> null);
        ListenableFuture<List<Row>> future = tbResultSet.allRows(MoreExecutors.directExecutor(), 5000);

        assertThatThrownBy(future::get)
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(ResultSetSizeLimitExceededException.class);
    }

    @Test
    void allRows_exceedsLimitOnSecondPage_failsAfterSecondPage() {
        Row row1 = mock(Row.class);
        Row row2 = mock(Row.class);
        Statement<?> statement = mock(Statement.class);
        doReturn(statement).when(statement).setPagingState((ByteBuffer) null);

        AsyncResultSet page2 = createMockResultSet(List.of(row2), false, 3000);
        TbResultSet tbResultSetPage2 = new TbResultSet(statement, page2, s -> null);
        SettableFuture<TbResultSet> page2Future = SettableFuture.create();
        page2Future.set(tbResultSetPage2);
        TbResultSetFuture tbPage2Future = new TbResultSetFuture(page2Future);

        ExecutionInfo page1ExecInfo = mock(ExecutionInfo.class);
        when(page1ExecInfo.getResponseSizeInBytes()).thenReturn(3000);
        when(page1ExecInfo.getPagingState()).thenReturn(null);

        AsyncResultSet page1 = createMockResultSet(List.of(row1), true, 3000);
        when(page1.getExecutionInfo()).thenReturn(page1ExecInfo);

        Function<Statement, TbResultSetFuture> executeAsync = s -> tbPage2Future;
        TbResultSet tbResultSet = new TbResultSet(statement, page1, executeAsync);
        ListenableFuture<List<Row>> future = tbResultSet.allRows(MoreExecutors.directExecutor(), 5000);

        assertThatThrownBy(future::get)
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(ResultSetSizeLimitExceededException.class);
    }

    @Test
    void allRows_unlimitedWithZero_returnsAllRowsRegardlessOfSize() throws Exception {
        Row row = mock(Row.class);
        AsyncResultSet asyncResultSet = createMockResultSet(List.of(row), false, 999999);
        Statement<?> statement = mock(Statement.class);

        TbResultSet tbResultSet = new TbResultSet(statement, asyncResultSet, s -> null);
        ListenableFuture<List<Row>> future = tbResultSet.allRows(MoreExecutors.directExecutor(), 0);

        List<Row> result = future.get();
        assertThat(result).hasSize(1);
    }

    @Test
    void allRows_noLimitOverload_returnsAllRows() throws Exception {
        Row row = mock(Row.class);
        AsyncResultSet asyncResultSet = createMockResultSet(List.of(row), false, 999999);
        Statement<?> statement = mock(Statement.class);

        TbResultSet tbResultSet = new TbResultSet(statement, asyncResultSet, s -> null);
        ListenableFuture<List<Row>> future = tbResultSet.allRows(MoreExecutors.directExecutor());

        List<Row> result = future.get();
        assertThat(result).hasSize(1);
    }

    private AsyncResultSet createMockResultSet(List<Row> rows, boolean hasMorePages, int responseSizeInBytes) {
        AsyncResultSet resultSet = mock(AsyncResultSet.class);
        ExecutionInfo executionInfo = mock(ExecutionInfo.class);
        ColumnDefinitions columnDefs = mock(ColumnDefinitions.class);

        when(executionInfo.getResponseSizeInBytes()).thenReturn(responseSizeInBytes);
        when(executionInfo.getPagingState()).thenReturn(null);
        when(resultSet.getExecutionInfo()).thenReturn(executionInfo);
        when(resultSet.getColumnDefinitions()).thenReturn(columnDefs);
        when(resultSet.currentPage()).thenReturn(rows);
        when(resultSet.hasMorePages()).thenReturn(hasMorePages);
        when(resultSet.remaining()).thenReturn(rows.size());

        return resultSet;
    }

}
