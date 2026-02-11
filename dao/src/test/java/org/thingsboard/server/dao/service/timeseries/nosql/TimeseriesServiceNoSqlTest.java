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
package org.thingsboard.server.dao.service.timeseries.nosql;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.junit.Test;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.dao.service.DaoNoSqlTest;
import org.thingsboard.server.dao.service.timeseries.BaseTimeseriesServiceTest;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@DaoNoSqlTest
public class TimeseriesServiceNoSqlTest extends BaseTimeseriesServiceTest {

    @Test
    public void shouldSaveEntryOfEachTypeWithTtl() throws ExecutionException, InterruptedException, TimeoutException {
        long ttlInSec = TimeUnit.SECONDS.toSeconds(3);
        List<TsKvEntry> timeseries = List.of(
                new BasicTsKvEntry(TimeUnit.MINUTES.toMillis(1), new BooleanDataEntry("test", true)),
                new BasicTsKvEntry(TimeUnit.MINUTES.toMillis(2), new StringDataEntry("test", "text")),
                new BasicTsKvEntry(TimeUnit.MINUTES.toMillis(3), new LongDataEntry("test", 15L)),
                new BasicTsKvEntry(TimeUnit.MINUTES.toMillis(4), new DoubleDataEntry("test", 10.5)),
                new BasicTsKvEntry(TimeUnit.MINUTES.toMillis(5), new JsonDataEntry("test", "{\"test\":\"testValue\"}")));

        DeviceId deviceId = new DeviceId(Uuids.timeBased());
        tsService.save(tenantId, deviceId, timeseries, ttlInSec).get(MAX_TIMEOUT, TimeUnit.SECONDS);

        List<TsKvEntry> fullList = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery("test", 0L,
                TimeUnit.MINUTES.toMillis(6), 1000, 10, Aggregation.NONE))).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        assertEquals(5, fullList.size());

        // check entries after ttl
        Thread.sleep(TimeUnit.SECONDS.toMillis(ttlInSec + 1));
        List<TsKvEntry> listAfterTtl = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery("test", 0L,
                TimeUnit.MINUTES.toMillis(6), 1000, 10, Aggregation.NONE))).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        assertEquals(0, listAfterTtl.size());
    }

    @Test
    public void shouldSaveBatchWithTtlAndVerifyEntryValues() throws ExecutionException, InterruptedException, TimeoutException {
        long ttlInSec = 300;
        BasicTsKvEntry booleanEntry = new BasicTsKvEntry(TimeUnit.MINUTES.toMillis(1), new BooleanDataEntry("test", true));
        BasicTsKvEntry stringEntry = new BasicTsKvEntry(TimeUnit.MINUTES.toMillis(2), new StringDataEntry("test", "text"));
        BasicTsKvEntry longEntry = new BasicTsKvEntry(TimeUnit.MINUTES.toMillis(3), new LongDataEntry("test", 15L));
        BasicTsKvEntry doubleEntry = new BasicTsKvEntry(TimeUnit.MINUTES.toMillis(4), new DoubleDataEntry("test", 10.5));
        BasicTsKvEntry jsonEntry = new BasicTsKvEntry(TimeUnit.MINUTES.toMillis(5), new JsonDataEntry("test", "{\"test\":\"testValue\"}"));

        DeviceId deviceId = new DeviceId(Uuids.timeBased());
        tsService.save(tenantId, deviceId, List.of(booleanEntry, stringEntry, longEntry, doubleEntry, jsonEntry), ttlInSec)
                .get(MAX_TIMEOUT, TimeUnit.SECONDS);

        List<TsKvEntry> fullList = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery("test", 0L,
                TimeUnit.MINUTES.toMillis(6), 1000, 10, Aggregation.NONE))).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        fullList.sort(Comparator.comparingLong(TsKvEntry::getTs));
        assertEquals(5, fullList.size());

        assertTrue(fullList.get(0).getBooleanValue().isPresent());
        assertEquals(true, fullList.get(0).getBooleanValue().get());

        assertTrue(fullList.get(1).getStrValue().isPresent());
        assertEquals("text", fullList.get(1).getStrValue().get());

        assertTrue(fullList.get(2).getLongValue().isPresent());
        assertEquals(Long.valueOf(15L), fullList.get(2).getLongValue().get());

        assertTrue(fullList.get(3).getDoubleValue().isPresent());
        assertEquals(Double.valueOf(10.5), fullList.get(3).getDoubleValue().get());

        assertTrue(fullList.get(4).getJsonValue().isPresent());
        assertEquals("{\"test\":\"testValue\"}", fullList.get(4).getJsonValue().get());
    }

    @Test
    public void shouldSaveBatchWithDifferentKeysAndTtl() throws ExecutionException, InterruptedException, TimeoutException {
        long ttlInSec = 300;
        long ts = TimeUnit.MINUTES.toMillis(1);
        List<TsKvEntry> timeseries = List.of(
                new BasicTsKvEntry(ts, new LongDataEntry("temperature", 25L)),
                new BasicTsKvEntry(ts, new DoubleDataEntry("humidity", 60.5)),
                new BasicTsKvEntry(ts, new BooleanDataEntry("active", true)));

        DeviceId deviceId = new DeviceId(Uuids.timeBased());
        tsService.save(tenantId, deviceId, timeseries, ttlInSec).get(MAX_TIMEOUT, TimeUnit.SECONDS);

        List<TsKvEntry> tempList = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery("temperature", 0L,
                ts + 1, 1000, 10, Aggregation.NONE))).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        assertEquals(1, tempList.size());
        assertEquals(Long.valueOf(25L), tempList.get(0).getLongValue().get());

        List<TsKvEntry> humidityList = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery("humidity", 0L,
                ts + 1, 1000, 10, Aggregation.NONE))).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        assertEquals(1, humidityList.size());
        assertEquals(Double.valueOf(60.5), humidityList.get(0).getDoubleValue().get());

        List<TsKvEntry> activeList = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery("active", 0L,
                ts + 1, 1000, 10, Aggregation.NONE))).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        assertEquals(1, activeList.size());
        assertEquals(true, activeList.get(0).getBooleanValue().get());
    }

    @Test
    public void shouldUpdateLatestAfterBatchSaveWithTtl() throws ExecutionException, InterruptedException, TimeoutException {
        long ttlInSec = 300;
        DeviceId deviceId = new DeviceId(Uuids.timeBased());

        // save initial value
        tsService.save(tenantId, deviceId, new BasicTsKvEntry(TimeUnit.MINUTES.toMillis(1), new LongDataEntry("test", 100L)))
                .get(MAX_TIMEOUT, TimeUnit.SECONDS);

        // batch save with later timestamps and TTL
        List<TsKvEntry> timeseries = List.of(
                new BasicTsKvEntry(TimeUnit.MINUTES.toMillis(2), new LongDataEntry("test", 200L)),
                new BasicTsKvEntry(TimeUnit.MINUTES.toMillis(3), new LongDataEntry("test", 300L)));
        tsService.save(tenantId, deviceId, timeseries, ttlInSec).get(MAX_TIMEOUT, TimeUnit.SECONDS);

        List<TsKvEntry> latest = tsService.findLatest(tenantId, deviceId, Collections.singleton("test")).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        assertEquals(1, latest.size());
        assertEquals(TimeUnit.MINUTES.toMillis(3), latest.get(0).getTs());
        assertEquals(Long.valueOf(300L), latest.get(0).getLongValue().get());
    }

    @Test
    public void shouldSaveWithoutLatestUpdateWithTtl() throws ExecutionException, InterruptedException, TimeoutException {
        long ttlInSec = 300;
        DeviceId deviceId = new DeviceId(Uuids.timeBased());

        // save initial entry that updates latest
        tsService.save(tenantId, deviceId, new BasicTsKvEntry(TimeUnit.MINUTES.toMillis(1), new LongDataEntry("test", 100L)))
                .get(MAX_TIMEOUT, TimeUnit.SECONDS);

        // save newer entries WITHOUT latest update
        List<TsKvEntry> timeseries = List.of(
                new BasicTsKvEntry(TimeUnit.MINUTES.toMillis(2), new LongDataEntry("test", 200L)),
                new BasicTsKvEntry(TimeUnit.MINUTES.toMillis(3), new LongDataEntry("test", 300L)));
        tsService.saveWithoutLatest(tenantId, deviceId, timeseries, ttlInSec).get(MAX_TIMEOUT, TimeUnit.SECONDS);

        // latest should still point to the first entry
        List<TsKvEntry> latest = tsService.findLatest(tenantId, deviceId, Collections.singleton("test")).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        assertEquals(1, latest.size());
        assertEquals(TimeUnit.MINUTES.toMillis(1), latest.get(0).getTs());
        assertEquals(Long.valueOf(100L), latest.get(0).getLongValue().get());

        // but all 3 entries should exist in the timeseries data
        List<TsKvEntry> fullList = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery("test", 0L,
                TimeUnit.MINUTES.toMillis(4), 1000, 10, Aggregation.NONE))).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        assertEquals(3, fullList.size());
    }

    @Test
    public void shouldSaveMultipleEntriesForSameKeyAndAggregateWithTtl() throws ExecutionException, InterruptedException, TimeoutException {
        long ttlInSec = 300;
        DeviceId deviceId = new DeviceId(Uuids.timeBased());

        List<TsKvEntry> timeseries = List.of(
                new BasicTsKvEntry(TimeUnit.MINUTES.toMillis(1), new LongDataEntry("value", 10L)),
                new BasicTsKvEntry(TimeUnit.MINUTES.toMillis(2), new LongDataEntry("value", 20L)),
                new BasicTsKvEntry(TimeUnit.MINUTES.toMillis(3), new LongDataEntry("value", 30L)),
                new BasicTsKvEntry(TimeUnit.MINUTES.toMillis(4), new LongDataEntry("value", 40L)));
        tsService.save(tenantId, deviceId, timeseries, ttlInSec).get(MAX_TIMEOUT, TimeUnit.SECONDS);

        // COUNT
        List<TsKvEntry> countList = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery("value", 0L,
                TimeUnit.MINUTES.toMillis(5), TimeUnit.MINUTES.toMillis(5), 10, Aggregation.COUNT))).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        assertEquals(1, countList.size());
        assertEquals(Long.valueOf(4L), countList.get(0).getLongValue().get());

        // SUM
        List<TsKvEntry> sumList = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery("value", 0L,
                TimeUnit.MINUTES.toMillis(5), TimeUnit.MINUTES.toMillis(5), 10, Aggregation.SUM))).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        assertEquals(1, sumList.size());
        assertEquals(Long.valueOf(100L), sumList.get(0).getLongValue().get());

        // AVG
        List<TsKvEntry> avgList = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery("value", 0L,
                TimeUnit.MINUTES.toMillis(5), TimeUnit.MINUTES.toMillis(5), 10, Aggregation.AVG))).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        assertEquals(1, avgList.size());
        assertEquals(Double.valueOf(25.0), avgList.get(0).getDoubleValue().get());

        // MIN
        List<TsKvEntry> minList = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery("value", 0L,
                TimeUnit.MINUTES.toMillis(5), TimeUnit.MINUTES.toMillis(5), 10, Aggregation.MIN))).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        assertEquals(1, minList.size());
        assertEquals(Long.valueOf(10L), minList.get(0).getLongValue().get());

        // MAX
        List<TsKvEntry> maxList = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery("value", 0L,
                TimeUnit.MINUTES.toMillis(5), TimeUnit.MINUTES.toMillis(5), 10, Aggregation.MAX))).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        assertEquals(1, maxList.size());
        assertEquals(Long.valueOf(40L), maxList.get(0).getLongValue().get());
    }

    @Test
    public void testNullValuesOfNoneTargetColumn() throws ExecutionException, InterruptedException, TimeoutException {
        long ts = TimeUnit.MINUTES.toMillis(1);
        long longValue = 10L;
        TsKvEntry longEntry = new BasicTsKvEntry(ts, new LongDataEntry("temp", longValue));
        double doubleValue = 20.6;
        TsKvEntry doubleEntry = new BasicTsKvEntry(ts, new DoubleDataEntry("temp", doubleValue));
        DeviceId deviceId = new DeviceId(Uuids.timeBased());
        tsService.save(tenantId, deviceId, longEntry).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        tsService.save(tenantId, deviceId, doubleEntry).get(MAX_TIMEOUT, TimeUnit.SECONDS);

        List<TsKvEntry> listWithoutAgg = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery("temp", 0L,
                ts + 1 , 1000, 3, Aggregation.NONE))).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        assertEquals(1, listWithoutAgg.size());
        assertTrue(listWithoutAgg.get(0).getLongValue().isPresent());
        assertFalse(listWithoutAgg.get(0).getDoubleValue().isPresent());
        assertThat(listWithoutAgg.get(0).getLongValue().get()).isEqualTo(longValue);

        // long value should not be reset to null, so avg = (doubleValue + longValue)/ 2
        List<TsKvEntry> listWithAgg = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery("temp", 0L,
                ts + 1, 200000, 3, Aggregation.AVG))).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        assertEquals(1, listWithAgg.size());
        assertTrue(listWithAgg.get(0).getDoubleValue().isPresent());
        double expectedValue = (doubleValue + longValue)/ 2;
        assertThat(listWithAgg.get(0).getDoubleValue().get()).isEqualTo(expectedValue);
    }
}
