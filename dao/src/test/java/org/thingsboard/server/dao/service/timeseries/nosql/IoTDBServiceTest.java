/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
import org.assertj.core.data.Offset;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DataType;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.objects.TelemetryEntityView;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.service.DaoIoTDBTest;
import org.thingsboard.server.dao.service.timeseries.BaseTimeseriesServiceTest;
import org.thingsboard.server.dao.timeseries.TimeseriesService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@DaoIoTDBTest
public class IoTDBServiceTest extends BaseTimeseriesServiceTest {

    @Autowired
    protected TimeseriesService tsService;

    @Autowired
    EntityViewService entityViewService;

    @Value("${database.ts.type}")
    String databaseTsLatestType;

    protected static final int MAX_TIMEOUT = 30;

    private static final String STRING_KEY = "stringKey";
    private static final String LONG_KEY = "longKey";
    private static final String DOUBLE_KEY = "doubleKey";
    private static final String BOOLEAN_KEY = "booleanKey";

    private static final long TS = 42L;
    private static final String DESC_ORDER = "DESC";

    KvEntry stringKvEntry = new StringDataEntry(STRING_KEY, "value");
    KvEntry longKvEntry = new LongDataEntry(LONG_KEY, Long.MAX_VALUE);
    KvEntry doubleKvEntry = new DoubleDataEntry(DOUBLE_KEY, Double.MAX_VALUE);
    KvEntry booleanKvEntry = new BooleanDataEntry(BOOLEAN_KEY, Boolean.TRUE);

    DeviceId deviceId = new DeviceId(Uuids.timeBased());

    @Test
    public void shouldSaveEntryOfEachType() throws ExecutionException, InterruptedException, TimeoutException {
        long ttlInSec = TimeUnit.SECONDS.toSeconds(3);
        List<TsKvEntry> timeseries = List.of(
                new BasicTsKvEntry(TimeUnit.MINUTES.toMillis(1), new BooleanDataEntry("test1", true)),
                new BasicTsKvEntry(TimeUnit.MINUTES.toMillis(2), new StringDataEntry("test2", "text")),
                new BasicTsKvEntry(TimeUnit.MINUTES.toMillis(3), new LongDataEntry("test3", 15L)),
                new BasicTsKvEntry(TimeUnit.MINUTES.toMillis(4), new DoubleDataEntry("test4", 10.5)));

        DeviceId deviceId = new DeviceId(Uuids.timeBased());
        tsService.save(tenantId, deviceId, timeseries, ttlInSec);

        List<TsKvEntry> fullList1 = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery("test1", 0L,
                TimeUnit.MINUTES.toMillis(6), 0, 10, Aggregation.NONE))).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        assertEquals(1, fullList1.size());

        List<TsKvEntry> fullList2= tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery("test2", 0L,
                TimeUnit.MINUTES.toMillis(6), 0, 10, Aggregation.NONE))).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        assertEquals(1, fullList2.size());

        List<TsKvEntry> fullList3= tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery("test3", 0L,
                TimeUnit.MINUTES.toMillis(6), 0, 10, Aggregation.NONE))).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        assertEquals(1, fullList3.size());

        List<TsKvEntry> fullList4= tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery("test4", 0L,
                TimeUnit.MINUTES.toMillis(6), 0, 10, Aggregation.NONE))).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        assertEquals(1, fullList4.size());

    }

    @Test
    public void testNullValuesOfNoneTargetColumn() throws ExecutionException, InterruptedException, TimeoutException {
        long ts = TimeUnit.MINUTES.toMillis(1);
        long longValue = 10L;
        TsKvEntry longEntry = new BasicTsKvEntry(ts, new LongDataEntry("temp1", longValue));
        long longValue2 = 20L;
        TsKvEntry doubleEntry = new BasicTsKvEntry(ts+1, new LongDataEntry("temp1", longValue2));
        DeviceId deviceId = new DeviceId(Uuids.timeBased());
        tsService.save(tenantId, deviceId, longEntry).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        tsService.save(tenantId, deviceId, doubleEntry).get(MAX_TIMEOUT, TimeUnit.SECONDS);

        List<TsKvEntry> listWithoutAgg = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery("temp1", 0L,
                ts + 2 , 0, 3, Aggregation.NONE))).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        assertEquals(2, listWithoutAgg.size());
        assertTrue(listWithoutAgg.get(0).getLongValue().isPresent());
        assertFalse(listWithoutAgg.get(0).getDoubleValue().isPresent());
        assertThat(listWithoutAgg.get(0).getLongValue().get()).isEqualTo(longValue2);

        // long value should not be reset to null, so avg = (doubleValue + longValue)/ 2
        List<TsKvEntry> listWithAgg = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery("temp1", 0L,
                ts + 2, 200000, 3, Aggregation.AVG))).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        assertEquals(1, listWithAgg.size());
        assertTrue(listWithAgg.get(0).getDoubleValue().isPresent());
        double expectedValue = (double) (longValue2 + longValue) / 2;
        assertThat(listWithAgg.get(0).getDoubleValue().get()).isEqualTo(expectedValue);
    }

    @Test
    public void testFindLatest_NotFound() throws Exception {
        List<TsKvEntry> entries = tsService.findLatest(tenantId, deviceId, Collections.singleton(STRING_KEY)).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        assertThat(entries).hasSize(1);
        TsKvEntry tsKvEntry = entries.get(0);
        assertThat(tsKvEntry).isNotNull();
        // null ts latest representation
        assertThat(tsKvEntry.getKey()).isEqualTo(STRING_KEY);
        assertThat(tsKvEntry.getDataType()).isEqualTo(DataType.STRING);
        assertThat(tsKvEntry.getValue()).isEqualTo("");
        assertThat(tsKvEntry.getTs()).isCloseTo(System.currentTimeMillis(), Offset.offset(TimeUnit.MINUTES.toMillis(1)));
    }
    @Test
    public void testFindLatestWithoutLatestUpdate() throws Exception {
        saveEntries(deviceId, TS - 2);
        saveEntries(deviceId, TS - 1);
        //saveEntriesWithoutLatest(deviceId, TS);

        List<TsKvEntry> entries = tsService.findLatest(tenantId, deviceId, Collections.singleton(STRING_KEY)).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        Assert.assertEquals(1, entries.size());
        equalsIgnoreVersion(toTsEntry(TS - 1, stringKvEntry), entries.get(0));
    }

    @Test
    public void testFindByQuery_whenPeriodEqualsInterval() throws Exception {
        saveEntries(deviceId, TS - 1L);
        for (long i = TS; i <= TS + 100L; i += 10L) {
            saveEntries(deviceId, i);
        }
        saveEntries(deviceId, TS + 100L + 1L);

        List<ReadTsKvQuery> queries = List.of(new BaseReadTsKvQuery(LONG_KEY, TS, TS + 100, 100, 1, Aggregation.COUNT, DESC_ORDER));

        List<TsKvEntry> entries = tsService.findAll(tenantId, deviceId, queries).get();
        Assert.assertEquals(1, entries.size());
        Assert.assertEquals(toTsEntry(TS , new LongDataEntry(LONG_KEY, 10L)), entries.get(0));

        EntityView entityView = saveAndCreateEntityView(deviceId, List.of(LONG_KEY));

        entries = tsService.findAll(tenantId, entityView.getId(), queries).get();
        Assert.assertEquals(1, entries.size());
        Assert.assertEquals(toTsEntry(TS , new LongDataEntry(LONG_KEY, 10L)), entries.get(0));
    }

    @Test
    public void testFindByQuery_whenPeriodHaveTwoIntervalWithEqualsLength() throws Exception {
        saveEntries(deviceId, TS - 1L);
        for (long i = TS; i <= TS + 100000L; i += 10000L) {
            saveEntries(deviceId, i);
        }
        saveEntries(deviceId, TS + 100000L + 1L);

        List<ReadTsKvQuery> queries = List.of(new BaseReadTsKvQuery(LONG_KEY, TS, TS + 99999, 50000, 1, Aggregation.COUNT, DESC_ORDER));

        List<TsKvEntry> entries = tsService.findAll(tenantId, deviceId, queries).get();
        Assert.assertEquals(2, entries.size());
        Assert.assertEquals(toTsEntry(TS , new LongDataEntry(LONG_KEY, 5L)), entries.get(0));
        Assert.assertEquals(toTsEntry(TS + 50000, new LongDataEntry(LONG_KEY, 5L)), entries.get(1));

        EntityView entityView = saveAndCreateEntityView(deviceId, List.of(LONG_KEY));

        entries = tsService.findAll(tenantId, entityView.getId(), queries).get();
        Assert.assertEquals(2, entries.size());
        Assert.assertEquals(toTsEntry(TS , new LongDataEntry(LONG_KEY, 5L)), entries.get(0));
        Assert.assertEquals(toTsEntry(TS + 50000, new LongDataEntry(LONG_KEY, 5L)), entries.get(1));
    }

    @Test
    public void testFindByQuery_whenPeriodHaveTwoInterval_whereSecondShorterThanFirst() throws Exception {
        saveEntries(deviceId, TS - 1L);
        for (long i = TS; i <= TS + 80000L; i += 10000L) {
            saveEntries(deviceId, i);
        }
        saveEntries(deviceId, TS + 80000L + 1L);

        List<ReadTsKvQuery> queries = List.of(new BaseReadTsKvQuery(LONG_KEY, TS, TS + 80000, 50000, 1, Aggregation.COUNT, DESC_ORDER));

        List<TsKvEntry> entries = tsService.findAll(tenantId, deviceId, queries).get();
        Assert.assertEquals(2, entries.size());
        Assert.assertEquals(toTsEntry(TS, new LongDataEntry(LONG_KEY, 5L)), entries.get(0));
        Assert.assertEquals(toTsEntry(TS + 50000, new LongDataEntry(LONG_KEY, 3L)), entries.get(1));

        EntityView entityView = saveAndCreateEntityView(deviceId, List.of(LONG_KEY));

        entries = tsService.findAll(tenantId, entityView.getId(), queries).get();
        Assert.assertEquals(2, entries.size());
        Assert.assertEquals(toTsEntry(TS, new LongDataEntry(LONG_KEY, 5L)), entries.get(0));
        Assert.assertEquals(toTsEntry(TS + 50000, new LongDataEntry(LONG_KEY, 3L)), entries.get(1));
    }

    @Test
    public void testFindByQuery_whenPeriodHaveTwoIntervalWithEqualsLength_whereNotAllEntriesInRange() throws Exception {
        for (long i = TS - 1L; i <= TS + 100000L + 1L; i += 10000) {
            saveEntries(deviceId, i);
        }

        List<ReadTsKvQuery> queries = List.of(new BaseReadTsKvQuery(LONG_KEY, TS, TS + 99999, 50000, 1, Aggregation.COUNT, DESC_ORDER));

        List<TsKvEntry> entries = tsService.findAll(tenantId, deviceId, queries).get();
        Assert.assertEquals(2, entries.size());
        Assert.assertEquals(toTsEntry(TS, new LongDataEntry(LONG_KEY, 5L)), entries.get(0));
        Assert.assertEquals(toTsEntry(TS + 50000, new LongDataEntry(LONG_KEY, 4L)), entries.get(1));

        EntityView entityView = saveAndCreateEntityView(deviceId, List.of(LONG_KEY));

        entries = tsService.findAll(tenantId, entityView.getId(), queries).get();
        Assert.assertEquals(2, entries.size());
        Assert.assertEquals(toTsEntry(TS, new LongDataEntry(LONG_KEY, 5L)), entries.get(0));
        Assert.assertEquals(toTsEntry(TS + 50000, new LongDataEntry(LONG_KEY, 4L)), entries.get(1));
    }

    @Test
    public void testFindByQuery_whenPeriodHaveTwoInterval_whereSecondShorterThanFirst_andNotAllEntriesInRange() throws Exception {
        for (long i = TS - 1L; i <= TS + 100000L + 1L; i += 10000L) {
            saveEntries(deviceId, i);
        }

        List<ReadTsKvQuery> queries = List.of(new BaseReadTsKvQuery(LONG_KEY, TS, TS + 80000, 50000, 1, Aggregation.COUNT, DESC_ORDER));

        List<TsKvEntry> entries = tsService.findAll(tenantId, deviceId, queries).get();
        Assert.assertEquals(2, entries.size());
        Assert.assertEquals(toTsEntry(TS, new LongDataEntry(LONG_KEY, 5L)), entries.get(0));
        Assert.assertEquals(toTsEntry(TS + 50000, new LongDataEntry(LONG_KEY, 3L)), entries.get(1));

        EntityView entityView = saveAndCreateEntityView(deviceId, List.of(LONG_KEY));

        entries = tsService.findAll(tenantId, entityView.getId(), queries).get();
        Assert.assertEquals(2, entries.size());
        Assert.assertEquals(toTsEntry(TS, new LongDataEntry(LONG_KEY, 5L)), entries.get(0));
        Assert.assertEquals(toTsEntry(TS + 50000, new LongDataEntry(LONG_KEY, 3L)), entries.get(1));
    }
    @Test
    public void testFindDeviceTsData() throws Exception {
        List<TsKvEntry> entries = new ArrayList<>();

        entries.add(save(deviceId, 5000, 100));
        entries.add(save(deviceId, 15000, 200));

        entries.add(save(deviceId, 25000, 300));
        entries.add(save(deviceId, 35000, 400));

        entries.add(save(deviceId, 45000, 500));
        entries.add(save(deviceId, 55000, 600));

        List<TsKvEntry> list = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery(LONG_KEY, 0,
                60000, 20000, 3, Aggregation.NONE))).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        assertEquals(3, list.size());
        assertEquals(55000, list.get(0).getTs());
        assertEquals(java.util.Optional.of(600L), list.get(0).getLongValue());

        assertEquals(45000, list.get(1).getTs());
        assertEquals(java.util.Optional.of(500L), list.get(1).getLongValue());

        assertEquals(35000, list.get(2).getTs());
        assertEquals(java.util.Optional.of(400L), list.get(2).getLongValue());

        list = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery(LONG_KEY, 0,
                60000, 20000, 3, Aggregation.AVG))).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        assertEquals(3, list.size());
        assertEquals(0, list.get(0).getTs());
        assertEquals(java.util.Optional.of(150.0), list.get(0).getDoubleValue());

        assertEquals(20000, list.get(1).getTs());
        assertEquals(java.util.Optional.of(350.0), list.get(1).getDoubleValue());

        assertEquals(40000, list.get(2).getTs());
        assertEquals(java.util.Optional.of(550.0), list.get(2).getDoubleValue());

        list = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery(LONG_KEY, 0,
                60000, 20000, 3, Aggregation.SUM))).get(MAX_TIMEOUT, TimeUnit.SECONDS);

        assertEquals(3, list.size());
        assertEquals(0, list.get(0).getTs());
        assertEquals(java.util.Optional.of(300.0), list.get(0).getDoubleValue());

        assertEquals(20000, list.get(1).getTs());
        assertEquals(java.util.Optional.of(700.0), list.get(1).getDoubleValue());

        assertEquals(40000, list.get(2).getTs());
        assertEquals(java.util.Optional.of(1100.0), list.get(2).getDoubleValue());

        list = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery(LONG_KEY, 0,
                60000, 20000, 3, Aggregation.MIN))).get(MAX_TIMEOUT, TimeUnit.SECONDS);

        assertEquals(3, list.size());
        assertEquals(0, list.get(0).getTs());
        assertEquals(java.util.Optional.of(100L), list.get(0).getLongValue());

        assertEquals(20000, list.get(1).getTs());
        assertEquals(java.util.Optional.of(300L), list.get(1).getLongValue());

        assertEquals(40000, list.get(2).getTs());
        assertEquals(java.util.Optional.of(500L), list.get(2).getLongValue());

        list = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery(LONG_KEY, 0,
                60000, 20000, 3, Aggregation.MAX))).get(MAX_TIMEOUT, TimeUnit.SECONDS);

        assertEquals(3, list.size());
        assertEquals(0, list.get(0).getTs());
        assertEquals(java.util.Optional.of(200L), list.get(0).getLongValue());

        assertEquals(20000, list.get(1).getTs());
        assertEquals(java.util.Optional.of(400L), list.get(1).getLongValue());

        assertEquals(40000, list.get(2).getTs());
        assertEquals(java.util.Optional.of(600L), list.get(2).getLongValue());

        list = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery(LONG_KEY, 0,
                60000, 20000, 3, Aggregation.COUNT))).get(MAX_TIMEOUT, TimeUnit.SECONDS);

        assertEquals(3, list.size());
        assertEquals(0, list.get(0).getTs());
        assertEquals(java.util.Optional.of(2L), list.get(0).getLongValue());

        assertEquals(20000, list.get(1).getTs());
        assertEquals(java.util.Optional.of(2L), list.get(1).getLongValue());

        assertEquals(40000, list.get(2).getTs());
        assertEquals(java.util.Optional.of(2L), list.get(2).getLongValue());

        entries.add(save(deviceId, 65000, "A1"));
        entries.add(save(deviceId, 75000, "A2"));
        entries.add(save(deviceId, 85000, "B1"));
        entries.add(save(deviceId, 95000, "B2"));
        entries.add(save(deviceId, 105000, "C1"));
        entries.add(save(deviceId, 115000, "C2"));

        list = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery(STRING_KEY, 60000,
                120000, 20000, 3, Aggregation.NONE))).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        assertEquals(3, list.size());
        assertEquals(115000, list.get(0).getTs());
        assertEquals(java.util.Optional.of("C2"), list.get(0).getStrValue());

        assertEquals(105000, list.get(1).getTs());
        assertEquals(java.util.Optional.of("C1"), list.get(1).getStrValue());

        assertEquals(95000, list.get(2).getTs());
        assertEquals(java.util.Optional.of("B2"), list.get(2).getStrValue());



        list = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery(STRING_KEY, 60000,
                120000, 20000, 3, Aggregation.COUNT))).get(MAX_TIMEOUT, TimeUnit.SECONDS);

        assertEquals(3, list.size());
        assertEquals(60000, list.get(0).getTs());
        assertEquals(java.util.Optional.of(2L), list.get(0).getLongValue());

        assertEquals(80000, list.get(1).getTs());
        assertEquals(java.util.Optional.of(2L), list.get(1).getLongValue());

        assertEquals(100000, list.get(2).getTs());
        assertEquals(java.util.Optional.of(2L), list.get(2).getLongValue());
    }

    @Test
    public void testFindDeviceLongAndDoubleTsData() throws Exception {
        List<TsKvEntry> entries = new ArrayList<>();

        entries.add(save(deviceId, 5000, 100));
        entries.add(save(deviceId, 15000, 200));
        entries.add(save(deviceId, 25000, 300));
        entries.add(save(deviceId, 35000, 400));
        entries.add(save(deviceId, 45000, 500));
        entries.add(save(deviceId, 55000, 600));

        entries.add(save(deviceId, 5000, 100.0));
        entries.add(save(deviceId, 15000, 200.0));
        entries.add(save(deviceId, 25000, 300.0));
        entries.add(save(deviceId, 35000, 400.0));
        entries.add(save(deviceId, 45000, 500.0));
        entries.add(save(deviceId, 55000, 600.0));

        List<TsKvEntry> list = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery(LONG_KEY, 0,
                60000, 20000, 3, Aggregation.NONE))).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        assertEquals(3, list.size());
        assertEquals(55000, list.get(0).getTs());
        assertEquals(java.util.Optional.of(600L), list.get(0).getLongValue());

        assertEquals(45000, list.get(1).getTs());
        assertEquals(java.util.Optional.of(500L), list.get(1).getLongValue());

        assertEquals(35000, list.get(2).getTs());
        assertEquals(java.util.Optional.of(400L), list.get(2).getLongValue());


        List<TsKvEntry> list_double = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery(DOUBLE_KEY, 0,
                60000, 20000, 3, Aggregation.NONE))).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        assertEquals(3, list_double.size());
        assertEquals(55000, list_double.get(0).getTs());
        assertEquals(java.util.Optional.of(600.0), list_double.get(0).getDoubleValue());

        assertEquals(45000, list_double.get(1).getTs());
        assertEquals(java.util.Optional.of(500.0), list_double.get(1).getDoubleValue());

        assertEquals(35000, list_double.get(2).getTs());
        assertEquals(java.util.Optional.of(400.0), list_double.get(2).getDoubleValue());


        list = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery(LONG_KEY, 0,
                60000, 20000, 3, Aggregation.AVG))).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        assertEquals(3, list.size());
        assertEquals(0, list.get(0).getTs());
        assertEquals(java.util.Optional.of(150.0), list.get(0).getDoubleValue());

        assertEquals(20000, list.get(1).getTs());
        assertEquals(java.util.Optional.of(350.0), list.get(1).getDoubleValue());

        assertEquals(40000, list.get(2).getTs());
        assertEquals(java.util.Optional.of(550.0), list.get(2).getDoubleValue());

        list = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery(LONG_KEY, 0,
                60000, 20000, 3, Aggregation.SUM))).get(MAX_TIMEOUT, TimeUnit.SECONDS);

        assertEquals(3, list.size());
        assertEquals(0, list.get(0).getTs());
        assertEquals(java.util.Optional.of(300.0), list.get(0).getDoubleValue());

        assertEquals(20000, list.get(1).getTs());
        assertEquals(java.util.Optional.of(700.0), list.get(1).getDoubleValue());

        assertEquals(40000, list.get(2).getTs());
        assertEquals(java.util.Optional.of(1100.0), list.get(2).getDoubleValue());

        list = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery(LONG_KEY, 0,
                60000, 20000, 3, Aggregation.MIN))).get(MAX_TIMEOUT, TimeUnit.SECONDS);

        assertEquals(3, list.size());
        assertEquals(0, list.get(0).getTs());
        assertEquals(java.util.Optional.of(100L), list.get(0).getLongValue());

        assertEquals(20000, list.get(1).getTs());
        assertEquals(java.util.Optional.of(300L), list.get(1).getLongValue());

        assertEquals(40000, list.get(2).getTs());
        assertEquals(java.util.Optional.of(500L), list.get(2).getLongValue());

        list = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery(LONG_KEY, 0,
                60000, 20000, 3, Aggregation.MAX))).get(MAX_TIMEOUT, TimeUnit.SECONDS);

        assertEquals(3, list.size());
        assertEquals(0, list.get(0).getTs());
        assertEquals(java.util.Optional.of(200L), list.get(0).getLongValue());

        assertEquals(20000, list.get(1).getTs());
        assertEquals(java.util.Optional.of(400L), list.get(1).getLongValue());

        assertEquals(40000, list.get(2).getTs());
        assertEquals(java.util.Optional.of(600L), list.get(2).getLongValue());

        list = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery(LONG_KEY, 0,
                60000, 20000, 3, Aggregation.COUNT))).get(MAX_TIMEOUT, TimeUnit.SECONDS);

        assertEquals(3, list.size());
        assertEquals(0, list.get(0).getTs());
        assertEquals(java.util.Optional.of(2L), list.get(0).getLongValue());

        assertEquals(20000, list.get(1).getTs());
        assertEquals(java.util.Optional.of(2L), list.get(1).getLongValue());

        assertEquals(40000, list.get(2).getTs());
        assertEquals(java.util.Optional.of(2L), list.get(2).getLongValue());
    }

    private TsKvEntry save(DeviceId deviceId, long ts, long value) throws Exception {
        TsKvEntry entry = new BasicTsKvEntry(ts, new LongDataEntry(LONG_KEY, value));
        tsService.save(tenantId, deviceId, entry).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        return entry;
    }
    private TsKvEntry save(DeviceId deviceId, long ts, double value) throws Exception {
        TsKvEntry entry = new BasicTsKvEntry(ts, new DoubleDataEntry(DOUBLE_KEY, value));
        tsService.save(tenantId, deviceId, entry).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        return entry;
    }

    private TsKvEntry save(DeviceId deviceId, long ts, String value) throws Exception {
        TsKvEntry entry = new BasicTsKvEntry(ts, new StringDataEntry(STRING_KEY, value));
        tsService.save(tenantId, deviceId, entry).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        return entry;
    }


    private EntityView saveAndCreateEntityView(DeviceId deviceId, List<String> timeseries) {
        EntityView entityView = new EntityView();
        entityView.setName("entity_view_name");
        entityView.setType("default");
        entityView.setTenantId(tenantId);
        TelemetryEntityView keys = new TelemetryEntityView();
        keys.setTimeseries(timeseries);
        entityView.setKeys(keys);
        entityView.setEntityId(deviceId);
        return entityViewService.saveEntityView(entityView);
    }

    private void saveEntries(DeviceId deviceId, long ts) throws ExecutionException, InterruptedException, TimeoutException {
        tsService.save(tenantId, deviceId, toTsEntry(ts, stringKvEntry)).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        tsService.save(tenantId, deviceId, toTsEntry(ts, longKvEntry)).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        tsService.save(tenantId, deviceId, toTsEntry(ts, doubleKvEntry)).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        tsService.save(tenantId, deviceId, toTsEntry(ts, booleanKvEntry)).get(MAX_TIMEOUT, TimeUnit.SECONDS);
    }
    private static TsKvEntry toTsEntry(long ts, KvEntry entry) {
        return new BasicTsKvEntry(ts, entry);
    }

    private static void equalsIgnoreVersion(TsKvEntry expected, TsKvEntry actual) {
        assertEquals(expected.getKey(), actual.getKey());
        assertEquals(expected.getValue(), actual.getValue());
        assertEquals(expected.getTs(), actual.getTs());
    }
}
