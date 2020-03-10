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

import com.datastax.driver.core.ProtocolVersion;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.exceptions.UnsupportedFeatureException;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.thingsboard.server.dao.exception.BufferLimitException;
import org.thingsboard.server.dao.util.AsyncRateLimiter;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RateLimitedResultSetFutureTest {

    private RateLimitedResultSetFuture resultSetFuture;

    @Mock
    private AsyncRateLimiter rateLimiter;
    @Mock
    private Session session;
    @Mock
    private Statement statement;
    @Mock
    private ResultSetFuture realFuture;
    @Mock
    private ResultSet rows;
    @Mock
    private Row row;

    @Test
    public void doNotReleasePermissionIfRateLimitFutureFailed() throws InterruptedException {
        when(rateLimiter.acquireAsync()).thenReturn(Futures.immediateFailedFuture(new BufferLimitException()));
        resultSetFuture = new RateLimitedResultSetFuture(session, rateLimiter, statement);
        Thread.sleep(1000L);
        verify(rateLimiter).acquireAsync();
        try {
            assertTrue(resultSetFuture.isDone());
            fail();
        } catch (Exception e) {
            assertTrue(e instanceof IllegalStateException);
            Throwable actualCause = e.getCause();
            assertTrue(actualCause instanceof ExecutionException);
        }
        verifyNoMoreInteractions(session, rateLimiter, statement);

    }

    @Test
    public void getUninterruptiblyDelegateToCassandra() throws InterruptedException, ExecutionException {
        when(rateLimiter.acquireAsync()).thenReturn(Futures.immediateFuture(null));
        when(session.executeAsync(statement)).thenReturn(realFuture);
        Mockito.doAnswer((Answer<Void>) invocation -> {
            Object[] args = invocation.getArguments();
            Runnable task = (Runnable) args[0];
            task.run();
            return null;
        }).when(realFuture).addListener(Mockito.any(), Mockito.any());

        when(realFuture.getUninterruptibly()).thenReturn(rows);

        resultSetFuture = new RateLimitedResultSetFuture(session, rateLimiter, statement);
        ResultSet actual = resultSetFuture.getUninterruptibly();
        assertSame(rows, actual);
        verify(rateLimiter, times(1)).acquireAsync();
        verify(rateLimiter, times(1)).release();
    }

    @Test
    public void addListenerAllowsFutureTransformation() throws InterruptedException, ExecutionException {
        when(rateLimiter.acquireAsync()).thenReturn(Futures.immediateFuture(null));
        when(session.executeAsync(statement)).thenReturn(realFuture);
        Mockito.doAnswer((Answer<Void>) invocation -> {
            Object[] args = invocation.getArguments();
            Runnable task = (Runnable) args[0];
            task.run();
            return null;
        }).when(realFuture).addListener(Mockito.any(), Mockito.any());

        when(realFuture.get()).thenReturn(rows);
        when(rows.one()).thenReturn(row);

        resultSetFuture = new RateLimitedResultSetFuture(session, rateLimiter, statement);

        ListenableFuture<Row> transform = Futures.transform(resultSetFuture, ResultSet::one, MoreExecutors.directExecutor());
        Row actualRow = transform.get();

        assertSame(row, actualRow);
        verify(rateLimiter, times(1)).acquireAsync();
        verify(rateLimiter, times(1)).release();
    }

    @Test
    public void immidiateCassandraExceptionReturnsPermit() throws InterruptedException, ExecutionException {
        when(rateLimiter.acquireAsync()).thenReturn(Futures.immediateFuture(null));
        when(session.executeAsync(statement)).thenThrow(new UnsupportedFeatureException(ProtocolVersion.V3, "hjg"));
        resultSetFuture = new RateLimitedResultSetFuture(session, rateLimiter, statement);
        ListenableFuture<Row> transform = Futures.transform(resultSetFuture, ResultSet::one, MoreExecutors.directExecutor());
        try {
            transform.get();
            fail();
        } catch (Exception e) {
            assertTrue(e instanceof ExecutionException);
        }
        verify(rateLimiter, times(1)).acquireAsync();
        verify(rateLimiter, times(1)).release();
    }

    @Test
    public void queryTimeoutReturnsPermit() throws InterruptedException, ExecutionException {
        when(rateLimiter.acquireAsync()).thenReturn(Futures.immediateFuture(null));
        when(session.executeAsync(statement)).thenReturn(realFuture);
        Mockito.doAnswer((Answer<Void>) invocation -> {
            Object[] args = invocation.getArguments();
            Runnable task = (Runnable) args[0];
            task.run();
            return null;
        }).when(realFuture).addListener(Mockito.any(), Mockito.any());

        when(realFuture.get()).thenThrow(new ExecutionException("Fail", new TimeoutException("timeout")));
        resultSetFuture = new RateLimitedResultSetFuture(session, rateLimiter, statement);
        ListenableFuture<Row> transform = Futures.transform(resultSetFuture, ResultSet::one, MoreExecutors.directExecutor());
        try {
            transform.get();
            fail();
        } catch (Exception e) {
            assertTrue(e instanceof ExecutionException);
        }
        verify(rateLimiter, times(1)).acquireAsync();
        verify(rateLimiter, times(1)).release();
    }

    @Test
    public void expiredQueryReturnPermit() throws InterruptedException, ExecutionException {
        CountDownLatch latch = new CountDownLatch(1);
        ListenableFuture<Void> future = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(1)).submit(() -> {
            latch.await();
            return null;
        });
        when(rateLimiter.acquireAsync()).thenReturn(future);
        resultSetFuture = new RateLimitedResultSetFuture(session, rateLimiter, statement);

        ListenableFuture<Row> transform = Futures.transform(resultSetFuture, ResultSet::one, MoreExecutors.directExecutor());
//        TimeUnit.MILLISECONDS.sleep(200);
        future.cancel(false);
        latch.countDown();

        try {
            transform.get();
            fail();
        } catch (Exception e) {
            assertTrue(e instanceof ExecutionException);
        }
        verify(rateLimiter, times(1)).acquireAsync();
        verify(rateLimiter, times(1)).release();
    }

}