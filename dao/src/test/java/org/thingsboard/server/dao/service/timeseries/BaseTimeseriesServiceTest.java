/**
 * Copyright © 2016-2017 The Thingsboard Authors
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
package org.thingsboard.server.dao.service.timeseries;

import com.datastax.driver.core.utils.UUIDs;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.kv.*;
import org.thingsboard.server.dao.service.AbstractServiceTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Andrew Shvayka
 */

@Slf4j
public abstract class BaseTimeseriesServiceTest extends AbstractServiceTest {

    private static final String STRING_KEY = "stringKey";
    private static final String LONG_KEY = "longKey";
    private static final String DOUBLE_KEY = "doubleKey";
    private static final String BOOLEAN_KEY = "booleanKey";

    private static final long TS = 42L;

    KvEntry stringKvEntry = new StringDataEntry(STRING_KEY, "value");
    KvEntry longKvEntry = new LongDataEntry(LONG_KEY, Long.MAX_VALUE);
    KvEntry doubleKvEntry = new DoubleDataEntry(DOUBLE_KEY, Double.MAX_VALUE);
    KvEntry booleanKvEntry = new BooleanDataEntry(BOOLEAN_KEY, Boolean.TRUE);

    @Test
    public void testFindAllLatest() throws Exception {
        DeviceId deviceId = new DeviceId(UUIDs.timeBased());

        saveEntries(deviceId, TS - 2);
        saveEntries(deviceId, TS - 1);
        saveEntries(deviceId, TS);

        List<TsKvEntry> tsList = tsService.findAllLatest(deviceId).get();

        assertNotNull(tsList);
        assertEquals(4, tsList.size());
        for (int i = 0; i < tsList.size(); i++) {
            assertEquals(TS, tsList.get(i).getTs());
        }

        Collections.sort(tsList, (o1, o2) -> o1.getKey().compareTo(o2.getKey()));

        List<TsKvEntry> expected = Arrays.asList(
                toTsEntry(TS, stringKvEntry),
                toTsEntry(TS, longKvEntry),
                toTsEntry(TS, doubleKvEntry),
                toTsEntry(TS, booleanKvEntry));
        Collections.sort(expected, (o1, o2) -> o1.getKey().compareTo(o2.getKey()));

        assertEquals(expected, tsList);
    }

    @Test
    public void testFindLatest() throws Exception {
        DeviceId deviceId = new DeviceId(UUIDs.timeBased());

        saveEntries(deviceId, TS - 2);
        saveEntries(deviceId, TS - 1);
        saveEntries(deviceId, TS);

        List<TsKvEntry> entries = tsService.findLatest(deviceId, Collections.singleton(STRING_KEY)).get();
        Assert.assertEquals(1, entries.size());
        Assert.assertEquals(toTsEntry(TS, stringKvEntry), entries.get(0));
    }

    @Test
    public void testFindDeviceTsData() throws Exception {
        DeviceId deviceId = new DeviceId(UUIDs.timeBased());
        List<TsKvEntry> entries = new ArrayList<>();

        entries.add(save(deviceId, 5000, 100));
        entries.add(save(deviceId, 15000, 200));

        entries.add(save(deviceId, 25000, 300));
        entries.add(save(deviceId, 35000, 400));

        entries.add(save(deviceId, 45000, 500));
        entries.add(save(deviceId, 55000, 600));

        List<TsKvEntry> list = tsService.findAll(deviceId, Collections.singletonList(new BaseTsKvQuery(LONG_KEY, 0,
                60000, 20000, 3, Aggregation.NONE))).get();
        assertEquals(3, list.size());
        assertEquals(55000, list.get(0).getTs());
        assertEquals(java.util.Optional.of(600L), list.get(0).getLongValue());

        assertEquals(45000, list.get(1).getTs());
        assertEquals(java.util.Optional.of(500L), list.get(1).getLongValue());

        assertEquals(35000, list.get(2).getTs());
        assertEquals(java.util.Optional.of(400L), list.get(2).getLongValue());

        list = tsService.findAll(deviceId, Collections.singletonList(new BaseTsKvQuery(LONG_KEY, 0,
                60000, 20000, 3, Aggregation.AVG))).get();
        assertEquals(3, list.size());
        assertEquals(10000, list.get(0).getTs());
        assertEquals(java.util.Optional.of(150L), list.get(0).getLongValue());

        assertEquals(30000, list.get(1).getTs());
        assertEquals(java.util.Optional.of(350L), list.get(1).getLongValue());

        assertEquals(50000, list.get(2).getTs());
        assertEquals(java.util.Optional.of(550L), list.get(2).getLongValue());

        list = tsService.findAll(deviceId, Collections.singletonList(new BaseTsKvQuery(LONG_KEY, 0,
                60000, 20000, 3, Aggregation.SUM))).get();

        assertEquals(3, list.size());
        assertEquals(10000, list.get(0).getTs());
        assertEquals(java.util.Optional.of(300L), list.get(0).getLongValue());

        assertEquals(30000, list.get(1).getTs());
        assertEquals(java.util.Optional.of(700L), list.get(1).getLongValue());

        assertEquals(50000, list.get(2).getTs());
        assertEquals(java.util.Optional.of(1100L), list.get(2).getLongValue());

        list = tsService.findAll(deviceId, Collections.singletonList(new BaseTsKvQuery(LONG_KEY, 0,
                60000, 20000, 3, Aggregation.MIN))).get();

        assertEquals(3, list.size());
        assertEquals(10000, list.get(0).getTs());
        assertEquals(java.util.Optional.of(100L), list.get(0).getLongValue());

        assertEquals(30000, list.get(1).getTs());
        assertEquals(java.util.Optional.of(300L), list.get(1).getLongValue());

        assertEquals(50000, list.get(2).getTs());
        assertEquals(java.util.Optional.of(500L), list.get(2).getLongValue());

        list = tsService.findAll(deviceId, Collections.singletonList(new BaseTsKvQuery(LONG_KEY, 0,
                60000, 20000, 3, Aggregation.MAX))).get();

        assertEquals(3, list.size());
        assertEquals(10000, list.get(0).getTs());
        assertEquals(java.util.Optional.of(200L), list.get(0).getLongValue());

        assertEquals(30000, list.get(1).getTs());
        assertEquals(java.util.Optional.of(400L), list.get(1).getLongValue());

        assertEquals(50000, list.get(2).getTs());
        assertEquals(java.util.Optional.of(600L), list.get(2).getLongValue());

        list = tsService.findAll(deviceId, Collections.singletonList(new BaseTsKvQuery(LONG_KEY, 0,
                60000, 20000, 3, Aggregation.COUNT))).get();

        assertEquals(3, list.size());
        assertEquals(10000, list.get(0).getTs());
        assertEquals(java.util.Optional.of(2L), list.get(0).getLongValue());

        assertEquals(30000, list.get(1).getTs());
        assertEquals(java.util.Optional.of(2L), list.get(1).getLongValue());

        assertEquals(50000, list.get(2).getTs());
        assertEquals(java.util.Optional.of(2L), list.get(2).getLongValue());
    }

    private TsKvEntry save(DeviceId deviceId, long ts, long value) throws Exception {
        TsKvEntry entry = new BasicTsKvEntry(ts, new LongDataEntry(LONG_KEY, value));
        tsService.save(deviceId, entry).get();
        return entry;
    }

    private void saveEntries(DeviceId deviceId, long ts) throws ExecutionException, InterruptedException {
        tsService.save(deviceId, toTsEntry(ts, stringKvEntry)).get();
        tsService.save(deviceId, toTsEntry(ts, longKvEntry)).get();
        tsService.save(deviceId, toTsEntry(ts, doubleKvEntry)).get();
        tsService.save(deviceId, toTsEntry(ts, booleanKvEntry)).get();
    }

    private static TsKvEntry toTsEntry(long ts, KvEntry entry) {
        return new BasicTsKvEntry(ts, entry);
    }


}
