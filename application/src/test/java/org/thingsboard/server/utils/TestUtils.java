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
package org.thingsboard.server.utils;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.device.data.DefaultDeviceConfiguration;
import org.thingsboard.server.common.data.device.data.DefaultDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.data.DeviceConfiguration;
import org.thingsboard.server.common.data.device.data.DeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.AllowCreateNewDevicesDeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileConfiguration;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileAlarm;
import org.thingsboard.server.common.data.device.profile.DeviceProfileConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;

import java.nio.ByteBuffer;
import java.util.UUID;

public class TestUtils {

    static EasyRandom easyRandom;

    static {
        EasyRandomParameters parameters = new EasyRandomParameters()
                .randomize(DeviceConfiguration.class, () -> easyRandom.nextObject(DefaultDeviceConfiguration.class))
                .randomize(DeviceTransportConfiguration.class, () -> easyRandom.nextObject(DefaultDeviceTransportConfiguration.class))
                .randomize(DeviceProfileConfiguration.class, () -> easyRandom.nextObject(DefaultDeviceProfileConfiguration.class))
                .randomize(DeviceProfileTransportConfiguration.class, () -> easyRandom.nextObject(DefaultDeviceProfileTransportConfiguration.class))
                .randomize(DeviceProfileProvisionConfiguration.class, () -> easyRandom.nextObject(AllowCreateNewDevicesDeviceProfileProvisionConfiguration.class))
                .randomize(DeviceProfileAlarm.class, DeviceProfileAlarm::new)
                .randomize(JsonNode.class, () -> JacksonUtil.newObjectNode()
                        .put(RandomStringUtils.randomAlphanumeric(10), RandomStringUtils.randomAlphanumeric(10))
                        .put(RandomStringUtils.randomAlphanumeric(10), easyRandom.nextDouble()))
                .randomize(EntityId.class, () -> new DeviceId(UUID.randomUUID()))
                .randomize(KvEntry.class, () -> easyRandom.nextBoolean() ? easyRandom.nextObject(StringDataEntry.class) : easyRandom.nextObject(DoubleDataEntry.class))
                .randomize(Long.class, () -> RandomUtils.nextLong(1, Integer.MAX_VALUE))
                .randomize(ByteBuffer.class, () -> {
                    byte[] bytes = new byte[50];
                    easyRandom.nextBytes(bytes);
                    return ByteBuffer.wrap(bytes);
                });

        easyRandom = new EasyRandom(parameters);
    }

    public static <T> T newRandomizedEntity(Class<T> type) {
        return easyRandom.nextObject(type);
    }

}
