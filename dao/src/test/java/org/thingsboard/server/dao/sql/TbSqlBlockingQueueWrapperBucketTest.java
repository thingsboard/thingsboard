// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql;

import com.google.common.util.concurrent.ListenableFuture;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.stats.MessagesStats;
import org.thingsboard.server.common.stats.StatsFactory;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TbSqlBlockingQueueWrapperBucketTest {

    @Test
    void defaultBucketIsHashModuloThreads() {
        TbSqlBlockingQueueWrapper<String, Void> w = new TbSqlBlockingQueueWrapper<>(
                mock(TbSqlBlockingQueueParams.class), s -> 100, 8, mock(StatsFactory.class));
        assertThat(w.resolveBucket("anything")).isEqualTo(100 % 8);
    }

    @Test
    void nullElementGoesToBucketZero() {
        TbSqlBlockingQueueWrapper<String, Void> w = new TbSqlBlockingQueueWrapper<>(
                mock(TbSqlBlockingQueueParams.class), s -> 100, 8, mock(StatsFactory.class));
        assertThat(w.resolveBucket(null)).isZero();
    }

    @Test
    void explicitBucketResolverOverridesModulo() {
        TbSqlBlockingQueueWrapper<String, Void> w = new TbSqlBlockingQueueWrapper<>(
                mock(TbSqlBlockingQueueParams.class), s -> 100, 8, mock(StatsFactory.class), s -> 5);
        assertThat(w.resolveBucket("anything")).isEqualTo(5);
    }

    @Test
    void outOfRangeResolverBucketThrows() {
        TbSqlBlockingQueueWrapper<String, Void> w = new TbSqlBlockingQueueWrapper<>(
                mock(TbSqlBlockingQueueParams.class), s -> 100, 8, mock(StatsFactory.class), s -> 8); // 8 == maxThreads, out of [0,8)
        assertThatThrownBy(() -> w.resolveBucket("anything")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void negativeResolverBucketThrows() {
        TbSqlBlockingQueueWrapper<String, Void> w = new TbSqlBlockingQueueWrapper<>(
                mock(TbSqlBlockingQueueParams.class), s -> 100, 8, mock(StatsFactory.class), s -> -3);
        assertThatThrownBy(() -> w.resolveBucket("anything")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void biFunctionInitReceivesBucketChosenByResolver() throws Exception {
        int maxThreads = 4;
        int chosenBucket = 3;

        TbSqlBlockingQueueParams params = TbSqlBlockingQueueParams.builder()
                .logName("Test")
                .batchSize(10)
                .maxDelay(50)
                .statsPrintIntervalMs(60_000)
                .statsNamePrefix("test")
                .batchSortEnabled(false)
                .withResponse(false)
                .build();

        StatsFactory statsFactory = mock(StatsFactory.class);
        when(statsFactory.createMessagesStats(anyString())).thenReturn(mock(MessagesStats.class));

        ScheduledLogExecutorComponent logExecutor = new ScheduledLogExecutorComponent();
        logExecutor.init();

        // bucketResolver maps our single test element to a known bucket index
        TbSqlBlockingQueueWrapper<String, Void> w = new TbSqlBlockingQueueWrapper<>(
                params, s -> 0, maxThreads, statsFactory, s -> chosenBucket);

        ConcurrentHashMap<Integer, List<String>> received = new ConcurrentHashMap<>();
        CountDownLatch latch = new CountDownLatch(1);

        try {
            w.init(logExecutor, (bucket, batch) -> {
                received.put(bucket, batch);
                latch.countDown();
                return null;
            }, Comparator.naturalOrder(), l -> l);

            ListenableFuture<Void> future = w.add("element");
            future.get(5, TimeUnit.SECONDS);

            assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(received).containsOnlyKeys(chosenBucket);
            assertThat(received.get(chosenBucket)).containsExactly("element");
        } finally {
            w.destroy();
            logExecutor.stop();
        }
    }

    @Test
    void functionOverloadDelegatesAndDoesNotPassBucket() throws Exception {
        int maxThreads = 2;

        TbSqlBlockingQueueParams params = TbSqlBlockingQueueParams.builder()
                .logName("Test")
                .batchSize(10)
                .maxDelay(50)
                .statsPrintIntervalMs(60_000)
                .statsNamePrefix("test")
                .batchSortEnabled(false)
                .withResponse(false)
                .build();

        StatsFactory statsFactory = mock(StatsFactory.class);
        when(statsFactory.createMessagesStats(anyString())).thenReturn(mock(MessagesStats.class));

        ScheduledLogExecutorComponent logExecutor = new ScheduledLogExecutorComponent();
        logExecutor.init();

        // hashCodeFunction routes "element" deterministically; the existing Function overload must still work
        TbSqlBlockingQueueWrapper<String, Void> w = new TbSqlBlockingQueueWrapper<>(
                params, s -> 0, maxThreads, statsFactory);

        ConcurrentHashMap<String, List<String>> received = new ConcurrentHashMap<>();
        CountDownLatch latch = new CountDownLatch(1);

        try {
            w.init(logExecutor, (Function<List<String>, List<Void>>) batch -> {
                received.put("seen", batch);
                latch.countDown();
                return null;
            }, Comparator.naturalOrder(), l -> l);

            w.add("element").get(5, TimeUnit.SECONDS);

            assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(received.get("seen")).containsExactly("element");
        } finally {
            w.destroy();
            logExecutor.stop();
        }
    }
}
