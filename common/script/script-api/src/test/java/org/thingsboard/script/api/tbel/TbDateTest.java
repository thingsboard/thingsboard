/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.script.api.tbel;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
class TbDateTest {

    ListeningExecutorService executor;

    @AfterEach
    void tearDown() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    /**
     * Note: This test simulated the high concurrency calls.
     * But it not always fails before as the concurrency issue happens far away from the test subject, inside the SimpleDateFormat class.
     * Few calls later after latch open, the concurrency issue is not well reproduced.
     * Depends on environment some failure may happen each 2 or 100 runs.
     * To be highly confident run this test in a ForkMode=method, repeat 100 times. This will provide about 99 failures per 100 runs.
     * The value of this test is *never* to fail when isoDateFormat.format(this) is properly synchronized
     * If this test fails time-to-time -- it is a sign that isoDateFormat.format() called concurrently and have to be fixed (synchronized)
     * The expected exception example:
     *   Caused by: java.lang.ArrayIndexOutOfBoundsException: Index 14 out of bounds for length 13
     * 	     at java.base/sun.util.calendar.BaseCalendar.getCalendarDateFromFixedDate(BaseCalendar.java:457)
     * 	     at java.base/java.util.GregorianCalendar.computeFields(GregorianCalendar.java:2394)
     * 	     at java.base/java.util.GregorianCalendar.computeFields(GregorianCalendar.java:2309)
     * 	     at java.base/java.util.Calendar.setTimeInMillis(Calendar.java:1834)
     * 	     at java.base/java.util.Calendar.setTime(Calendar.java:1800)
     * 	     at java.base/java.text.SimpleDateFormat.format(SimpleDateFormat.java:974)
     */
    @Test
    void testToISOStringConcurrently() throws ExecutionException, InterruptedException, TimeoutException {
        int threads = 5;
        executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(threads));
        for (int j = 0; j < 1000; j++) {
            final int iteration = j;
            CountDownLatch readyLatch = new CountDownLatch(threads);
            CountDownLatch latch = new CountDownLatch(1);
            long now = 1709217342000L;
            List<ListenableFuture<String>> futures = new ArrayList<>(threads);
            for (int i = 0; i < threads; i++) {
                long ts = now + TimeUnit.DAYS.toMillis(i * 366) + TimeUnit.MINUTES.toMillis(iteration) + TimeUnit.SECONDS.toMillis(iteration) + iteration;
                TbDate tbDate = new TbDate(ts);
                futures.add(executor.submit(() -> {
                    readyLatch.countDown();
                    if (!latch.await(30, TimeUnit.SECONDS)) {
                        throw new RuntimeException("await timeout");
                    }
                    return tbDate.toISOString();
                }));
            }
            ListenableFuture<List<String>> future = Futures.allAsList(futures);
            Futures.addCallback(future, new FutureCallback<List<String>>() {
                @Override
                public void onSuccess(List<String> result) {

                }

                @Override
                public void onFailure(Throwable t) {
                    log.error("Failure happens on iteration {}", iteration);
                }
            }, MoreExecutors.directExecutor());
            readyLatch.await(30, TimeUnit.SECONDS);
            latch.countDown();
            future.get(30, TimeUnit.SECONDS);
        }
    }

    @Test
    void testToISOStringThreadLocalStaticFormatter() throws ExecutionException, InterruptedException, TimeoutException {
        executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(1));
        long ts = 1709217342987L; //Thu Feb 29 2024 14:35:42.987 GMT+0000
        int offset = Calendar.getInstance().get(Calendar.ZONE_OFFSET); // for example 3600000 for GMT + 1
        TbDate tbDate = new TbDate(ts - offset);
        String datePrefix = "2024-02-29T14:35:42.987"; //without time zone
        assertThat(tbDate.toISOString())
                .as("format in main thread")
                .startsWith(datePrefix);
        assertThat(executor.submit(tbDate::toISOString).get(30, TimeUnit.SECONDS))
                .as("format in executor thread")
                .startsWith(datePrefix);
        assertThat(new TbDate(ts - offset).toISOString())
                .as("new instance format in main thread")
                .startsWith(datePrefix);
        assertThat(executor.submit(() -> new TbDate(ts - offset).toISOString()).get(30, TimeUnit.SECONDS))
                .as("new instance format in executor thread")
                .startsWith(datePrefix);
    }

}
