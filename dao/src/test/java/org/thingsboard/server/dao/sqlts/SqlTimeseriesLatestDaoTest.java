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
package org.thingsboard.server.dao.sqlts;

import com.google.common.util.concurrent.ListenableFuture;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.dao.service.AbstractServiceTest;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.timeseries.TimeseriesLatestDao;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DaoSqlTest
public class SqlTimeseriesLatestDaoTest extends AbstractServiceTest {

    @Autowired
    private TimeseriesLatestDao timeseriesLatestDao;

    @Test
    public void saveLatestTest() throws Exception {
        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        var entry = createEntry("key", 1000);
        Long version = timeseriesLatestDao.saveLatest(tenantId, deviceId, entry).get();
        assertNotNull(version);
        assertTrue(version > 0);

        TsKvEntry foundEntry = timeseriesLatestDao.findLatest(tenantId, deviceId, "key").get();
        assertNotNull(foundEntry);
        equalsIgnoreVersion(entry, foundEntry);
        assertEquals(version, foundEntry.getVersion());

        var updatedEntry = createEntry("key", 2000);
        Long updatedVersion = timeseriesLatestDao.saveLatest(tenantId, deviceId, updatedEntry).get();
        assertNotNull(updatedVersion);
        assertTrue(updatedVersion > version);

        foundEntry = timeseriesLatestDao.findLatest(tenantId, deviceId, "key").get();
        assertNotNull(foundEntry);
        equalsIgnoreVersion(updatedEntry, foundEntry);
        assertEquals(updatedVersion, foundEntry.getVersion());

        var oldEntry = createEntry("key", 1);
        Long oldVersion = timeseriesLatestDao.saveLatest(tenantId, deviceId, oldEntry).get();
        assertNull(oldVersion);

        foundEntry = timeseriesLatestDao.findLatest(tenantId, deviceId, "key").get();
        assertNotNull(foundEntry);
        equalsIgnoreVersion(updatedEntry, foundEntry);
        assertEquals(updatedVersion, foundEntry.getVersion());
    }

    @Test
    public void updateWithOldTsTest() throws Exception {
        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        int n = 50;
        for (int i = 0; i < n; i++) {
            timeseriesLatestDao.saveLatest(tenantId, deviceId, createEntry("key_" + i, System.currentTimeMillis()));
        }

        List<ListenableFuture<Long>> futures = new ArrayList<>(n);

        for (int i = 0; i < n; i++) {
            long ts = i % 2 == 0 ? System.currentTimeMillis() : 1000;
            futures.add(timeseriesLatestDao.saveLatest(tenantId, deviceId, createEntry("key_" + i, ts)));
        }

        for (int i = 0; i < futures.size(); i++) {
            Long version = futures.get(i).get();
            if (i % 2 == 0) {
                assertNotNull(version);
                assertTrue(version > 0);
            } else {
                assertNull(version);
            }
        }
    }

    private TsKvEntry createEntry(String key, long ts) {
        return new BasicTsKvEntry(ts, new StringDataEntry(key, RandomStringUtils.random(10)));
    }

    private void equalsIgnoreVersion(TsKvEntry expected, TsKvEntry actual) {
        Assert.assertEquals(expected.getKey(), actual.getKey());
        Assert.assertEquals(expected.getValue(), actual.getValue());
        Assert.assertEquals(expected.getTs(), actual.getTs());
    }

}
