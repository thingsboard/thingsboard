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
import org.mvel2.ConversionException;
import org.thingsboard.common.util.JacksonUtil;

import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

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
        int hrs = 14;
        String pattern = "2024-02-29T%s:35:42.987Z";
        String tsStr = String.format(pattern, hrs);  //Thu Feb 29 2024 14:35:42.987 GMT+0000
        TbDate tbDate = new TbDate(tsStr);
        long ts = 1709217342987L; //Thu Feb 29 2024 14:35:42.987 GMT+0000
        Assert.assertEquals(ts, tbDate.parseSecondMilli());
        int offsetMin = tbDate.getTimezoneOffset(); // for example 3600000 for GMT + 1

        String datePrefix = tsStr; //without time zone
        assertThat(tbDate.toISOString())
                .as("format in main thread")
                .startsWith(datePrefix);
        assertThat(executor.submit(tbDate::toISOString).get(30, TimeUnit.SECONDS))
                .as("format in executor thread")
                .startsWith(datePrefix);
        int offsetHrs = offsetMin/60;
        String datePrefixLocal = String.format(pattern, (hrs - offsetHrs));
        long offsetMilli = offsetMin*60*1000;
        assertThat(new TbDate(ts - offsetMilli).toISOString())
                .as("new instance format in main thread")
                .startsWith(datePrefixLocal);
        assertThat(executor.submit(() -> new TbDate(ts - offsetMilli).toISOString()).get(30, TimeUnit.SECONDS))
                .as("new instance format in executor thread")
                .startsWith(datePrefixLocal);
    }

    @Test
    void testToLocaleDateString() {
        String s = "09:15:30 PM, Sun 10/09/2022";
        String pattern = "hh:mm:ss a, EEE M/d/uuuu";
        TbDate d = new TbDate(s, pattern, Locale.US);
        Assert.assertEquals("2022-10-09T18:15:30Z", d.toISOString());
        s = "09:15:30 пп, середа, 4 жовтня 2023 р.";
        pattern = "hh:mm:ss a, EEEE, d MMMM y 'р.'";
        d = new TbDate(s, pattern, Locale.forLanguageTag("uk-UA"));
        Assert.assertEquals("2023-10-04T18:15:30Z", d.toISOString());

        d = new TbDate(1693962245000L);
        Assert.assertEquals("2023-09-06T01:04:05Z", d.toISOString());

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
        Assert.assertEquals("5\u200F/9\u200F/2023, 9:04:05 م",  d.toLocaleString( "ar-EG", "America/New_York"));

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
        Assert.assertEquals("الثلاثاء، 5 سبتمبر 2023 في 9:04:05 م التوقيت الصيفي الشرقي لأمريكا الشمالية", d.toLocaleString("ar-EG", JacksonUtil.newObjectNode()
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

    @Test
    void TestFromString () {
        String stringDateUTC = "2023-09-06T01:04:05.00Z";
        TbDate d = new TbDate(stringDateUTC);
        Assert.assertEquals("2023-09-06T01:04:05Z", d.toISOString());
        String stringDateTZ = "2023-09-06T01:04:05.00+04:00";
        d = new TbDate(stringDateTZ);
        Assert.assertEquals("2023-09-05T21:04:05Z", d.toISOString());
        stringDateTZ = "2023-09-06T01:04:05.00-02:00";
        d = new TbDate(stringDateTZ);
        Assert.assertEquals("2023-09-06T03:04:05Z", d.toISOString());
        String stringDateRFC_1123  = "Sat, 3 Jun 2023 11:05:30 GMT";
        d = new TbDate(stringDateRFC_1123);
        stringDateRFC_1123  = "Sat, 3 Jun 2023 11:05:30 +0400";
        d = new TbDate(stringDateRFC_1123);
        Assert.assertEquals("2023-06-03T07:05:30Z", d.toISOString());
        stringDateRFC_1123  = "Thu, 29 Feb 2024 11:05:30 -03";
        d = new TbDate(stringDateRFC_1123);
        Assert.assertEquals("2024-02-29T14:05:30Z", d.toISOString());

        String stringDateRFC_1123_error  = "Tue, 3 Jun 2023 11:05:30 GMT";
        Exception actual = assertThrows(ConversionException.class, () -> {
            new TbDate(stringDateRFC_1123_error);
        });
        String expectedMessage = "Cannot parse value";
        assertTrue(actual.getMessage().contains(expectedMessage));
    }

    @Test
    void TestParse () {
        String stringDateUTC = "2023-09-06T01:04:05.345Z";
        TbDate d = new TbDate(stringDateUTC);
        Assert.assertEquals(1693962245345L, d.parseSecondMilli());
        Assert.assertEquals(1693962245L, d.parseSecond());
        String stringDateStart = "1970-01-01T00:00:00Z";
        d = new TbDate(stringDateStart);
        long actualMillis = TbDate.parse("1970-01-01 T00:00:00");
        Assert.assertEquals(d.getTimezoneOffset(), actualMillis/60/1000);
        String pattern = "yyyy-MM-dd HH:mm:ss.SSS";
        String stringDate = "1995-12-04 00:12:00.000";
        Assert.assertNotEquals(-1L,  TbDate.parse(stringDate, pattern));
    }

    @Test
    void TestDate_Year_Moth_Date_Hs_Min_Sec () {
        TbDate d = new TbDate(2023, 8, 18);
        Assert.assertEquals("2023-08-18T00:00:00Z", d.toISOString());
        d = new TbDate(2023, 9, 17, 17, 34);
        Assert.assertEquals("2023-09-17T17:34:00Z", d.toISOString());
        d = new TbDate(23, 9, 7, 8, 4);
        Assert.assertEquals("2023-09-07T08:04:00Z", d.toISOString());
        d = new TbDate(23, 9, 7, 8, 4, 5);
        Assert.assertEquals("2023-09-07T08:04:05Z", d.toISOString());
        d = new TbDate(23, 9, 7, 8, 4, 5, "+04:00");
        Assert.assertEquals("2023-09-07T04:04:05Z", d.toISOString());
        d = new TbDate(23, 9, 7, 8, 4, 5, "-03:00");
        Assert.assertEquals("2023-09-07T11:04:05Z", d.toISOString());
        d = new TbDate(23, 9, 7, 23, 4, 5, "-03:00");
        Assert.assertEquals("2023-09-08T02:04:05Z", d.toISOString());
        d = new TbDate(23, 9, 7, 23, 4, 5, 567,"-03:00");
        Assert.assertEquals("2023-09-08T02:04:05.567Z", d.toISOString());
    }

    @Test
    void TestMethodGetAsDateUTC () {
        TbDate dd = new TbDate(TbDate.UTC(1996, 2, 2, 3, 4, 5));
        Assert.assertEquals(823230245000L, dd.valueOf());
        dd = new TbDate(1996, 2, 2, 3, 4, 5);
        Assert.assertEquals(823230245000L, dd.valueOf());
        TbDate beforeStartUTC = new TbDate(1969, 7, 20, 20, 17, 40);
        Assert.assertEquals(-14182940000L, beforeStartUTC.getTime());

        TbDate d1 = new TbDate(1975, 12, 31, 23,15,30, 567,"+02:00");
        TbDate d2 = new TbDate(1975, 12, 31, 23,15,30, 567,"-02:00");

        Assert.assertEquals(189292530567L, d1.getTime());
        Assert.assertEquals(189306930567L, d2.getTime());
        Assert.assertEquals(d1.getTimezoneOffset(), d2.getTimezoneOffset());

        Assert.assertEquals(1975, d1.getUTCFullYear());
        Assert.assertEquals(1976, d2.getUTCFullYear());

        Assert.assertEquals(12, d1.getUTCMonth());
        Assert.assertEquals(1, d2.getUTCMonth());

        Assert.assertEquals(31, d1.getUTCDate());
        Assert.assertEquals(1, d2.getUTCDate());

        Assert.assertEquals(3, d1.getUTCDay());
        Assert.assertEquals(4, d2.getUTCDay());

        Assert.assertEquals(21, d1.getUTCHours());
        Assert.assertEquals(1, d2.getUTCHours());

        Assert.assertEquals(15, d1.getUTCMinutes());
        Assert.assertEquals(15, d2.getUTCMinutes());

        Assert.assertEquals(30, d1.getUTCSeconds());
        Assert.assertEquals(30, d2.getUTCSeconds());

        Assert.assertEquals(567, d1.getUTCMilliseconds());
        Assert.assertEquals(567, d2.getUTCMilliseconds());
    }    @Test
    void TestMethodGetAsDateLocal () {
        TbDate d1 = new TbDate(1975, 12, 31, 23,15,30, 567,"+02:00");
        TbDate d2 = new TbDate(1975, 12, 31, 23,15,30, 567,"-02:00");
        TbDate dLocal1 = new TbDate(d1.parseSecondMilli()-d1.getTimezoneOffset()*60*1000);
        TbDate dLocal2 = new TbDate(d2.parseSecondMilli()-d2.getTimezoneOffset()*60*1000);

        Assert.assertEquals(dLocal1.getUTCFullYear(), d1.getFullYear());
        Assert.assertEquals(dLocal2.getUTCFullYear(), d2.getFullYear());

        Assert.assertEquals(dLocal1.getUTCMonth(), d1.getMonth());
        Assert.assertEquals(dLocal2.getUTCMonth(), d2.getMonth());

        Assert.assertEquals(dLocal1.getUTCDate(), d1.getDate());
        Assert.assertEquals(dLocal2.getUTCDate(), d2.getDate());

        Assert.assertEquals(dLocal1.getUTCDay(), d1.getDay());
        Assert.assertEquals(dLocal1.getUTCDay(), d2.getDay());

        Assert.assertEquals(dLocal1.getUTCHours(), d1.getHours());
        Assert.assertEquals(dLocal2.getUTCHours(), d2.getHours());

        Assert.assertEquals(dLocal1.getUTCMinutes(), d1.getMinutes());
        Assert.assertEquals(dLocal2.getUTCMinutes(), d2.getMinutes());

        Assert.assertEquals(dLocal1.getUTCSeconds(), d1.getSeconds());
        Assert.assertEquals(dLocal2.getUTCSeconds(), d2.getSeconds());

        Assert.assertEquals(dLocal1.getUTCMilliseconds(), d1.getMilliseconds());
        Assert.assertEquals(dLocal2.getUTCMilliseconds(), d2.getMilliseconds());
    }

    @Test
    void TestMethodSetUTCFullYearMonthDate() {
        TbDate d1 = new TbDate(1975, 12, 31, 23,15,30, 567,"-03:00");
        Assert.assertEquals(1976, d1.getUTCFullYear());
        Assert.assertEquals(4, d1.getUTCDay());
        d1.setUTCFullYear(1969);
        Assert.assertEquals(1969, d1.getUTCFullYear());
        d1.setUTCFullYear(1975);
        Assert.assertEquals(1975, d1.getUTCFullYear());
        Assert.assertEquals(3, d1.getUTCDay());
        d1.setUTCFullYear(1977, 4);
        Assert.assertEquals(1977, d1.getUTCFullYear());
        Assert.assertEquals(4, d1.getUTCMonth());
        Assert.assertEquals(5, d1.getUTCDay());
        d1.setUTCFullYear(2023, 2, 24);
        Assert.assertEquals(2023, d1.getUTCFullYear());
        Assert.assertEquals(2, d1.getUTCMonth());
        Assert.assertEquals(24, d1.getUTCDate());
        Assert.assertEquals(5, d1.getUTCDay());

        d1.setUTCMonth(11);
        Assert.assertEquals(11, d1.getUTCMonth());
        Assert.assertEquals(5, d1.getUTCDay());
        d1.setUTCMonth(2, 28);
        Assert.assertEquals(2, d1.getUTCMonth());
        Assert.assertEquals(28, d1.getUTCDate());
        Assert.assertEquals(2, d1.getUTCDay());

        d1.setUTCDate(11);
        Assert.assertEquals(11, d1.getUTCDate());
        Assert.assertEquals(6, d1.getUTCDay());
    }

    @Test
    void TestMethodSetUTCHoursMinutesSecondsMilliSec() {
        TbDate d1 = new TbDate(1975, 12, 31, 23, 15, 30, 567, "-03:00");
        Assert.assertEquals(2, d1.getUTCHours());
        Assert.assertEquals(4, d1.getUTCDay());

        d1.setUTCHours(5);
        Assert.assertEquals(5, d1.getUTCHours());
        Assert.assertEquals(4, d1.getUTCDay());
        d1.setUTCHours(12, 45);
        Assert.assertEquals(12, d1.getUTCHours());
        Assert.assertEquals(45, d1.getUTCMinutes());
        Assert.assertEquals(4, d1.getUTCDay());
        d1.setUTCHours(0, 12, 59);
        Assert.assertEquals(0, d1.getUTCHours());
        Assert.assertEquals(12, d1.getUTCMinutes());
        Assert.assertEquals(59, d1.getUTCSeconds());
        Assert.assertEquals(4, d1.getUTCDay());
        d1.setUTCHours(4, 58, 2, 456);
        Assert.assertEquals(4, d1.getUTCHours());
        Assert.assertEquals(58, d1.getUTCMinutes());
        Assert.assertEquals(2, d1.getUTCSeconds());
        Assert.assertEquals(456, d1.getUTCMilliseconds());
        Assert.assertEquals(4, d1.getUTCDay());

        d1.setUTCMinutes(5);
        Assert.assertEquals(5, d1.getUTCMinutes());
        Assert.assertEquals(4, d1.getUTCDay());
        d1.setUTCMinutes(15, 32);
        Assert.assertEquals(15, d1.getUTCMinutes());
        Assert.assertEquals(32, d1.getUTCSeconds());
        Assert.assertEquals(4, d1.getUTCDay());
        d1.setUTCMinutes(10, 42, 321);
        Assert.assertEquals(10, d1.getUTCMinutes());
        Assert.assertEquals(42, d1.getUTCSeconds());
        Assert.assertEquals(321, d1.getUTCMilliseconds());
        Assert.assertEquals(4, d1.getUTCDay());

        d1.setUTCSeconds(5);
        Assert.assertEquals(5, d1.getUTCSeconds());
        Assert.assertEquals(4, d1.getUTCDay());
        d1.setUTCSeconds(15, 32);
        Assert.assertEquals(15, d1.getUTCSeconds());
        Assert.assertEquals(32, d1.getUTCMilliseconds());
        Assert.assertEquals(4, d1.getUTCDay());

        d1.setUTCMilliseconds(5);
        Assert.assertEquals(5, d1.getUTCMilliseconds());
        Assert.assertEquals(4, d1.getUTCDay());
    }
    @Test
    void TestMethodSetTome() {
        TbDate d1 = new TbDate(1975, 12, 31, 23, 15, 30, 567, "-03:00");
        long dateMilliSecond = d1.getTime();
        int fiveMinutesInMillis = 5 * 60 * 1000;
        Assert.assertEquals(15, d1.getUTCMinutes());
        Assert.assertEquals(4, d1.getUTCDay());
        d1.setTime(dateMilliSecond + fiveMinutesInMillis);
        Assert.assertEquals(20, d1.getUTCMinutes());
        Assert.assertEquals(4, d1.getUTCDay());
        d1.setTime(-378682769433L);
        Assert.assertEquals(1958, d1.getUTCFullYear());
        Assert.assertEquals(1, d1.getUTCMonth());
        Assert.assertEquals(1, d1.getUTCDate());
        Assert.assertEquals(2, d1.getUTCHours());
        Assert.assertEquals(20, d1.getUTCMinutes());
        Assert.assertEquals(30, d1.getUTCSeconds());
        Assert.assertEquals(567, d1.getUTCMilliseconds());
        Assert.assertEquals(3, d1.getUTCDay());
    }

    @Test
    void TestMethodSeFullYearMonthDate() {
        TbDate d1 = new TbDate(1975, 12, 31, 23,15,30, 567,"-03:00");
        Assert.assertEquals(1976, d1.getFullYear());
        Assert.assertEquals(1, d1.getMonth());
        Assert.assertEquals(1, d1.getDate());
        Assert.assertEquals(4, d1.getUTCDay());
        d1.setFullYear(1969);
        Assert.assertEquals(1969, d1.getFullYear());
        d1.setUTCFullYear(1975);
        Assert.assertEquals(1975, d1.getFullYear());
        Assert.assertEquals(3, d1.getUTCDay());
        d1.setFullYear(1977, 4);
        Assert.assertEquals(1977, d1.getFullYear());
        Assert.assertEquals(4, d1.getMonth());
        Assert.assertEquals(5, d1.getDay());
        d1.setFullYear(2023, 2, 24);
        Assert.assertEquals(2023, d1.getFullYear());
        Assert.assertEquals(2, d1.getMonth());
        Assert.assertEquals(24, d1.getDate());
        Assert.assertEquals(5, d1.getDay());

        d1.setMonth(11);
        Assert.assertEquals(11, d1.getMonth());
        Assert.assertEquals(5, d1.getDay());
        d1.setMonth(2, 28);
        Assert.assertEquals(2, d1.getMonth());
        Assert.assertEquals(28, d1.getDate());
        Assert.assertEquals(2, d1.getDay());

        d1.setDate(11);
        Assert.assertEquals(11, d1.getDate());
        Assert.assertEquals(6, d1.getDay());
    }
    @Test
    void TestMethodSeHoursMinutesSecondsMilliSec() {
        TbDate d1 = new TbDate(1975, 12, 31, 23, 15, 30, 567, "-03:00");
        Assert.assertEquals(5, d1.getHours());
        Assert.assertEquals(4, d1.getDay());

        d1.setHours(5);
        Assert.assertEquals(8, d1.getHours());
        Assert.assertEquals(4, d1.getDay());
        d1.setHours(23, 45);
        Assert.assertEquals(1, d1.getMonth());
        Assert.assertEquals(2, d1.getDate());
        Assert.assertEquals(2, d1.getHours());
        Assert.assertEquals(45, d1.getMinutes());
        Assert.assertEquals(5, d1.getDay());
        d1.setUTCHours(0, 12, 59);
        Assert.assertEquals(3, d1.getHours());
        Assert.assertEquals(12, d1.getMinutes());
        Assert.assertEquals(59, d1.getSeconds());
        Assert.assertEquals(4, d1.getDay());
        d1.setUTCHours(4, 58, 2, 456);
        Assert.assertEquals(7, d1.getHours());
        Assert.assertEquals(58, d1.getMinutes());
        Assert.assertEquals(2, d1.getSeconds());
        Assert.assertEquals(456, d1.getMilliseconds());
        Assert.assertEquals(4, d1.getDay());

        d1.setMinutes(5);
        Assert.assertEquals(5, d1.getMinutes());
        Assert.assertEquals(4, d1.getUTCDay());
        d1.setMinutes(15, 32);
        Assert.assertEquals(15, d1.getMinutes());
        Assert.assertEquals(32, d1.getSeconds());
        Assert.assertEquals(4, d1.getDay());
        d1.setMinutes(10, 42, 321);
        Assert.assertEquals(10, d1.getMinutes());
        Assert.assertEquals(42, d1.getSeconds());
        Assert.assertEquals(321, d1.getMilliseconds());
        Assert.assertEquals(4, d1.getDay());

        d1.setSeconds(5);
        Assert.assertEquals(5, d1.getSeconds());
        Assert.assertEquals(4, d1.getDay());
        d1.setSeconds(15, 32);
        Assert.assertEquals(15, d1.getSeconds());
        Assert.assertEquals(32, d1.getMilliseconds());
        Assert.assertEquals(4, d1.getDay());

        d1.setMilliseconds(5);
        Assert.assertEquals(5, d1.getMilliseconds());
        Assert.assertEquals(4, d1.getDay());
    }
    @Test
    public void toStringAsJs() {
        TbDate d1 = new TbDate(1975, 12, 31, 23,15,30, 567,"-14:00");
        Assert.assertEquals("1976 Jan 1, Thu 16:15:30 Eastern European Time", d1.toString());
        Assert.assertEquals("1976 Jan 1, Thu 16:15:30 Eastern European Time", d1.toString("GMT"));
        Assert.assertEquals("1976 Jan 1, Thu 16:15:30 Eastern European Time", d1.toString("UTC"));
        Assert.assertEquals("Thursday, January 1, 1976 at 4:15:30 PM Eastern European Standard Time", d1.toString("en-US"));
        Assert.assertEquals("1976 Jan 1, Thu 16:15:30", d1.toUTCString());
        Assert.assertEquals("четвер, 1 січня 1976 р., 16:15:30", d1.toUTCString("uk-UA"));
        Assert.assertEquals("Thursday, January 1, 1976, 4:15:30 PM", d1.toUTCString("en-US"));
        Assert.assertEquals("четвер, 1 січня 1976 р., 16:15:30", d1.toUTCString("uk-UA"));
        Assert.assertEquals("Thursday, January 1, 1976, 4:15:30 PM", d1.toUTCString("en-US"));
        Assert.assertEquals("1976 Jan 1, Thu", d1.toDateString());
        Assert.assertEquals("четвер, 1 січня 1976 р.", d1.toDateString("uk-UA"));
        Assert.assertEquals("Thursday, January 1, 1976", d1.toDateString("en-US"));
        Assert.assertEquals("16:15:30 Eastern European Time", d1.toTimeString());
        Assert.assertEquals("16:15:30 за східноєвропейським стандартним часом", d1.toTimeString("uk-UA"));
        Assert.assertEquals("4:15:30 PM Eastern European Standard Time", d1.toTimeString("en-US"));
        Assert.assertEquals("1976-01-01T13:15:30.567Z", d1.toJSON());
    }

    /**
     * Date.UTC(0)
     * -2208988800000
     * > "Mon, 01 Jan 1900 00:00:00 GMT"
     * new Date(Date.UTC(0, 0, 0, 0, 0, 0));
     * "Sun, 31 Dec 1899 00:00:00 GMT"
     */
    @Test
    public void toUTC() {
        Assert.assertEquals(-2209075200000L, TbDate.UTC(0));
        Assert.assertEquals("1899-12-31T00:00:00Z", new TbDate(TbDate.UTC(0)).toJSON());
        Assert.assertEquals("1996-02-02T03:04:05Z", new TbDate(TbDate.UTC(96, 2, 2, 3, 4, 5)).toJSON());
        Assert.assertEquals("2022-12-31T03:04:05.678Z", new TbDate(TbDate.UTC(22, 0, 0, 3, 4, 5, 678)).toJSON());
        Assert.assertEquals("0903-12-31T03:04:05.678Z", new TbDate(TbDate.UTC(903, 0, 0, 3, 4, 5, 678)).toJSON());
        Assert.assertEquals("1958-12-31T03:04:05.678Z", new TbDate(TbDate.UTC(1958, 0, 0, 3, 4, 5, 678)).toJSON());
        Assert.assertEquals("2032-04-05T03:04:05.678Z", new TbDate(TbDate.UTC(2032, 4, 5, 3, 4, 5, 678)).toJSON());
        Assert.assertEquals("2024-02-29T03:04:05.678Z", new TbDate(TbDate.UTC(2024, 2, 29, 3, 4, 5, 678)).toJSON());
        Exception actual = assertThrows(DateTimeParseException.class, () -> {
            TbDate.UTC(2023, 2, 29, 3, 4, 5, 678);
        });
        String expectedMessage = "could not be parsed";
        assertTrue(actual.getMessage().contains(expectedMessage));
    }
}

