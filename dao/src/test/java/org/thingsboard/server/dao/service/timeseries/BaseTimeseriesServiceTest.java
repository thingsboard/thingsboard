/**
 * Copyright © 2016-2021 The Thingsboard Authors
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

import com.datastax.oss.driver.api.core.uuid.Uuids;
import lombok.extern.slf4j.Slf4j;
import org.junit.*;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.BaseDeleteTsKvQuery;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.objects.TelemetryEntityView;
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
    private static final String DESC_ORDER = "DESC";

    KvEntry stringKvEntry = new StringDataEntry(STRING_KEY, "value");
    KvEntry longKvEntry = new LongDataEntry(LONG_KEY, Long.MAX_VALUE);
    KvEntry doubleKvEntry = new DoubleDataEntry(DOUBLE_KEY, Double.MAX_VALUE);
    KvEntry booleanKvEntry = new BooleanDataEntry(BOOLEAN_KEY, Boolean.TRUE);

    private TenantId tenantId;

    @Before
    public void before() {
        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        Tenant savedTenant = tenantService.saveTenant(tenant);
        Assert.assertNotNull(savedTenant);
        tenantId = savedTenant.getId();
    }

    @After
    public void after() {
        tenantService.deleteTenant(tenantId);
    }

    @Test
    public void testFindAllLatest() throws Exception {
        DeviceId deviceId = new DeviceId(Uuids.timeBased());

        saveEntries(deviceId, TS - 2);
        saveEntries(deviceId, TS - 1);
        saveEntries(deviceId, TS);

        testLatestTsAndVerify(deviceId);
    }

    private void testLatestTsAndVerify(EntityId entityId) throws ExecutionException, InterruptedException {
        List<TsKvEntry> tsList = tsService.findAllLatest(tenantId, entityId).get();

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

    @Test
    public void testFindLatest() throws Exception {
        DeviceId deviceId = new DeviceId(Uuids.timeBased());

        saveEntries(deviceId, TS - 2);
        saveEntries(deviceId, TS - 1);
        saveEntries(deviceId, TS);

        List<TsKvEntry> entries = tsService.findLatest(tenantId, deviceId, Collections.singleton(STRING_KEY)).get();
        Assert.assertEquals(1, entries.size());
        Assert.assertEquals(toTsEntry(TS, stringKvEntry), entries.get(0));
    }

    @Test
    public void testFindByQueryAscOrder() throws Exception {
        DeviceId deviceId = new DeviceId(Uuids.timeBased());

        saveEntries(deviceId, TS - 2);
        saveEntries(deviceId, TS - 1);
        saveEntries(deviceId, TS);

        List<ReadTsKvQuery> queries = new ArrayList<>();
        queries.add(new BaseReadTsKvQuery(STRING_KEY, TS - 3, TS, 0, 1000, Aggregation.NONE, "ASC"));

        List<TsKvEntry> entries = tsService.findAll(tenantId, deviceId, queries).get();
        Assert.assertEquals(3, entries.size());
        Assert.assertEquals(toTsEntry(TS - 2, stringKvEntry), entries.get(0));
        Assert.assertEquals(toTsEntry(TS - 1, stringKvEntry), entries.get(1));
        Assert.assertEquals(toTsEntry(TS, stringKvEntry), entries.get(2));

        EntityView entityView = saveAndCreateEntityView(deviceId, Arrays.asList(STRING_KEY));

        entries = tsService.findAll(tenantId, entityView.getId(), queries).get();
        Assert.assertEquals(3, entries.size());
        Assert.assertEquals(toTsEntry(TS - 2, stringKvEntry), entries.get(0));
        Assert.assertEquals(toTsEntry(TS - 1, stringKvEntry), entries.get(1));
        Assert.assertEquals(toTsEntry(TS, stringKvEntry), entries.get(2));
    }

    @Test
    public void testFindByQueryDescOrder() throws Exception {
        DeviceId deviceId = new DeviceId(Uuids.timeBased());

        saveEntries(deviceId, TS - 2);
        saveEntries(deviceId, TS - 1);
        saveEntries(deviceId, TS);

        List<ReadTsKvQuery> queries = new ArrayList<>();
        queries.add(new BaseReadTsKvQuery(STRING_KEY, TS - 3, TS, 0, 1000, Aggregation.NONE, "DESC"));

        List<TsKvEntry> entries = tsService.findAll(tenantId, deviceId, queries).get();
        Assert.assertEquals(3, entries.size());
        Assert.assertEquals(toTsEntry(TS, stringKvEntry), entries.get(0));
        Assert.assertEquals(toTsEntry(TS - 1, stringKvEntry), entries.get(1));
        Assert.assertEquals(toTsEntry(TS - 2, stringKvEntry), entries.get(2));

        EntityView entityView = saveAndCreateEntityView(deviceId, Arrays.asList(STRING_KEY));

        entries = tsService.findAll(tenantId, entityView.getId(), queries).get();
        Assert.assertEquals(3, entries.size());
        Assert.assertEquals(toTsEntry(TS, stringKvEntry), entries.get(0));
        Assert.assertEquals(toTsEntry(TS - 1, stringKvEntry), entries.get(1));
        Assert.assertEquals(toTsEntry(TS - 2, stringKvEntry), entries.get(2));
    }

    @Test
    public void testDeleteDeviceTsDataWithOverwritingLatest() throws Exception {
        DeviceId deviceId = new DeviceId(Uuids.timeBased());

        saveEntries(deviceId, 10000);
        saveEntries(deviceId, 20000);
        saveEntries(deviceId, 30000);
        saveEntries(deviceId, 40000);

        tsService.remove(tenantId, deviceId, Collections.singletonList(
                new BaseDeleteTsKvQuery(STRING_KEY, 25000, 45000, true))).get();

        List<TsKvEntry> list = tsService.findAll(tenantId, deviceId, Collections.singletonList(
                new BaseReadTsKvQuery(STRING_KEY, 5000, 45000, 10000, 10, Aggregation.NONE))).get();
        Assert.assertEquals(2, list.size());

        List<TsKvEntry> latest = tsService.findLatest(tenantId, deviceId, Collections.singletonList(STRING_KEY)).get();
        Assert.assertEquals(20000, latest.get(0).getTs());
    }

    @Test
    public void testFindDeviceTsData() throws Exception {
        DeviceId deviceId = new DeviceId(Uuids.timeBased());
        List<TsKvEntry> entries = new ArrayList<>();

        entries.add(save(deviceId, 5000, 100));
        entries.add(save(deviceId, 15000, 200));

        entries.add(save(deviceId, 25000, 300));
        entries.add(save(deviceId, 35000, 400));

        entries.add(save(deviceId, 45000, 500));
        entries.add(save(deviceId, 55000, 600));

        List<TsKvEntry> list = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery(LONG_KEY, 0,
                60000, 20000, 3, Aggregation.NONE))).get();
        assertEquals(3, list.size());
        assertEquals(55000, list.get(0).getTs());
        assertEquals(java.util.Optional.of(600L), list.get(0).getLongValue());

        assertEquals(45000, list.get(1).getTs());
        assertEquals(java.util.Optional.of(500L), list.get(1).getLongValue());

        assertEquals(35000, list.get(2).getTs());
        assertEquals(java.util.Optional.of(400L), list.get(2).getLongValue());

        list = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery(LONG_KEY, 0,
                60000, 20000, 3, Aggregation.AVG))).get();
        assertEquals(3, list.size());
        assertEquals(10000, list.get(0).getTs());
        assertEquals(java.util.Optional.of(150.0), list.get(0).getDoubleValue());

        assertEquals(30000, list.get(1).getTs());
        assertEquals(java.util.Optional.of(350.0), list.get(1).getDoubleValue());

        assertEquals(50000, list.get(2).getTs());
        assertEquals(java.util.Optional.of(550.0), list.get(2).getDoubleValue());

        list = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery(LONG_KEY, 0,
                60000, 20000, 3, Aggregation.SUM))).get();

        assertEquals(3, list.size());
        assertEquals(10000, list.get(0).getTs());
        assertEquals(java.util.Optional.of(300L), list.get(0).getLongValue());

        assertEquals(30000, list.get(1).getTs());
        assertEquals(java.util.Optional.of(700L), list.get(1).getLongValue());

        assertEquals(50000, list.get(2).getTs());
        assertEquals(java.util.Optional.of(1100L), list.get(2).getLongValue());

        list = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery(LONG_KEY, 0,
                60000, 20000, 3, Aggregation.MIN))).get();

        assertEquals(3, list.size());
        assertEquals(10000, list.get(0).getTs());
        assertEquals(java.util.Optional.of(100L), list.get(0).getLongValue());

        assertEquals(30000, list.get(1).getTs());
        assertEquals(java.util.Optional.of(300L), list.get(1).getLongValue());

        assertEquals(50000, list.get(2).getTs());
        assertEquals(java.util.Optional.of(500L), list.get(2).getLongValue());

        list = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery(LONG_KEY, 0,
                60000, 20000, 3, Aggregation.MAX))).get();

        assertEquals(3, list.size());
        assertEquals(10000, list.get(0).getTs());
        assertEquals(java.util.Optional.of(200L), list.get(0).getLongValue());

        assertEquals(30000, list.get(1).getTs());
        assertEquals(java.util.Optional.of(400L), list.get(1).getLongValue());

        assertEquals(50000, list.get(2).getTs());
        assertEquals(java.util.Optional.of(600L), list.get(2).getLongValue());

        list = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery(LONG_KEY, 0,
                60000, 20000, 3, Aggregation.COUNT))).get();

        assertEquals(3, list.size());
        assertEquals(10000, list.get(0).getTs());
        assertEquals(java.util.Optional.of(2L), list.get(0).getLongValue());

        assertEquals(30000, list.get(1).getTs());
        assertEquals(java.util.Optional.of(2L), list.get(1).getLongValue());

        assertEquals(50000, list.get(2).getTs());
        assertEquals(java.util.Optional.of(2L), list.get(2).getLongValue());


        entries.add(save(deviceId, 65000, "A1"));
        entries.add(save(deviceId, 75000, "A2"));
        entries.add(save(deviceId, 85000, "B1"));
        entries.add(save(deviceId, 95000, "B2"));
        entries.add(save(deviceId, 105000, "C1"));
        entries.add(save(deviceId, 115000, "C2"));

        list = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery(LONG_KEY, 60000,
                120000, 20000, 3, Aggregation.NONE))).get();
        assertEquals(3, list.size());
        assertEquals(115000, list.get(0).getTs());
        assertEquals(java.util.Optional.of("C2"), list.get(0).getStrValue());

        assertEquals(105000, list.get(1).getTs());
        assertEquals(java.util.Optional.of("C1"), list.get(1).getStrValue());

        assertEquals(95000, list.get(2).getTs());
        assertEquals(java.util.Optional.of("B2"), list.get(2).getStrValue());


        list = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery(LONG_KEY, 60000,
                120000, 20000, 3, Aggregation.MIN))).get();

        assertEquals(3, list.size());
        assertEquals(70000, list.get(0).getTs());
        assertEquals(java.util.Optional.of("A1"), list.get(0).getStrValue());

        assertEquals(90000, list.get(1).getTs());
        assertEquals(java.util.Optional.of("B1"), list.get(1).getStrValue());

        assertEquals(110000, list.get(2).getTs());
        assertEquals(java.util.Optional.of("C1"), list.get(2).getStrValue());

        list = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery(LONG_KEY, 60000,
                120000, 20000, 3, Aggregation.MAX))).get();

        assertEquals(3, list.size());
        assertEquals(70000, list.get(0).getTs());
        assertEquals(java.util.Optional.of("A2"), list.get(0).getStrValue());

        assertEquals(90000, list.get(1).getTs());
        assertEquals(java.util.Optional.of("B2"), list.get(1).getStrValue());

        assertEquals(110000, list.get(2).getTs());
        assertEquals(java.util.Optional.of("C2"), list.get(2).getStrValue());

        list = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery(LONG_KEY, 60000,
                120000, 20000, 3, Aggregation.COUNT))).get();

        assertEquals(3, list.size());
        assertEquals(70000, list.get(0).getTs());
        assertEquals(java.util.Optional.of(2L), list.get(0).getLongValue());

        assertEquals(90000, list.get(1).getTs());
        assertEquals(java.util.Optional.of(2L), list.get(1).getLongValue());

        assertEquals(110000, list.get(2).getTs());
        assertEquals(java.util.Optional.of(2L), list.get(2).getLongValue());
    }

    @Test
    public void testFindDeviceLongAndDoubleTsData() throws Exception {
        DeviceId deviceId = new DeviceId(Uuids.timeBased());
        List<TsKvEntry> entries = new ArrayList<>();

        entries.add(save(deviceId, 5000, 100));
        entries.add(save(deviceId, 15000, 200.0));

        entries.add(save(deviceId, 25000, 300));
        entries.add(save(deviceId, 35000, 400.0));

        entries.add(save(deviceId, 45000, 500));
        entries.add(save(deviceId, 55000, 600.0));

        List<TsKvEntry> list = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery(LONG_KEY, 0,
                60000, 20000, 3, Aggregation.NONE))).get();
        assertEquals(3, list.size());
        assertEquals(55000, list.get(0).getTs());
        assertEquals(java.util.Optional.of(600.0), list.get(0).getDoubleValue());

        assertEquals(45000, list.get(1).getTs());
        assertEquals(java.util.Optional.of(500L), list.get(1).getLongValue());

        assertEquals(35000, list.get(2).getTs());
        assertEquals(java.util.Optional.of(400.0), list.get(2).getDoubleValue());

        list = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery(LONG_KEY, 0,
                60000, 20000, 3, Aggregation.AVG))).get();
        assertEquals(3, list.size());
        assertEquals(10000, list.get(0).getTs());
        assertEquals(java.util.Optional.of(150.0), list.get(0).getDoubleValue());

        assertEquals(30000, list.get(1).getTs());
        assertEquals(java.util.Optional.of(350.0), list.get(1).getDoubleValue());

        assertEquals(50000, list.get(2).getTs());
        assertEquals(java.util.Optional.of(550.0), list.get(2).getDoubleValue());

        list = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery(LONG_KEY, 0,
                60000, 20000, 3, Aggregation.SUM))).get();

        assertEquals(3, list.size());
        assertEquals(10000, list.get(0).getTs());
        assertEquals(java.util.Optional.of(300.0), list.get(0).getDoubleValue());

        assertEquals(30000, list.get(1).getTs());
        assertEquals(java.util.Optional.of(700.0), list.get(1).getDoubleValue());

        assertEquals(50000, list.get(2).getTs());
        assertEquals(java.util.Optional.of(1100.0), list.get(2).getDoubleValue());

        list = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery(LONG_KEY, 0,
                60000, 20000, 3, Aggregation.MIN))).get();

        assertEquals(3, list.size());
        assertEquals(10000, list.get(0).getTs());
        assertEquals(java.util.Optional.of(100.0), list.get(0).getDoubleValue());

        assertEquals(30000, list.get(1).getTs());
        assertEquals(java.util.Optional.of(300.0), list.get(1).getDoubleValue());

        assertEquals(50000, list.get(2).getTs());
        assertEquals(java.util.Optional.of(500.0), list.get(2).getDoubleValue());

        list = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery(LONG_KEY, 0,
                60000, 20000, 3, Aggregation.MAX))).get();

        assertEquals(3, list.size());
        assertEquals(10000, list.get(0).getTs());
        assertEquals(java.util.Optional.of(200.0), list.get(0).getDoubleValue());

        assertEquals(30000, list.get(1).getTs());
        assertEquals(java.util.Optional.of(400.0), list.get(1).getDoubleValue());

        assertEquals(50000, list.get(2).getTs());
        assertEquals(java.util.Optional.of(600.0), list.get(2).getDoubleValue());

        list = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery(LONG_KEY, 0,
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
        tsService.save(tenantId, deviceId, entry).get();
        return entry;
    }

    private TsKvEntry save(DeviceId deviceId, long ts, double value) throws Exception {
        TsKvEntry entry = new BasicTsKvEntry(ts, new DoubleDataEntry(LONG_KEY, value));
        tsService.save(tenantId, deviceId, entry).get();
        return entry;
    }

    private TsKvEntry save(DeviceId deviceId, long ts, String value) throws Exception {
        TsKvEntry entry = new BasicTsKvEntry(ts, new StringDataEntry(LONG_KEY, value));
        tsService.save(tenantId, deviceId, entry).get();
        return entry;
    }


    private void saveEntries(DeviceId deviceId, long ts) throws ExecutionException, InterruptedException {
        tsService.save(tenantId, deviceId, toTsEntry(ts, stringKvEntry)).get();
        tsService.save(tenantId, deviceId, toTsEntry(ts, longKvEntry)).get();
        tsService.save(tenantId, deviceId, toTsEntry(ts, doubleKvEntry)).get();
        tsService.save(tenantId, deviceId, toTsEntry(ts, booleanKvEntry)).get();
    }

    private static TsKvEntry toTsEntry(long ts, KvEntry entry) {
        return new BasicTsKvEntry(ts, entry);
    }


}
