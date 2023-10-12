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
import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.thingsboard.common.util.JacksonUtil;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
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

    @Test
    void testToLocaleDateString() {
        TbDate d = new TbDate(1693962245000L);

        // Depends on time zone, so we just check it works;
        Assert.assertNotNull(d.toLocaleDateString());
        Assert.assertNotNull(d.toLocaleDateString("en-US"));

        Assert.assertEquals("9/5/23", d.toLocaleDateString("en-US", "America/New_York")); 
        Assert.assertEquals("23. 9. 5.", d.toLocaleDateString("ko-KR",  "America/New_York")); 
        Assert.assertEquals("06.09.23",  d.toLocaleDateString( "uk-UA", "Europe/Kiev")); 
        Assert.assertEquals("5\u200F/9\u200F/2023",  d.toLocaleDateString( "ar-EG", "America/New_York")); 

        Assert.assertEquals("Tuesday, September 5, 2023", d.toLocaleDateString("en-US", JacksonUtil.newObjectNode()
                .put("timeZone", "America/New_York")
                .put("dateStyle", "full")
                .toString())); 
        Assert.assertEquals("2023년 9월 5일 화요일", d.toLocaleDateString("ko-KR", JacksonUtil.newObjectNode()
                .put("timeZone", "America/New_York")
                .put("dateStyle", "full")
                .toString())); 
        Assert.assertEquals("середа, 6 вересня 2023 р.", d.toLocaleDateString("uk-UA", JacksonUtil.newObjectNode()
                .put("timeZone", "Europe/Kiev")
                .put("dateStyle", "full")
                .toString())); 
        Assert.assertEquals("الثلاثاء، 5 سبتمبر 2023", d.toLocaleDateString("ar-EG", JacksonUtil.newObjectNode()
                .put("timeZone", "America/New_York")
                .put("dateStyle", "full")
                .toString())); 

        Assert.assertEquals("Tuesday 9/5/2023", d.toLocaleDateString("en-US", JacksonUtil.newObjectNode()
                .put("timeZone", "America/New_York")
                .put("pattern", "EEEE M/d/yyyy")
                .toString())); 
        Assert.assertEquals("화요일 9/5/2023", d.toLocaleDateString("ko-KR", JacksonUtil.newObjectNode()
                .put("timeZone", "America/New_York")
                .put("pattern", "EEEE M/d/yyyy")
                .toString())); 
        Assert.assertEquals("середа 9/6/2023", d.toLocaleDateString("uk-UA", JacksonUtil.newObjectNode()
                .put("timeZone", "Europe/Kiev")
                .put("pattern", "EEEE M/d/yyyy")
                .toString())); 
        Assert.assertEquals("الثلاثاء 9/5/2023", d.toLocaleDateString("ar-EG", JacksonUtil.newObjectNode()
                .put("timeZone", "America/New_York")
                .put("pattern", "EEEE M/d/yyyy")
                .toString())); 
    }

    @Test
    void testToLocaleTimeString() {
        TbDate d = new TbDate(1693962245000L);

        // Depends on time zone, so we just check it works;
        Assert.assertNotNull(d.toLocaleTimeString());
        Assert.assertNotNull(d.toLocaleTimeString("en-US"));


        Assert.assertEquals("9:04:05 PM", d.toLocaleTimeString("en-US", "America/New_York"));
        Assert.assertEquals("오후 9:04:05", d.toLocaleTimeString("ko-KR",  "America/New_York"));
        Assert.assertEquals("04:04:05",  d.toLocaleTimeString( "uk-UA", "Europe/Kiev"));
        Assert.assertEquals("9:04:05 م",  d.toLocaleTimeString( "ar-EG", "America/New_York"));

        Assert.assertEquals("9:04:05 PM Eastern Daylight Time", d.toLocaleTimeString("en-US", JacksonUtil.newObjectNode()
                .put("timeZone", "America/New_York")
                .put("timeStyle", "full")
                .toString())); 
        Assert.assertEquals("오후 9시 4분 5초 미 동부 하계 표준시", d.toLocaleTimeString("ko-KR", JacksonUtil.newObjectNode()
                .put("timeZone", "America/New_York")
                .put("timeStyle", "full")
                .toString())); 
        Assert.assertEquals("04:04:05 за східноєвропейським літнім часом", d.toLocaleTimeString("uk-UA", JacksonUtil.newObjectNode()
                .put("timeZone", "Europe/Kiev")
                .put("timeStyle", "full")
                .toString())); 
        Assert.assertEquals("9:04:05 م التوقيت الصيفي الشرقي لأمريكا الشمالية", d.toLocaleTimeString("ar-EG", JacksonUtil.newObjectNode()
                .put("timeZone", "America/New_York")
                .put("timeStyle", "full")
                .toString())); 

        Assert.assertEquals("9:04:05 PM", d.toLocaleTimeString("en-US", JacksonUtil.newObjectNode()
                .put("timeZone", "America/New_York")
                .put("pattern", "h:mm:ss a")
                .toString())); 
        Assert.assertEquals("9:04:05 오후", d.toLocaleTimeString("ko-KR", JacksonUtil.newObjectNode()
                .put("timeZone", "America/New_York")
                .put("pattern", "h:mm:ss a")
                .toString())); 
        Assert.assertEquals("4:04:05 дп", d.toLocaleTimeString("uk-UA", JacksonUtil.newObjectNode()
                .put("timeZone", "Europe/Kiev")
                .put("pattern", "h:mm:ss a")
                .toString())); 
        Assert.assertEquals("9:04:05 م", d.toLocaleTimeString("ar-EG", JacksonUtil.newObjectNode()
                .put("timeZone", "America/New_York")
                .put("pattern", "h:mm:ss a")
                .toString())); 
    }

    @Test
    void testToLocaleString() {
        TbDate d = new TbDate(1693962245000L);

        // Depends on time zone, so we just check it works;
        Assert.assertNotNull(d.toLocaleString());
        Assert.assertNotNull(d.toLocaleString("en-US"));

        Assert.assertEquals("9/5/23, 9:04:05 PM", d.toLocaleString("en-US", "America/New_York")); 
        Assert.assertEquals("23. 9. 5. 오후 9:04:05", d.toLocaleString("ko-KR",  "America/New_York")); 
        Assert.assertEquals("06.09.23, 04:04:05",  d.toLocaleString( "uk-UA", "Europe/Kiev")); 
        Assert.assertEquals("5\u200F/9\u200F/2023 9:04:05 م",  d.toLocaleString( "ar-EG", "America/New_York")); 

        Assert.assertEquals("Tuesday, September 5, 2023 at 9:04:05 PM Eastern Daylight Time", d.toLocaleString("en-US", JacksonUtil.newObjectNode()
                .put("timeZone", "America/New_York")
                .put("dateStyle", "full")
                .put("timeStyle", "full")
                .toString())); 
        Assert.assertEquals("2023년 9월 5일 화요일 오후 9시 4분 5초 미 동부 하계 표준시", d.toLocaleString("ko-KR", JacksonUtil.newObjectNode()
                .put("timeZone", "America/New_York")
                .put("dateStyle", "full")
                .put("timeStyle", "full")
                .toString())); 
        Assert.assertEquals("середа, 6 вересня 2023 р. о 04:04:05 за східноєвропейським літнім часом", d.toLocaleString("uk-UA", JacksonUtil.newObjectNode()
                .put("timeZone", "Europe/Kiev")
                .put("dateStyle", "full")
                .put("timeStyle", "full")
                .toString())); 
        Assert.assertEquals("الثلاثاء، 5 سبتمبر 2023 9:04:05 م التوقيت الصيفي الشرقي لأمريكا الشمالية", d.toLocaleString("ar-EG", JacksonUtil.newObjectNode()
                .put("timeZone", "America/New_York")
                .put("dateStyle", "full")
                .put("timeStyle", "full")
                .toString())); 

        Assert.assertEquals("9/5/2023, 9:04:05 PM", d.toLocaleString("en-US", JacksonUtil.newObjectNode()
                .put("timeZone", "America/New_York")
                .put("pattern", "M/d/yyyy, h:mm:ss a")
                .toString())); 
        Assert.assertEquals("9/5/2023, 9:04:05 오후", d.toLocaleString("ko-KR", JacksonUtil.newObjectNode()
                .put("timeZone", "America/New_York")
                .put("pattern", "M/d/yyyy, h:mm:ss a")
                .toString())); 
        Assert.assertEquals("9/6/2023, 4:04:05 дп", d.toLocaleString("uk-UA", JacksonUtil.newObjectNode()
                .put("timeZone", "Europe/Kiev")
                .put("pattern", "M/d/yyyy, h:mm:ss a")
                .toString())); 
        Assert.assertEquals("9/5/2023, 9:04:05 م", d.toLocaleString("ar-EG", JacksonUtil.newObjectNode()
                .put("timeZone", "America/New_York")
                .put("pattern", "M/d/yyyy, h:mm:ss a")
                .toString())); 
    }

    private static String toLocalString(TbDate d, Locale locale, ZoneId tz) {
        LocalDateTime ldt = d.toInstant().atZone(tz).toLocalDateTime();

//        new DateTimeFormatterBuilder().appendPattern(pattern).toFormatter(locale)

        String formatPattern =
                DateTimeFormatterBuilder.getLocalizedDateTimePattern(
                        FormatStyle.SHORT,
                        FormatStyle.MEDIUM,
                        IsoChronology.INSTANCE,
                        locale);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(formatPattern, locale);
        return formatter.format(ldt);
    }

    private static String toLocalString2(TbDate d, Locale locale, ZoneId tz) {
        DateFormat dateFormat = SimpleDateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, locale);
        dateFormat.setTimeZone(TimeZone.getTimeZone(tz));
        return dateFormat.format(d);
    }

}
