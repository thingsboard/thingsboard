package org.thingsboard.server.queue.util;

import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.FSTUtils;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.gen.data.DeviceProto;
import org.thingsboard.server.gen.data.DeviceProto2;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;


public class DataDecodingEncodingServiceTest {


    private static final int _1M = 1000 * 1000;

    private static Device d;

    @BeforeAll
    public static void before() {
        d = new Device();
        d.setId(new DeviceId(UUID.randomUUID()));
        d.setCreatedTime(System.currentTimeMillis());
        d.setTenantId(new TenantId(UUID.randomUUID()));
        d.setCustomerId(new CustomerId(UUID.randomUUID()));
        d.setName(StringUtils.randomAlphabetic(10));
        d.setLabel(StringUtils.randomAlphabetic(10));
        d.setType(StringUtils.randomAlphabetic(10));
//        d.setAdditionalInfo(JacksonUtil.newObjectNode().put("smth", StringUtils.randomAlphabetic(100)));
        d.setFirmwareId(new OtaPackageId(UUID.randomUUID()));
        d.setSoftwareId(new OtaPackageId(UUID.randomUUID()));
        d.setExternalId(new DeviceId(UUID.randomUUID()));
    }

    @Ignore
    @Test
    void testSerialize() {

        Assert.assertArrayEquals(toProto(d), toMapStruct(d));
        // warmup
        testSerialize("FST", FSTUtils::encode);
        testSerialize("Proto", this::toProto);
        testSerialize("Proto2", this::toProto2);
        testSerialize("MapStruct", this::toMapStruct);
        testSerialize("Jackson", DataDecodingEncodingServiceTest::serializeJackson);
//        testSerialize("Default", SerializationUtils::serialize);

        testDeserialize("FST", FSTUtils.encode(d), FSTUtils::decode);
        testDeserialize("Proto", toProto(d), this::fromProto);
        testDeserialize("Jackson", serializeJackson(d), data -> JacksonUtil.fromBytes(data, Device.class));
//        testDeserialize("Default", serializeDefault(d), data -> (Device) SerializationUtils.deserialize(data));
    }

    private byte[] toMapStruct(Device device) {
        return DeviceMapper.INSTANCE.map(device).toByteArray();
    }

    private byte[] toProto(Device device) {
        var builder = DeviceProto.newBuilder();
        builder.setId(device.getId().getId().toString());
        builder.setCreatedTime(device.getCreatedTime());
        builder.setTenantId(device.getTenantId().getId().toString());
        toProto(device::getCustomerId, builder::setCustomerId);
        builder.setName(device.getName());
        builder.setLabel(device.getLabel());
        builder.setType(device.getType());
        toProto(device::getFirmwareId, builder::setFirmwareId);
        toProto(device::getSoftwareId, builder::setSoftwareId);
        toProto(device::getExternalId, builder::setExternalId);
        return builder.build().toByteArray();
    }

    private byte[] toProto2(Device device) {
        var builder = DeviceProto2.newBuilder();
        builder.setIdM(device.getId().getId().getMostSignificantBits());
        builder.setIdL(device.getId().getId().getLeastSignificantBits());
        builder.setCreatedTime(device.getCreatedTime());
        builder.setTenantIdM(device.getTenantId().getId().getMostSignificantBits());
        builder.setTenantIdL(device.getTenantId().getId().getLeastSignificantBits());
        toProto(device::getCustomerId, builder::setCustomerIdM, builder::setCustomerIdL);
        builder.setName(device.getName());
        builder.setLabel(device.getLabel());
        builder.setType(device.getType());
        toProto(device::getFirmwareId, builder::setFirmwareIdM, builder::setFirmwareIdL);
        toProto(device::getSoftwareId, builder::setSoftwareIdM, builder::setSoftwareIdL);
        toProto(device::getExternalId, builder::setExternalIdM, builder::setExternalIdL);
        return builder.build().toByteArray();
    }

    @SneakyThrows
    private Device fromProto(byte[] bytes) {
        DeviceProto dp = DeviceProto.parseFrom(bytes);
        Device d = new Device();
        d.setId(new DeviceId(UUID.fromString(dp.getId())));
        d.setCreatedTime(dp.getCreatedTime());
        d.setTenantId(new TenantId(UUID.fromString(dp.getTenantId())));
        if (dp.hasCustomerId()) {
            d.setCustomerId(new CustomerId(UUID.fromString(dp.getCustomerId())));
        }
        if (dp.hasName()) {
            d.setName(dp.getName());
        }
        if (dp.hasLabel()) {
            d.setLabel(dp.getLabel());
        }
        if (dp.hasType()) {
            d.setType(dp.getType());
        }
        if (dp.hasFirmwareId()) {
            d.setFirmwareId(new OtaPackageId(UUID.fromString(dp.getFirmwareId())));
        }
        if (dp.hasSoftwareId()) {
            d.setSoftwareId(new OtaPackageId(UUID.fromString(dp.getSoftwareId())));
        }
        if (dp.hasExternalId()) {
            d.setExternalId(new DeviceId(UUID.fromString(dp.getExternalId())));
        }
        return d;
    }


    private void toProto(Supplier<UUIDBased> extractId, Consumer<String> setId) {
        UUIDBased id = extractId.get();
        if (id != null) {
            setId.accept(id.getId().toString());
        }
    }

    private void toProto(Supplier<UUIDBased> extractId, Consumer<Long> setIdM, Consumer<Long> setIdL) {
        UUIDBased id = extractId.get();
        if (id != null) {
            var uuid = id.getId();
            setIdM.accept(uuid.getMostSignificantBits());
            setIdL.accept(uuid.getLeastSignificantBits());
        }
    }

    private void testSerialize(String methodName, Function<Device, byte[]> serializeFunction) {
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

    static <T> byte[] serializeDefault(T object) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(object);
            return bos.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }

    static <T> byte[] serializeJackson(T object) {
        return JacksonUtil.toString(object).getBytes(StandardCharsets.UTF_8);
    }

}
