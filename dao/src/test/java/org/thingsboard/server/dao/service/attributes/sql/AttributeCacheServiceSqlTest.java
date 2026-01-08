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
package org.thingsboard.server.dao.service.attributes.sql;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.cache.TbCacheValueWrapper;
import org.thingsboard.server.cache.VersionedTbCache;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.dao.attributes.AttributeCacheKey;
import org.thingsboard.server.dao.service.AbstractServiceTest;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@DaoSqlTest
public class AttributeCacheServiceSqlTest extends AbstractServiceTest {

    private static final String TEST_KEY = "key";
    private static final String TEST_VALUE = "value";
    private static final DeviceId DEVICE_ID = new DeviceId(UUID.randomUUID());

    @Autowired
    VersionedTbCache<AttributeCacheKey, AttributeKvEntry> cache;

    @Test
    public void testPutAndGet() {
        AttributeCacheKey testKey = new AttributeCacheKey(AttributeScope.CLIENT_SCOPE, DEVICE_ID, TEST_KEY);
        AttributeKvEntry testValue = new BaseAttributeKvEntry(new StringDataEntry(TEST_KEY, TEST_VALUE), 1, 1L);
        cache.put(testKey, testValue);

        TbCacheValueWrapper<AttributeKvEntry> wrapper = cache.get(testKey);
        assertNotNull(wrapper);

        assertEquals(testValue, wrapper.get());

        AttributeKvEntry testValue2 = new BaseAttributeKvEntry(new StringDataEntry(TEST_KEY, TEST_VALUE), 1, 2L);
        cache.put(testKey, testValue2);

        wrapper = cache.get(testKey);
        assertNotNull(wrapper);

        assertEquals(testValue2, wrapper.get());

        AttributeKvEntry testValue3 = new BaseAttributeKvEntry(new StringDataEntry(TEST_KEY, TEST_VALUE), 1, 0L);
        cache.put(testKey, testValue3);

        wrapper = cache.get(testKey);
        assertNotNull(wrapper);

        assertEquals(testValue2, wrapper.get());

        cache.evict(testKey);
    }

    @Test
    public void testEvictWithVersion() {
        AttributeCacheKey testKey = new AttributeCacheKey(AttributeScope.CLIENT_SCOPE, DEVICE_ID, TEST_KEY);
        AttributeKvEntry testValue = new BaseAttributeKvEntry(new StringDataEntry(TEST_KEY, TEST_VALUE), 1, 1L);
        cache.put(testKey, testValue);

        TbCacheValueWrapper<AttributeKvEntry> wrapper = cache.get(testKey);
        assertNotNull(wrapper);

        assertEquals(testValue, wrapper.get());

        cache.evict(testKey, 2L);

        wrapper = cache.get(testKey);
        assertNotNull(wrapper);

        assertNull(wrapper.get());

        cache.evict(testKey);
    }

    @Test
    public void testEvict() {
        AttributeCacheKey testKey = new AttributeCacheKey(AttributeScope.CLIENT_SCOPE, DEVICE_ID, TEST_KEY);
        AttributeKvEntry testValue = new BaseAttributeKvEntry(new StringDataEntry(TEST_KEY, TEST_VALUE), 1, 1L);
        cache.put(testKey, testValue);

        TbCacheValueWrapper<AttributeKvEntry> wrapper = cache.get(testKey);
        assertNotNull(wrapper);

        assertEquals(testValue, wrapper.get());

        cache.evict(testKey);

        wrapper = cache.get(testKey);
        assertNull(wrapper);
    }
}
