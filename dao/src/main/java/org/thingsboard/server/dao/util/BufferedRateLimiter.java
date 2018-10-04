/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
package org.thingsboard.server.dao.util;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.thingsboard.server.dao.exception.BufferLimitException;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
@NoSqlAnyDao
public class BufferedRateLimiter implements AsyncRateLimiter {

    private final ListeningExecutorService pool = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10));

    private final int permitsLimit;
    private final int maxPermitWaitTime;
    private final AtomicInteger permits;
    private final BlockingQueue<LockedFuture> queue;

    private final AtomicInteger maxQueueSize = new AtomicInteger();
    private final AtomicInteger maxGrantedPermissions = new AtomicInteger();
    private final AtomicInteger totalGranted = new AtomicInteger();
    private final AtomicInteger totalReleased = new AtomicInteger();
    private final AtomicInteger totalRequested = new AtomicInteger();

    public BufferedRateLimiter(@Value("${cassandra.query.buffer_size}") int queueLimit,
                               @Value("${cassandra.query.concurrent_limit}") int permitsLimit,
                               @Value("${cassandra.query.permit_max_wait_time}") int maxPermitWaitTime) {
        this.permitsLimit = permitsLimit;
        this.maxPermitWaitTime = maxPermitWaitTime;
        this.permits = new AtomicInteger();
        this.queue = new LinkedBlockingQueue<>(queueLimit);
    }

    @Override
    public ListenableFuture<Void> acquireAsync() {
        totalRequested.incrementAndGet();
        if (queue.isEmpty()) {
            if (permits.incrementAndGet() <= permitsLimit) {
                if (permits.get() > maxGrantedPermissions.get()) {
                    maxGrantedPermissions.set(permits.get());
                }
                totalGranted.incrementAndGet();
                return Futures.immediateFuture(null);
            }
            permits.decrementAndGet();
        }

        return putInQueue();
    }

    @Override
    public void release() {
        permits.decrementAndGet();
        totalReleased.incrementAndGet();
        reprocessQueue();
    }

    private void reprocessQueue() {
        while (permits.get() < permitsLimit) {
            if (permits.incrementAndGet() <= permitsLimit) {
                if (permits.get() > maxGrantedPermissions.get()) {
                    maxGrantedPermissions.set(permits.get());
                }
                LockedFuture lockedFuture = queue.poll();
                if (lockedFuture != null) {
                    totalGranted.incrementAndGet();
                    lockedFuture.latch.countDown();
                } else {
                    permits.decrementAndGet();
                    break;
                }
            } else {
                permits.decrementAndGet();
            }
        }
    }

    private LockedFuture createLockedFuture() {
        CountDownLatch latch = new CountDownLatch(1);
        ListenableFuture<Void> future = pool.submit(() -> {
            latch.await();
            return null;
        });
        return new LockedFuture(latch, future, System.currentTimeMillis());
    }

    private ListenableFuture<Void> putInQueue() {

        int size = queue.size();
        if (size > maxQueueSize.get()) {
            maxQueueSize.set(size);
        }

        if (queue.remainingCapacity() > 0) {
            try {
                LockedFuture lockedFuture = createLockedFuture();
                if (!queue.offer(lockedFuture, 1, TimeUnit.SECONDS)) {
                    lockedFuture.cancelFuture();
                    return Futures.immediateFailedFuture(new BufferLimitException());
                }
                if(permits.get() < permitsLimit) {
                    reprocessQueue();
                }
                if(permits.get() < permitsLimit) {
                    reprocessQueue();
                }
                return lockedFuture.future;
            } catch (InterruptedException e) {
                return Futures.immediateFailedFuture(new BufferLimitException());
            }
        }
        return Futures.immediateFailedFuture(new BufferLimitException());
    }

    @Scheduled(fixedDelayString = "${cassandra.query.rate_limit_print_interval_ms}")
    public void printStats() {
        int expiredCount = 0;
        for (LockedFuture lockedFuture : queue) {
            if (lockedFuture.isExpired()) {
                lockedFuture.cancelFuture();
                expiredCount++;
            }
        }
        log.info("Permits maxBuffer [{}] maxPermits [{}] expired [{}] currPermits [{}] currBuffer [{}] " +
                        "totalPermits [{}] totalRequests [{}] totalReleased [{}]",
                maxQueueSize.getAndSet(0), maxGrantedPermissions.getAndSet(0), expiredCount,
                permits.get(), queue.size(),
                totalGranted.getAndSet(0), totalRequested.getAndSet(0), totalReleased.getAndSet(0));
    }

    private class LockedFuture {
        final CountDownLatch latch;
        final ListenableFuture<Void> future;
        final long createTime;

        public LockedFuture(CountDownLatch latch, ListenableFuture<Void> future, long createTime) {
            this.latch = latch;
            this.future = future;
            this.createTime = createTime;
        }

        void cancelFuture() {
            future.cancel(false);
            latch.countDown();
        }

        boolean isExpired() {
            return (System.currentTimeMillis() - createTime) > maxPermitWaitTime;
        }

    }


}
