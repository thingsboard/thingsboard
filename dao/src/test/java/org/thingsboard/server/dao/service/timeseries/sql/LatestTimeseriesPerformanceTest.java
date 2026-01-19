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
package org.thingsboard.server.dao.service.timeseries.sql;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.dao.service.AbstractServiceTest;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.timeseries.TimeseriesLatestDao;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@DaoSqlTest
@Slf4j
public class LatestTimeseriesPerformanceTest extends AbstractServiceTest {

    private static final String STRING_KEY = "stringKey";
    private static final String LONG_KEY = "longKey";
    private static final String DOUBLE_KEY = "doubleKey";
    private static final String BOOLEAN_KEY = "booleanKey";
    private static final int AMOUNT_OF_UNIQ_KEY = 10000;
    private static final int TIMEOUT = 100;

    private final Random random = new Random();

    @Autowired
    private TimeseriesLatestDao timeseriesLatestDao;

    private ListeningExecutorService testExecutor;

    private EntityId entityId;

    private AtomicLong saveCounter;

    @Before
    public void before() {
        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        Tenant savedTenant = tenantService.saveTenant(tenant);
        Assert.assertNotNull(savedTenant);
        tenantId = savedTenant.getId();
        entityId = new DeviceId(UUID.randomUUID());
        saveCounter = new AtomicLong(0);
        testExecutor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(200, ThingsBoardThreadFactory.forName(getClass().getSimpleName() + "-test-scope")));
    }

    @After
    public void after() {
        tenantService.deleteTenant(tenantId);
        if (testExecutor != null) {
            testExecutor.shutdownNow();
        }
    }

    @Test
    public void test_save_latest_timeseries() throws Exception {
        warmup();
        saveCounter.set(0);

        long startTime = System.currentTimeMillis();
        List<ListenableFuture<?>> futures = new ArrayList<>();
        for (int i = 0; i < 25_000; i++) {
            futures.add(save(generateStrEntry(getRandomKey())));
            futures.add(save(generateLngEntry(getRandomKey())));
            futures.add(save(generateDblEntry(getRandomKey())));
            futures.add(save(generateBoolEntry(getRandomKey())));
        }
        Futures.allAsList(futures).get(TIMEOUT, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();

        long totalTime = endTime - startTime;

        log.info("Total time: {}", totalTime);
        log.info("Saved count: {}", saveCounter.get());
        log.warn("Saved per 1 sec: {}", saveCounter.get() * 1000 / totalTime);
    }

    private void warmup() throws Exception {
        List<ListenableFuture<?>> futures = new ArrayList<>();
        for (int i = 0; i < AMOUNT_OF_UNIQ_KEY; i++) {
            futures.add(save(generateStrEntry(i)));
            futures.add(save(generateLngEntry(i)));
            futures.add(save(generateDblEntry(i)));
            futures.add(save(generateBoolEntry(i)));
        }
        Futures.allAsList(futures).get(TIMEOUT, TimeUnit.SECONDS);
    }

    private ListenableFuture<?> save(TsKvEntry tsKvEntry) {
        return Futures.transformAsync(testExecutor.submit(() -> timeseriesLatestDao.saveLatest(tenantId, entityId, tsKvEntry)), result -> {
            saveCounter.incrementAndGet();
            return result;
        }, testExecutor);
    }

    private TsKvEntry generateStrEntry(int keyIndex) {
        return new BasicTsKvEntry(System.currentTimeMillis(), new StringDataEntry(STRING_KEY + keyIndex, RandomStringUtils.random(10)));
    }

    private TsKvEntry generateLngEntry(int keyIndex) {
        return new BasicTsKvEntry(System.currentTimeMillis(), new LongDataEntry(LONG_KEY + keyIndex, random.nextLong()));
    }

    private TsKvEntry generateDblEntry(int keyIndex) {
        return new BasicTsKvEntry(System.currentTimeMillis(), new DoubleDataEntry(DOUBLE_KEY + keyIndex, random.nextDouble()));
    }

    private TsKvEntry generateBoolEntry(int keyIndex) {
        return new BasicTsKvEntry(System.currentTimeMillis(), new BooleanDataEntry(BOOLEAN_KEY + keyIndex, random.nextBoolean()));
    }

    private int getRandomKey() {
        return random.nextInt(AMOUNT_OF_UNIQ_KEY);
    }

}
