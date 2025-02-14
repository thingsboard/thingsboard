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
package org.thingsboard.server.edqs.load;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.edqs.AttributeKv;
import org.thingsboard.server.common.data.edqs.LatestTsKv;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DataType;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.edqs.processor.EdqsConverter;
import org.thingsboard.server.edqs.repo.TenantRepo;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@RequiredArgsConstructor
public class TenantRepoLoader {

    private static final int DEVICE_COUNT = 100000;
    private static final int ATTRS_PER_DEVICE = 30;
    private static final int ATTRS_AVG_STR_LENGTH = 12;
    private static final int ATTRS_AVG_JSON_LENGTH = 265;
    private static final int TS_PER_DEVICE = 29;
    private static final int TS_AVG_STR_LENGTH = 59;
    private static final int TS_AVG_JSON_LENGTH = 4005;

    private static final Map<DataType, Integer> ATTR_CHANCES = new HashMap<>();
    private static final Random random = new Random();

    static {
        ATTR_CHANCES.put(DataType.BOOLEAN, 5);
        ATTR_CHANCES.put(DataType.STRING, 49);
        ATTR_CHANCES.put(DataType.LONG, 34);
        ATTR_CHANCES.put(DataType.DOUBLE, 2);
        ATTR_CHANCES.put(DataType.JSON, 10);
    }

    private static final Map<DataType, Integer> TS_CHANCES = new HashMap<>();

    static {
        TS_CHANCES.put(DataType.BOOLEAN, 6);
        TS_CHANCES.put(DataType.STRING, 19);
        TS_CHANCES.put(DataType.LONG, 36);
        TS_CHANCES.put(DataType.DOUBLE, 32);
        TS_CHANCES.put(DataType.JSON, 7);
    }


    @Getter
    private final TenantRepo tenantRepo;

    public void load() {
        long ts = System.currentTimeMillis() - DEVICE_COUNT;
        for (int i = 0; i < DEVICE_COUNT; i++) {
            DeviceId deviceId = new DeviceId(UUID.randomUUID());
            Device device = new Device();
            device.setId(deviceId);
            device.setCreatedTime(ts + i);
            device.setName("Device " + i);
            device.setLabel("Device Label" + i);
            device.setType("Device Type " + (i % 100));
            tenantRepo.addOrUpdate(EdqsConverter.toEntity(EntityType.DEVICE, device));
            for (int j = 0; j < ATTRS_PER_DEVICE; j++) {
                String key = getRandomKey();
                AttributeKv attributeKv = new AttributeKv();
                attributeKv.setEntityId(deviceId);
                attributeKv.setScope(AttributeScope.SERVER_SCOPE);
                attributeKv.setKey(key);
                attributeKv.setLastUpdateTs(ts);
                attributeKv.setValue(getRandomKvEntry(key, ATTR_CHANCES, ATTRS_AVG_STR_LENGTH, ATTRS_AVG_JSON_LENGTH));
                tenantRepo.addOrUpdateAttribute(attributeKv);
            }
            for (int j = 0; j < TS_PER_DEVICE; j++) {
                String key = getRandomKey();
                LatestTsKv latestTsKv = new LatestTsKv();
                latestTsKv.setEntityId(deviceId);
                latestTsKv.setKey(key);
                latestTsKv.setTs(ts);
                latestTsKv.setValue(getRandomKvEntry(key, TS_CHANCES, TS_AVG_STR_LENGTH, TS_AVG_JSON_LENGTH));
                tenantRepo.addOrUpdateLatestKv(latestTsKv);
            }
        }
    }

    private KvEntry getRandomKvEntry(String key, Map<DataType, Integer> chances, int strLength, int jsnLength) {
        int i = random.nextInt(100);
        int s = 0;
        for (var pair : chances.entrySet()) {
            s += pair.getValue();
            if (i < s) {
                switch (pair.getKey()) {
                    case BOOLEAN -> {
                        return new BooleanDataEntry(key, random.nextBoolean());
                    }
                    case LONG -> {
                        return new LongDataEntry(key, random.nextLong());
                    }
                    case DOUBLE -> {
                        return new DoubleDataEntry(key, random.nextDouble());
                    }
                    case STRING -> {
                        return new StringDataEntry(key, StringUtils.randomAlphanumeric(strLength));
                    }
                    case JSON -> {
                        return new JsonDataEntry(key, StringUtils.randomAlphanumeric(jsnLength));
                    }
                }
            }
        }
        throw new RuntimeException("Something went wrong");
    }

    private String getRandomKey() {
        return RandomStringUtils.randomAlphabetic(10);
    }

}
