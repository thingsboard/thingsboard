/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.common.mapping;

import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.device.data.DefaultDeviceConfiguration;
import org.thingsboard.server.common.data.device.data.DefaultDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.data.DeviceData;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.data.DeviceProto;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.function.Function;

public class DataSerializationTest {


    private static final int _1M = 1000 * 1000;

    private static Device d;

    @BeforeAll
    public static void before() {
        d = new Device();
        d.setId(new DeviceId(UUID.randomUUID()));
        d.setCreatedTime(System.currentTimeMillis());
        d.setTenantId(new TenantId(UUID.randomUUID()));
        d.setDeviceProfileId(new DeviceProfileId(UUID.randomUUID()));
        d.setCustomerId(new CustomerId(UUID.randomUUID()));
        d.setName(StringUtils.randomAlphabetic(10));
        d.setLabel(StringUtils.randomAlphabetic(10));
        d.setType(StringUtils.randomAlphabetic(10));
        var dd = new DeviceData();
        dd.setConfiguration(new DefaultDeviceConfiguration());
        dd.setTransportConfiguration(new DefaultDeviceTransportConfiguration());
        d.setDeviceData(dd);
        d.setAdditionalInfo(JacksonUtil.newObjectNode().put("smth", StringUtils.randomAlphabetic(100)));
        d.setFirmwareId(new OtaPackageId(UUID.randomUUID()));
        //d.setSoftwareId(new OtaPackageId(UUID.randomUUID())); - check some fields are null;
        d.setExternalId(new DeviceId(UUID.randomUUID()));
    }

    @Test
    void testDeviceSerialization() {
        Assert.assertEquals(d, fromMapStruct(toMapStruct(d)));
    }

    @Test
    void testDeviceSerialization1M() {
        testDeviceSerialization1M("Proto", this::toMapStruct);
        testDeviceSerialization1M("Jackson", this::serializeJackson);
        testDeserialize("Proto", toMapStruct(d), this::fromMapStruct);
        testDeserialize("Jackson", serializeJackson(d), data -> JacksonUtil.fromBytes(data, Device.class));
    }

    private byte[] toMapStruct(Device device) {
        return ToProtoMapper.INSTANCE.mapDevice(device).toByteArray();
    }

    @SneakyThrows
    private Device fromMapStruct(byte[] data) {
        return ToDataMapper.INSTANCE.mapDevice(DeviceProto.parseFrom(data));
    }

    private void testDeviceSerialization1M(String methodName, Function<Device, byte[]> serializeFunction) {
        // WARMUP;
        for (int i = 0; i < _1M; i++) {
            d.setName(Integer.toString(i));
            serializeFunction.apply(d);
        }
        long startTs = System.nanoTime();
        for (int i = 0; i < _1M; i++) {
            d.setName(Integer.toString(i));
            serializeFunction.apply(d);
        }
        long elapsedTime = System.nanoTime() - startTs;
        System.out.println("S: " + methodName + " SIZE: " + serializeFunction.apply(d).length + " TIME: " + (elapsedTime / 1000000) + "ms");
    }

    private void testDeserialize(String methodName, byte[] data, Function<byte[], Device> deserializeFunction) {
        // WARMUP;
        for (int i = 0; i < 1000; i++) {
            d.setName(Integer.toString(i));
            deserializeFunction.apply(data);
        }
        long startTs = System.nanoTime();
        for (int i = 0; i < _1M; i++) {
            d.setName(Integer.toString(i));
            deserializeFunction.apply(data);
        }
        long elapsedTime = System.nanoTime() - startTs;
        System.out.println("D: " + methodName + " TIME: " + (elapsedTime / 1000000) + "ms");
    }

    <T> byte[] serializeJackson(T object) {
        return JacksonUtil.toString(object).getBytes(StandardCharsets.UTF_8);
    }

}
