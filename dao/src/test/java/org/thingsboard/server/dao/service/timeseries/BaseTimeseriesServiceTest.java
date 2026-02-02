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
package org.thingsboard.server.dao.service.timeseries;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.data.Offset;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import org.thingsboard.server.common.data.kv.DataType;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQueryResult;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.objects.TelemetryEntityView;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.service.AbstractServiceTest;
import org.thingsboard.server.dao.timeseries.TimeseriesService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Andrew Shvayka
 */

@Slf4j
public abstract class BaseTimeseriesServiceTest extends AbstractServiceTest {

    @Autowired
    protected TimeseriesService tsService;

    @Autowired
    EntityViewService entityViewService;

    @Value("${database.ts.type}")
    String databaseTsLatestType;

    protected static final int MAX_TIMEOUT = 30;

    protected static final String STRING_KEY = "stringKey";
    private static final String LONG_KEY = "longKey";
    private static final String DOUBLE_KEY = "doubleKey";
    private static final String BOOLEAN_KEY = "booleanKey";

    protected static final long TS = 42L;
    private static final String DESC_ORDER = "DESC";

    protected KvEntry stringKvEntry = new StringDataEntry(STRING_KEY, "value");
    KvEntry longKvEntry = new LongDataEntry(LONG_KEY, Long.MAX_VALUE);
    KvEntry doubleKvEntry = new DoubleDataEntry(DOUBLE_KEY, Double.MAX_VALUE);
    KvEntry booleanKvEntry = new BooleanDataEntry(BOOLEAN_KEY, Boolean.TRUE);

    protected TenantId tenantId;
    protected DeviceId deviceId = new DeviceId(Uuids.timeBased());

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
        saveEntries(deviceId, TS - 2);
        saveEntries(deviceId, TS - 1);
        saveEntries(deviceId, TS);

        testLatestTsAndVerify(deviceId);
    }

    private void testLatestTsAndVerify(EntityId entityId) throws ExecutionException, InterruptedException, TimeoutException {
        List<TsKvEntry> tsList = tsService.findAllLatest(tenantId, entityId).get(MAX_TIMEOUT, TimeUnit.SECONDS);

        assertNotNull(tsList);
        assertEquals(4, tsList.size());
        for (TsKvEntry entry : tsList) {
            assertEquals(TS, entry.getTs());
        }

        Collections.sort(tsList, Comparator.comparing(KvEntry::getKey));

        List<TsKvEntry> expected = Arrays.asList(
                toTsEntry(TS, stringKvEntry),
                toTsEntry(TS, longKvEntry),
                toTsEntry(TS, doubleKvEntry),
                toTsEntry(TS, booleanKvEntry));
        Collections.sort(expected, Comparator.comparing(KvEntry::getKey));

        for (int i = 0; i < expected.size(); i++) {
            var expectedEntry = expected.get(i);
            var actualEntry = tsList.get(i);
            equalsIgnoreVersion(expectedEntry, actualEntry);

        }
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
        saveEntries(deviceId, TS - 2);
        saveEntries(deviceId, TS - 1);
        saveEntries(deviceId, TS);

        List<TsKvEntry> entries = tsService.findLatest(tenantId, deviceId, Collections.singleton(STRING_KEY)).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        Assert.assertEquals(1, entries.size());
        equalsIgnoreVersion(toTsEntry(TS, stringKvEntry), entries.get(0));
    }

    @Test
    public void testFindLatestOpt_givenSaveWithHistoricalNonOrderedTS() throws Exception {
        if (databaseTsLatestType.equals("cassandra")) {
            return;
        }

        save(tenantId, deviceId, toTsEntry(TS - 1, stringKvEntry));
        save(tenantId, deviceId, toTsEntry(TS, stringKvEntry));
        save(tenantId, deviceId, toTsEntry(TS - 10, stringKvEntry));
        save(tenantId, deviceId, toTsEntry(TS - 11, stringKvEntry));

        Optional<TsKvEntry> entryOpt = tsService.findLatest(tenantId, deviceId, STRING_KEY).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        assertThat(entryOpt).isNotNull().isPresent();
        equalsIgnoreVersion(toTsEntry(TS, stringKvEntry), entryOpt.get());
    }

    @Test
    public void testFindLatestOpt_givenSaveWithSameTSOverwriteValue() throws Exception {
        save(tenantId, deviceId, toTsEntry(TS, new StringDataEntry(STRING_KEY, "old")));
        save(tenantId, deviceId, toTsEntry(TS, new StringDataEntry(STRING_KEY, "new")));

        Optional<TsKvEntry> entryOpt = tsService.findLatest(tenantId, deviceId, STRING_KEY).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        assertThat(entryOpt).isNotNull().isPresent();
        equalsIgnoreVersion(toTsEntry(TS, new StringDataEntry(STRING_KEY, "new")), entryOpt.get());
    }

    public void testFindLatestOpt_givenSaveWithSameTSOverwriteTypeAndValue() throws Exception {
        save(tenantId, deviceId, toTsEntry(TS, new JsonDataEntry("temp", "{\"hello\":\"world\"}")));
        save(tenantId, deviceId, toTsEntry(TS, new BooleanDataEntry("temp", true)));
        save(tenantId, deviceId, toTsEntry(TS, new LongDataEntry("temp", 100L)));
        save(tenantId, deviceId, toTsEntry(TS, new DoubleDataEntry("temp", Math.PI)));
        save(tenantId, deviceId, toTsEntry(TS, new StringDataEntry("temp", "NOOP")));

        Optional<TsKvEntry> entryOpt = tsService.findLatest(tenantId, deviceId, STRING_KEY).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        assertThat(entryOpt).isNotNull().isPresent();
        Assert.assertEquals(toTsEntry(TS, new StringDataEntry("temp", "NOOP")), entryOpt.orElse(null));
    }

    @Test
    public void testFindLatestOpt() throws Exception {
        saveEntries(deviceId, TS - 2);
        saveEntries(deviceId, TS - 1);
        saveEntries(deviceId, TS);

        Optional<TsKvEntry> entryOpt = tsService.findLatest(tenantId, deviceId, STRING_KEY).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        assertThat(entryOpt).isNotNull().isPresent();
        equalsIgnoreVersion(toTsEntry(TS, stringKvEntry), entryOpt.get());
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
        assertThat(tsKvEntry.getValue()).isNull();
        assertThat(tsKvEntry.getTs()).isCloseTo(System.currentTimeMillis(), Offset.offset(TimeUnit.MINUTES.toMillis(1)));
    }

    @Test
    public void testFindLatestOpt_NotFound() throws Exception {
        Optional<TsKvEntry> entryOpt = tsService.findLatest(tenantId, deviceId, STRING_KEY).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        assertThat(entryOpt).isNotNull().isNotPresent();
    }

    @Test
    public void testFindLatestWithoutLatestUpdate() throws Exception {
        saveEntries(deviceId, TS - 2);
        saveEntries(deviceId, TS - 1);
        saveEntriesWithoutLatest(deviceId, TS);

        List<TsKvEntry> entries = tsService.findLatest(tenantId, deviceId, Collections.singleton(STRING_KEY)).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        Assert.assertEquals(1, entries.size());
        equalsIgnoreVersion(toTsEntry(TS - 1, stringKvEntry), entries.get(0));
    }

    @Test
    public void testFindByQueryAscOrder() throws Exception {
        saveEntries(deviceId, TS - 3);
        saveEntries(deviceId, TS - 2);
        saveEntries(deviceId, TS - 1);

        List<ReadTsKvQuery> queries = new ArrayList<>();
        queries.add(new BaseReadTsKvQuery(STRING_KEY, TS - 3, TS, 0, 1000, Aggregation.NONE, "ASC"));

        List<TsKvEntry> entries = tsService.findAll(tenantId, deviceId, queries).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        Assert.assertEquals(3, entries.size());
        Assert.assertEquals(toTsEntry(TS - 3, stringKvEntry), entries.get(0));
        Assert.assertEquals(toTsEntry(TS - 2, stringKvEntry), entries.get(1));
        Assert.assertEquals(toTsEntry(TS - 1, stringKvEntry), entries.get(2));

        EntityView entityView = saveAndCreateEntityView(deviceId, List.of(STRING_KEY));

        entries = tsService.findAll(tenantId, entityView.getId(), queries).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        Assert.assertEquals(3, entries.size());
        Assert.assertEquals(toTsEntry(TS - 3, stringKvEntry), entries.get(0));
        Assert.assertEquals(toTsEntry(TS - 2, stringKvEntry), entries.get(1));
        Assert.assertEquals(toTsEntry(TS - 1, stringKvEntry), entries.get(2));
    }

    @Test
    public void testFindByQuery_whenPeriodEqualsOneMilisecondPeriod() throws Exception {
        saveEntries(deviceId, TS - 1L);
        saveEntries(deviceId, TS);
        saveEntries(deviceId, TS + 1L);

        List<ReadTsKvQuery> queries = List.of(new BaseReadTsKvQuery(LONG_KEY, TS, TS, 1, 1, Aggregation.COUNT, DESC_ORDER));

        List<TsKvEntry> entries = tsService.findAll(tenantId, deviceId, queries).get();
        Assert.assertEquals(1, entries.size());
        Assert.assertEquals(toTsEntry(TS, new LongDataEntry(LONG_KEY, 1L)), entries.get(0));

        EntityView entityView = saveAndCreateEntityView(deviceId, List.of(LONG_KEY));

        entries = tsService.findAll(tenantId, entityView.getId(), queries).get();
        Assert.assertEquals(1, entries.size());
        Assert.assertEquals(toTsEntry(TS, new LongDataEntry(LONG_KEY, 1L)), entries.get(0));
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
        Assert.assertEquals(toTsEntry(TS + 50, new LongDataEntry(LONG_KEY, 10L)), entries.get(0));

        EntityView entityView = saveAndCreateEntityView(deviceId, List.of(LONG_KEY));

        entries = tsService.findAll(tenantId, entityView.getId(), queries).get();
        Assert.assertEquals(1, entries.size());
        Assert.assertEquals(toTsEntry(TS + 50, new LongDataEntry(LONG_KEY, 10L)), entries.get(0));
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
        Assert.assertEquals(toTsEntry(TS + 25000, new LongDataEntry(LONG_KEY, 5L)), entries.get(0));
        Assert.assertEquals(toTsEntry(TS + 75000 - 1, new LongDataEntry(LONG_KEY, 5L)), entries.get(1));

        EntityView entityView = saveAndCreateEntityView(deviceId, List.of(LONG_KEY));

        entries = tsService.findAll(tenantId, entityView.getId(), queries).get();
        Assert.assertEquals(2, entries.size());
        Assert.assertEquals(toTsEntry(TS + 25000, new LongDataEntry(LONG_KEY, 5L)), entries.get(0));
        Assert.assertEquals(toTsEntry(TS + 75000 - 1, new LongDataEntry(LONG_KEY, 5L)), entries.get(1));
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
        Assert.assertEquals(toTsEntry(TS + 25000, new LongDataEntry(LONG_KEY, 5L)), entries.get(0));
        Assert.assertEquals(toTsEntry(TS + 65000, new LongDataEntry(LONG_KEY, 3L)), entries.get(1));

        EntityView entityView = saveAndCreateEntityView(deviceId, List.of(LONG_KEY));

        entries = tsService.findAll(tenantId, entityView.getId(), queries).get();
        Assert.assertEquals(2, entries.size());
        Assert.assertEquals(toTsEntry(TS + 25000, new LongDataEntry(LONG_KEY, 5L)), entries.get(0));
        Assert.assertEquals(toTsEntry(TS + 65000, new LongDataEntry(LONG_KEY, 3L)), entries.get(1));
    }

    @Test
    public void testFindByQuery_whenPeriodHaveTwoIntervalWithEqualsLength_whereNotAllEntriesInRange() throws Exception {
        for (long i = TS - 1L; i <= TS + 100000L + 1L; i += 10000) {
            saveEntries(deviceId, i);
        }

        List<ReadTsKvQuery> queries = List.of(new BaseReadTsKvQuery(LONG_KEY, TS, TS + 99999, 50000, 1, Aggregation.COUNT, DESC_ORDER));

        List<TsKvEntry> entries = tsService.findAll(tenantId, deviceId, queries).get();
        Assert.assertEquals(2, entries.size());
        Assert.assertEquals(toTsEntry(TS + 25000, new LongDataEntry(LONG_KEY, 5L)), entries.get(0));
        Assert.assertEquals(toTsEntry(TS + 75000 - 1, new LongDataEntry(LONG_KEY, 4L)), entries.get(1));

        EntityView entityView = saveAndCreateEntityView(deviceId, List.of(LONG_KEY));

        entries = tsService.findAll(tenantId, entityView.getId(), queries).get();
        Assert.assertEquals(2, entries.size());
        Assert.assertEquals(toTsEntry(TS + 25000, new LongDataEntry(LONG_KEY, 5L)), entries.get(0));
        Assert.assertEquals(toTsEntry(TS + 75000 - 1, new LongDataEntry(LONG_KEY, 4L)), entries.get(1));
    }

    @Test
    public void testFindByQuery_whenPeriodHaveTwoInterval_whereSecondShorterThanFirst_andNotAllEntriesInRange() throws Exception {
        for (long i = TS - 1L; i <= TS + 100000L + 1L; i += 10000L) {
            saveEntries(deviceId, i);
        }

        List<ReadTsKvQuery> queries = List.of(new BaseReadTsKvQuery(LONG_KEY, TS, TS + 80000, 50000, 1, Aggregation.COUNT, DESC_ORDER));

        List<TsKvEntry> entries = tsService.findAll(tenantId, deviceId, queries).get();
        Assert.assertEquals(2, entries.size());
        Assert.assertEquals(toTsEntry(TS + 25000, new LongDataEntry(LONG_KEY, 5L)), entries.get(0));
        Assert.assertEquals(toTsEntry(TS + 65000, new LongDataEntry(LONG_KEY, 3L)), entries.get(1));

        EntityView entityView = saveAndCreateEntityView(deviceId, List.of(LONG_KEY));

        entries = tsService.findAll(tenantId, entityView.getId(), queries).get();
        Assert.assertEquals(2, entries.size());
        Assert.assertEquals(toTsEntry(TS + 25000, new LongDataEntry(LONG_KEY, 5L)), entries.get(0));
        Assert.assertEquals(toTsEntry(TS + 65000, new LongDataEntry(LONG_KEY, 3L)), entries.get(1));
    }

    @Test
    public void testFindByQueryDescOrder() throws Exception {
        saveEntries(deviceId, TS - 3);
        saveEntries(deviceId, TS - 2);
        saveEntries(deviceId, TS - 1);

        List<ReadTsKvQuery> queries = new ArrayList<>();
        queries.add(new BaseReadTsKvQuery(STRING_KEY, TS - 3, TS, 0, 1000, Aggregation.NONE, "DESC"));

        List<TsKvEntry> entries = tsService.findAll(tenantId, deviceId, queries).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        Assert.assertEquals(3, entries.size());
        Assert.assertEquals(toTsEntry(TS - 1, stringKvEntry), entries.get(0));
        Assert.assertEquals(toTsEntry(TS - 2, stringKvEntry), entries.get(1));
        Assert.assertEquals(toTsEntry(TS - 3, stringKvEntry), entries.get(2));

        EntityView entityView = saveAndCreateEntityView(deviceId, List.of(STRING_KEY));

        entries = tsService.findAll(tenantId, entityView.getId(), queries).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        Assert.assertEquals(3, entries.size());
        Assert.assertEquals(toTsEntry(TS - 1, stringKvEntry), entries.get(0));
        Assert.assertEquals(toTsEntry(TS - 2, stringKvEntry), entries.get(1));
        Assert.assertEquals(toTsEntry(TS - 3, stringKvEntry), entries.get(2));
    }

    @Test
    public void testFindAllByQueries_verifyQueryId() throws Exception {
        saveEntries(deviceId, TS);
        saveEntries(deviceId, TS - 2);
        saveEntries(deviceId, TS - 10);

        BaseReadTsKvQuery query = new BaseReadTsKvQuery(STRING_KEY, TS - 10, TS + 1, 0, 1000, Aggregation.NONE, "DESC");
        findAndVerifyQueryId(deviceId, query);
    }

    @Test
    public void testFindAllByQueries_verifyQueryId_forEntityView() throws Exception {
        saveEntries(deviceId, TS);
        saveEntries(deviceId, TS - 2);
        saveEntries(deviceId, TS - 12);

        EntityView entityView = saveAndCreateEntityView(deviceId, List.of(LONG_KEY));

        BaseReadTsKvQuery query = new BaseReadTsKvQuery(LONG_KEY, TS - 10, TS + 1, 0, 1000, Aggregation.NONE, "DESC");
        findAndVerifyQueryId(entityView.getId(), query);
    }

    private void findAndVerifyQueryId(EntityId entityId, ReadTsKvQuery query) throws InterruptedException, ExecutionException, TimeoutException {
        List<ReadTsKvQueryResult> results = tsService.findAllByQueries(tenantId, entityId, List.of(query)).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        assertThat(results).isNotEmpty();
        assertThat(results).extracting(ReadTsKvQueryResult::getQueryId).containsOnly(query.getId());
    }

    @Test
    public void testDeleteDeviceTsDataWithOverwritingLatest() throws Exception {
        saveEntries(deviceId, 10000);
        saveEntries(deviceId, 20000);
        saveEntries(deviceId, 30000);
        saveEntries(deviceId, 40000);

        tsService.remove(tenantId, deviceId, Collections.singletonList(
                new BaseDeleteTsKvQuery(STRING_KEY, 25000, 45000, true))).get(MAX_TIMEOUT, TimeUnit.SECONDS);

        List<TsKvEntry> list = tsService.findAll(tenantId, deviceId, Collections.singletonList(
                new BaseReadTsKvQuery(STRING_KEY, 5000, 45000, 10000, 10, Aggregation.NONE))).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        Assert.assertEquals(2, list.size());

        List<TsKvEntry> latest = tsService.findLatest(tenantId, deviceId, Collections.singletonList(STRING_KEY)).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        Assert.assertEquals(20000, latest.get(0).getTs());
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
        assertEquals(10000, list.get(0).getTs());
        assertEquals(java.util.Optional.of(150.0), list.get(0).getDoubleValue());

        assertEquals(30000, list.get(1).getTs());
        assertEquals(java.util.Optional.of(350.0), list.get(1).getDoubleValue());

        assertEquals(50000, list.get(2).getTs());
        assertEquals(java.util.Optional.of(550.0), list.get(2).getDoubleValue());

        list = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery(LONG_KEY, 0,
                60000, 20000, 3, Aggregation.SUM))).get(MAX_TIMEOUT, TimeUnit.SECONDS);

        assertEquals(3, list.size());
        assertEquals(10000, list.get(0).getTs());
        assertEquals(java.util.Optional.of(300L), list.get(0).getLongValue());

        assertEquals(30000, list.get(1).getTs());
        assertEquals(java.util.Optional.of(700L), list.get(1).getLongValue());

        assertEquals(50000, list.get(2).getTs());
        assertEquals(java.util.Optional.of(1100L), list.get(2).getLongValue());

        list = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery(LONG_KEY, 0,
                60000, 20000, 3, Aggregation.MIN))).get(MAX_TIMEOUT, TimeUnit.SECONDS);

        assertEquals(3, list.size());
        assertEquals(10000, list.get(0).getTs());
        assertEquals(java.util.Optional.of(100L), list.get(0).getLongValue());

        assertEquals(30000, list.get(1).getTs());
        assertEquals(java.util.Optional.of(300L), list.get(1).getLongValue());

        assertEquals(50000, list.get(2).getTs());
        assertEquals(java.util.Optional.of(500L), list.get(2).getLongValue());

        list = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery(LONG_KEY, 0,
                60000, 20000, 3, Aggregation.MAX))).get(MAX_TIMEOUT, TimeUnit.SECONDS);

        assertEquals(3, list.size());
        assertEquals(10000, list.get(0).getTs());
        assertEquals(java.util.Optional.of(200L), list.get(0).getLongValue());

        assertEquals(30000, list.get(1).getTs());
        assertEquals(java.util.Optional.of(400L), list.get(1).getLongValue());

        assertEquals(50000, list.get(2).getTs());
        assertEquals(java.util.Optional.of(600L), list.get(2).getLongValue());

        list = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery(LONG_KEY, 0,
                60000, 20000, 3, Aggregation.COUNT))).get(MAX_TIMEOUT, TimeUnit.SECONDS);

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
                120000, 20000, 3, Aggregation.NONE))).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        assertEquals(3, list.size());
        assertEquals(115000, list.get(0).getTs());
        assertEquals(java.util.Optional.of("C2"), list.get(0).getStrValue());

        assertEquals(105000, list.get(1).getTs());
        assertEquals(java.util.Optional.of("C1"), list.get(1).getStrValue());

        assertEquals(95000, list.get(2).getTs());
        assertEquals(java.util.Optional.of("B2"), list.get(2).getStrValue());


        list = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery(LONG_KEY, 60000,
                120000, 20000, 3, Aggregation.MIN))).get(MAX_TIMEOUT, TimeUnit.SECONDS);

        assertEquals(3, list.size());
        assertEquals(70000, list.get(0).getTs());
        assertEquals(java.util.Optional.of("A1"), list.get(0).getStrValue());

        assertEquals(90000, list.get(1).getTs());
        assertEquals(java.util.Optional.of("B1"), list.get(1).getStrValue());

        assertEquals(110000, list.get(2).getTs());
        assertEquals(java.util.Optional.of("C1"), list.get(2).getStrValue());

        list = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery(LONG_KEY, 60000,
                120000, 20000, 3, Aggregation.MAX))).get(MAX_TIMEOUT, TimeUnit.SECONDS);

        assertEquals(3, list.size());
        assertEquals(70000, list.get(0).getTs());
        assertEquals(java.util.Optional.of("A2"), list.get(0).getStrValue());

        assertEquals(90000, list.get(1).getTs());
        assertEquals(java.util.Optional.of("B2"), list.get(1).getStrValue());

        assertEquals(110000, list.get(2).getTs());
        assertEquals(java.util.Optional.of("C2"), list.get(2).getStrValue());

        list = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery(LONG_KEY, 60000,
                120000, 20000, 3, Aggregation.COUNT))).get(MAX_TIMEOUT, TimeUnit.SECONDS);

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
        List<TsKvEntry> entries = new ArrayList<>();

        entries.add(save(deviceId, 5000, 100));
        entries.add(save(deviceId, 15000, 200.0));

        entries.add(save(deviceId, 25000, 300));
        entries.add(save(deviceId, 35000, 400.0));

        entries.add(save(deviceId, 45000, 500));
        entries.add(save(deviceId, 55000, 600.0));

        List<TsKvEntry> list = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery(LONG_KEY, 0,
                60000, 20000, 3, Aggregation.NONE))).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        assertEquals(3, list.size());
        assertEquals(55000, list.get(0).getTs());
        assertEquals(java.util.Optional.of(600.0), list.get(0).getDoubleValue());

        assertEquals(45000, list.get(1).getTs());
        assertEquals(java.util.Optional.of(500L), list.get(1).getLongValue());

        assertEquals(35000, list.get(2).getTs());
        assertEquals(java.util.Optional.of(400.0), list.get(2).getDoubleValue());

        list = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery(LONG_KEY, 0,
                60000, 20000, 3, Aggregation.AVG))).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        assertEquals(3, list.size());
        assertEquals(10000, list.get(0).getTs());
        assertEquals(java.util.Optional.of(150.0), list.get(0).getDoubleValue());

        assertEquals(30000, list.get(1).getTs());
        assertEquals(java.util.Optional.of(350.0), list.get(1).getDoubleValue());

        assertEquals(50000, list.get(2).getTs());
        assertEquals(java.util.Optional.of(550.0), list.get(2).getDoubleValue());

        list = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery(LONG_KEY, 0,
                60000, 20000, 3, Aggregation.SUM))).get(MAX_TIMEOUT, TimeUnit.SECONDS);

        assertEquals(3, list.size());
        assertEquals(10000, list.get(0).getTs());
        assertEquals(java.util.Optional.of(300.0), list.get(0).getDoubleValue());

        assertEquals(30000, list.get(1).getTs());
        assertEquals(java.util.Optional.of(700.0), list.get(1).getDoubleValue());

        assertEquals(50000, list.get(2).getTs());
        assertEquals(java.util.Optional.of(1100.0), list.get(2).getDoubleValue());

        list = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery(LONG_KEY, 0,
                60000, 20000, 3, Aggregation.MIN))).get(MAX_TIMEOUT, TimeUnit.SECONDS);

        assertEquals(3, list.size());
        assertEquals(10000, list.get(0).getTs());
        assertEquals(java.util.Optional.of(100.0), list.get(0).getDoubleValue());

        assertEquals(30000, list.get(1).getTs());
        assertEquals(java.util.Optional.of(300.0), list.get(1).getDoubleValue());

        assertEquals(50000, list.get(2).getTs());
        assertEquals(java.util.Optional.of(500.0), list.get(2).getDoubleValue());

        list = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery(LONG_KEY, 0,
                60000, 20000, 3, Aggregation.MAX))).get(MAX_TIMEOUT, TimeUnit.SECONDS);

        assertEquals(3, list.size());
        assertEquals(10000, list.get(0).getTs());
        assertEquals(java.util.Optional.of(200.0), list.get(0).getDoubleValue());

        assertEquals(30000, list.get(1).getTs());
        assertEquals(java.util.Optional.of(400.0), list.get(1).getDoubleValue());

        assertEquals(50000, list.get(2).getTs());
        assertEquals(java.util.Optional.of(600.0), list.get(2).getDoubleValue());

        list = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery(LONG_KEY, 0,
                60000, 20000, 3, Aggregation.COUNT))).get(MAX_TIMEOUT, TimeUnit.SECONDS);

        assertEquals(3, list.size());
        assertEquals(10000, list.get(0).getTs());
        assertEquals(java.util.Optional.of(2L), list.get(0).getLongValue());

        assertEquals(30000, list.get(1).getTs());
        assertEquals(java.util.Optional.of(2L), list.get(1).getLongValue());

        assertEquals(50000, list.get(2).getTs());
        assertEquals(java.util.Optional.of(2L), list.get(2).getLongValue());
    }

    @Test
    public void testSaveTs_RemoveTs_AndSaveTsAgain() throws Exception {
        save(deviceId, 2000000L, 95);
        save(deviceId, 4000000L, 100);
        save(deviceId, 6000000L, 105);
        List<TsKvEntry> list = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery(LONG_KEY, 0L,
                8000000L, 200000, 3, Aggregation.NONE))).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        assertEquals(3, list.size());

        tsService.remove(tenantId, deviceId, Collections.singletonList(
                new BaseDeleteTsKvQuery(LONG_KEY, 0L, 8000000L, false))).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        list = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery(LONG_KEY, 0L,
                8000000L, 200000, 3, Aggregation.NONE))).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        assertEquals(0, list.size());

        save(deviceId, 2000000L, 99);
        save(deviceId, 4000000L, 104);
        save(deviceId, 6000000L, 109);
        list = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery(LONG_KEY, 0L,
                8000000L, 200000, 3, Aggregation.NONE))).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        assertEquals(3, list.size());
    }

    @Test
    public void shouldSaveEntryOfEachType() throws Exception {
        BasicTsKvEntry booleanEntry = new BasicTsKvEntry(TimeUnit.MINUTES.toMillis(1), new BooleanDataEntry("test", true));
        BasicTsKvEntry stringEntry = new BasicTsKvEntry(TimeUnit.MINUTES.toMillis(2), new StringDataEntry("test", "text"));
        BasicTsKvEntry longEntry = new BasicTsKvEntry(TimeUnit.MINUTES.toMillis(3), new LongDataEntry("test", 15L));
        BasicTsKvEntry doubleEntry = new BasicTsKvEntry(TimeUnit.MINUTES.toMillis(4), new DoubleDataEntry("test", 10.5));
        BasicTsKvEntry jsonEntry = new BasicTsKvEntry(TimeUnit.MINUTES.toMillis(5), new JsonDataEntry("test", "{\"test\":\"testValue\"}"));
        List<TsKvEntry> timeseries = List.of(booleanEntry, stringEntry, longEntry, doubleEntry, jsonEntry);

        for (TsKvEntry tsKvEntry : timeseries) {
            save(tenantId, deviceId, tsKvEntry);
        }

        List<TsKvEntry> listUntil3Minutes = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery("test", 0L,
                TimeUnit.MINUTES.toMillis(3), 1000, 10, Aggregation.NONE))).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        assertEquals(2, listUntil3Minutes.size());
        assertThat(listUntil3Minutes).containsOnlyOnceElementsOf(List.of(
                booleanEntry, stringEntry));

        List<TsKvEntry> fullList = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery("test", 0L,
                TimeUnit.MINUTES.toMillis(6), 1000, 10, Aggregation.NONE))).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        assertEquals(5, fullList.size());
        assertThat(fullList).containsOnlyOnceElementsOf(timeseries);
    }

    @Test
    public void testFindAllByQueriesWithAggregationAndZeroInterval() throws Exception {
        testFindAllByQueriesWithAggregationAndInvalidInterval(0);
    }

    @Test
    public void testFindAllByQueriesWithAggregationAndNegativeInterval() throws Exception {
        testFindAllByQueriesWithAggregationAndInvalidInterval(-1);
    }

    private void testFindAllByQueriesWithAggregationAndInvalidInterval(long interval) {
        BaseReadTsKvQuery query = new BaseReadTsKvQuery(STRING_KEY, TS, TS, interval, 1000, Aggregation.SUM, "DESC");
        Assert.assertThrows(IncorrectParameterException.class, () -> findAndVerifyQueryId(deviceId, query));
    }

    private TsKvEntry save(DeviceId deviceId, long ts, long value) throws Exception {
        TsKvEntry entry = new BasicTsKvEntry(ts, new LongDataEntry(LONG_KEY, value));
        tsService.save(tenantId, deviceId, entry).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        return entry;
    }

    private TsKvEntry save(DeviceId deviceId, long ts, double value) throws Exception {
        TsKvEntry entry = new BasicTsKvEntry(ts, new DoubleDataEntry(LONG_KEY, value));
        tsService.save(tenantId, deviceId, entry).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        return entry;
    }

    private TsKvEntry save(DeviceId deviceId, long ts, String value) throws Exception {
        TsKvEntry entry = new BasicTsKvEntry(ts, new StringDataEntry(LONG_KEY, value));
        tsService.save(tenantId, deviceId, entry).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        return entry;
    }

    private void save(TenantId tenantId, DeviceId deviceId, TsKvEntry tsKvEntry) throws Exception {
        tsService.save(tenantId, deviceId, tsKvEntry).get(MAX_TIMEOUT, TimeUnit.SECONDS);
    }

    private void saveEntries(DeviceId deviceId, long ts) throws ExecutionException, InterruptedException, TimeoutException {
        tsService.save(tenantId, deviceId, toTsEntry(ts, stringKvEntry)).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        tsService.save(tenantId, deviceId, toTsEntry(ts, longKvEntry)).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        tsService.save(tenantId, deviceId, toTsEntry(ts, doubleKvEntry)).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        tsService.save(tenantId, deviceId, toTsEntry(ts, booleanKvEntry)).get(MAX_TIMEOUT, TimeUnit.SECONDS);
    }

    private void saveEntriesWithoutLatest(DeviceId deviceId, long ts) throws ExecutionException, InterruptedException, TimeoutException {
        List<TsKvEntry> tsKvEntry = List.of(
                toTsEntry(ts, stringKvEntry),
                toTsEntry(ts, longKvEntry),
                toTsEntry(ts, doubleKvEntry),
                toTsEntry(ts, booleanKvEntry));
        tsService.saveWithoutLatest(tenantId, deviceId, tsKvEntry, 0).get(MAX_TIMEOUT, TimeUnit.SECONDS);
    }

    protected static TsKvEntry toTsEntry(long ts, KvEntry entry) {
        return new BasicTsKvEntry(ts, entry);
    }

    protected static void equalsIgnoreVersion(TsKvEntry expected, TsKvEntry actual) {
        assertEquals(expected.getKey(), actual.getKey());
        assertEquals(expected.getValue(), actual.getValue());
        assertEquals(expected.getTs(), actual.getTs());
    }

}
