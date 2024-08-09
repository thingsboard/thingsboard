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
package org.thingsboard.server.transport.coap.efento;

import com.google.protobuf.ByteString;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.gen.transport.coap.MeasurementTypeProtos;
import org.thingsboard.server.gen.transport.coap.MeasurementsProtos;
import org.thingsboard.server.transport.coap.CoapTransportContext;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class CoapEfentTransportResourceTest {

    private static CoapEfentoTransportResource coapEfentoTransportResource;

    @BeforeAll
    static void setUp() {
        var ctxMock = mock(CoapTransportContext.class);
        coapEfentoTransportResource = new CoapEfentoTransportResource(ctxMock, "testName");
    }

    @Test
    void checkContinuousSensor() {
        long tsInSec = Instant.now().getEpochSecond();
        MeasurementsProtos.ProtoMeasurements measurements = MeasurementsProtos.ProtoMeasurements.newBuilder()
                .setSerialNum(integerToByteString(1234))
                .setCloudToken("test_token")
                .setMeasurementPeriodBase(180)
                .setMeasurementPeriodFactor(1)
                .setBatteryStatus(true)
                .setSignal(0)
                .setNextTransmissionAt(1000)
                .setTransferReason(0)
                .setHash(0)
                .addAllChannels(List.of(MeasurementsProtos.ProtoChannel.newBuilder()
                                .setType(MeasurementTypeProtos.MeasurementType.MEASUREMENT_TYPE_TEMPERATURE)
                                .setTimestamp(Math.toIntExact(tsInSec))
                                .addAllSampleOffsets(List.of(223, 224))
                                .build(),
                        MeasurementsProtos.ProtoChannel.newBuilder()
                                .setType(MeasurementTypeProtos.MeasurementType.MEASUREMENT_TYPE_HUMIDITY)
                                .setTimestamp(Math.toIntExact(tsInSec))
                                .addAllSampleOffsets(List.of(20, 30))
                                .build()
                ))
                .build();
        List<CoapEfentoTransportResource.EfentoTelemetry> efentoMeasurements = coapEfentoTransportResource.getEfentoMeasurements(measurements, UUID.randomUUID());
        assertThat(efentoMeasurements).hasSize(2);
        assertThat(efentoMeasurements.get(0).getTs()).isEqualTo(tsInSec * 1000);
        assertThat(efentoMeasurements.get(0).getValues().getAsJsonObject().get("temperature_1").getAsDouble()).isEqualTo(22.3);
        assertThat(efentoMeasurements.get(0).getValues().getAsJsonObject().get("humidity_2").getAsDouble()).isEqualTo(20);
        assertThat(efentoMeasurements.get(1).getTs()).isEqualTo((tsInSec + 180) * 1000);
        assertThat(efentoMeasurements.get(1).getValues().getAsJsonObject().get("temperature_1").getAsDouble()).isEqualTo(22.4);
        assertThat(efentoMeasurements.get(1).getValues().getAsJsonObject().get("humidity_2").getAsDouble()).isEqualTo(30);
    }

    @Test
    void checkBinarySensor() {
        long tsInSec = Instant.now().getEpochSecond();
        MeasurementsProtos.ProtoMeasurements measurements = MeasurementsProtos.ProtoMeasurements.newBuilder()
                .setSerialNum(integerToByteString(1234))
                .setCloudToken("test_token")
                .setMeasurementPeriodBase(180)
                .setMeasurementPeriodFactor(1)
                .setBatteryStatus(true)
                .setSignal(0)
                .setNextTransmissionAt(1000)
                .setTransferReason(0)
                .setHash(0)
                .addChannels(MeasurementsProtos.ProtoChannel.newBuilder()
                        .setType(MeasurementTypeProtos.MeasurementType.MEASUREMENT_TYPE_OK_ALARM)
                        .setTimestamp(Math.toIntExact(tsInSec))
                        .addAllSampleOffsets(List.of(1, 1))
                        .build())
                .build();
        List<CoapEfentoTransportResource.EfentoTelemetry> efentoMeasurements = coapEfentoTransportResource.getEfentoMeasurements(measurements, UUID.randomUUID());
        assertThat(efentoMeasurements).hasSize(1);
        assertThat(efentoMeasurements.get(0).getTs()).isEqualTo(tsInSec * 1000);
        assertThat(efentoMeasurements.get(0).getValues().getAsJsonObject().get("ok_alarm_1").getAsString()).isEqualTo("ALARM");
    }

    @Test
    void checkBinarySensorWhenValueIsVarying() {
        long tsInSec = Instant.now().getEpochSecond();
        MeasurementsProtos.ProtoMeasurements measurements = MeasurementsProtos.ProtoMeasurements.newBuilder()
                .setSerialNum(integerToByteString(1234))
                .setCloudToken("test_token")
                .setMeasurementPeriodBase(180)
                .setMeasurementPeriodFactor(1)
                .setBatteryStatus(true)
                .setSignal(0)
                .setNextTransmissionAt(1000)
                .setTransferReason(0)
                .setHash(0)
                .addChannels(MeasurementsProtos.ProtoChannel.newBuilder()
                        .setType(MeasurementTypeProtos.MeasurementType.MEASUREMENT_TYPE_OK_ALARM)
                        .setTimestamp(Math.toIntExact(tsInSec))
                        .addAllSampleOffsets(List.of(1, -10))
                        .build())
                .build();
        List<CoapEfentoTransportResource.EfentoTelemetry> efentoMeasurements = coapEfentoTransportResource.getEfentoMeasurements(measurements, UUID.randomUUID());
        assertThat(efentoMeasurements).hasSize(2);
        assertThat(efentoMeasurements.get(0).getTs()).isEqualTo(tsInSec * 1000);
        assertThat(efentoMeasurements.get(0).getValues().getAsJsonObject().get("ok_alarm_1").getAsString()).isEqualTo("ALARM");
        assertThat(efentoMeasurements.get(1).getTs()).isEqualTo((tsInSec + 9) * 1000);
        assertThat(efentoMeasurements.get(1).getValues().getAsJsonObject().get("ok_alarm_1").getAsString()).isEqualTo("OK");
    }

    public static ByteString integerToByteString(Integer intValue) {
        // Allocate a ByteBuffer with the size of an integer (4 bytes)
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);

        // Put the integer value into the ByteBuffer
        buffer.putInt(intValue);

        // Convert the ByteBuffer to a byte array
        byte[] byteArray = buffer.array();

        // Create a ByteString from the byte array
        return ByteString.copyFrom(byteArray);
    }

}
