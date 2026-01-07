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
package org.thingsboard.server.transport.coap.efento;

import com.google.protobuf.ByteString;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.thingsboard.server.gen.transport.coap.MeasurementTypeProtos.MeasurementType;
import org.thingsboard.server.gen.transport.coap.MeasurementsProtos;
import org.thingsboard.server.gen.transport.coap.MeasurementsProtos.ProtoMeasurements;
import org.thingsboard.server.transport.coap.CoapTransportContext;
import org.thingsboard.server.transport.coap.efento.utils.CoapEfentoUtils;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.thingsboard.server.gen.transport.coap.MeasurementTypeProtos.MeasurementType.MEASUREMENT_TYPE_AMBIENT_LIGHT;
import static org.thingsboard.server.gen.transport.coap.MeasurementTypeProtos.MeasurementType.MEASUREMENT_TYPE_ATMOSPHERIC_PRESSURE;
import static org.thingsboard.server.gen.transport.coap.MeasurementTypeProtos.MeasurementType.MEASUREMENT_TYPE_BREATH_VOC;
import static org.thingsboard.server.gen.transport.coap.MeasurementTypeProtos.MeasurementType.MEASUREMENT_TYPE_CO2_EQUIVALENT;
import static org.thingsboard.server.gen.transport.coap.MeasurementTypeProtos.MeasurementType.MEASUREMENT_TYPE_CURRENT;
import static org.thingsboard.server.gen.transport.coap.MeasurementTypeProtos.MeasurementType.MEASUREMENT_TYPE_CURRENT_PRECISE;
import static org.thingsboard.server.gen.transport.coap.MeasurementTypeProtos.MeasurementType.MEASUREMENT_TYPE_DIFFERENTIAL_PRESSURE;
import static org.thingsboard.server.gen.transport.coap.MeasurementTypeProtos.MeasurementType.MEASUREMENT_TYPE_DISTANCE_MM;
import static org.thingsboard.server.gen.transport.coap.MeasurementTypeProtos.MeasurementType.MEASUREMENT_TYPE_ELECTRICITY_METER;
import static org.thingsboard.server.gen.transport.coap.MeasurementTypeProtos.MeasurementType.MEASUREMENT_TYPE_ELEC_METER_ACC_MAJOR;
import static org.thingsboard.server.gen.transport.coap.MeasurementTypeProtos.MeasurementType.MEASUREMENT_TYPE_ELEC_METER_ACC_MINOR;
import static org.thingsboard.server.gen.transport.coap.MeasurementTypeProtos.MeasurementType.MEASUREMENT_TYPE_FLOODING;
import static org.thingsboard.server.gen.transport.coap.MeasurementTypeProtos.MeasurementType.MEASUREMENT_TYPE_HIGH_PRESSURE;
import static org.thingsboard.server.gen.transport.coap.MeasurementTypeProtos.MeasurementType.MEASUREMENT_TYPE_HUMIDITY;
import static org.thingsboard.server.gen.transport.coap.MeasurementTypeProtos.MeasurementType.MEASUREMENT_TYPE_HUMIDITY_ACCURATE;
import static org.thingsboard.server.gen.transport.coap.MeasurementTypeProtos.MeasurementType.MEASUREMENT_TYPE_IAQ;
import static org.thingsboard.server.gen.transport.coap.MeasurementTypeProtos.MeasurementType.MEASUREMENT_TYPE_OK_ALARM;
import static org.thingsboard.server.gen.transport.coap.MeasurementTypeProtos.MeasurementType.MEASUREMENT_TYPE_OUTPUT_CONTROL;
import static org.thingsboard.server.gen.transport.coap.MeasurementTypeProtos.MeasurementType.MEASUREMENT_TYPE_PERCENTAGE;
import static org.thingsboard.server.gen.transport.coap.MeasurementTypeProtos.MeasurementType.MEASUREMENT_TYPE_PULSE_CNT;
import static org.thingsboard.server.gen.transport.coap.MeasurementTypeProtos.MeasurementType.MEASUREMENT_TYPE_PULSE_CNT_ACC_MAJOR;
import static org.thingsboard.server.gen.transport.coap.MeasurementTypeProtos.MeasurementType.MEASUREMENT_TYPE_PULSE_CNT_ACC_MINOR;
import static org.thingsboard.server.gen.transport.coap.MeasurementTypeProtos.MeasurementType.MEASUREMENT_TYPE_PULSE_CNT_ACC_WIDE_MAJOR;
import static org.thingsboard.server.gen.transport.coap.MeasurementTypeProtos.MeasurementType.MEASUREMENT_TYPE_PULSE_CNT_ACC_WIDE_MINOR;
import static org.thingsboard.server.gen.transport.coap.MeasurementTypeProtos.MeasurementType.MEASUREMENT_TYPE_SOIL_MOISTURE;
import static org.thingsboard.server.gen.transport.coap.MeasurementTypeProtos.MeasurementType.MEASUREMENT_TYPE_STATIC_IAQ;
import static org.thingsboard.server.gen.transport.coap.MeasurementTypeProtos.MeasurementType.MEASUREMENT_TYPE_TEMPERATURE;
import static org.thingsboard.server.gen.transport.coap.MeasurementTypeProtos.MeasurementType.MEASUREMENT_TYPE_VOLTAGE;
import static org.thingsboard.server.gen.transport.coap.MeasurementTypeProtos.MeasurementType.MEASUREMENT_TYPE_WATER_METER;
import static org.thingsboard.server.gen.transport.coap.MeasurementTypeProtos.MeasurementType.MEASUREMENT_TYPE_WATER_METER_ACC_MAJOR;
import static org.thingsboard.server.gen.transport.coap.MeasurementTypeProtos.MeasurementType.MEASUREMENT_TYPE_WATER_METER_ACC_MINOR;
import static org.thingsboard.server.transport.coap.efento.utils.CoapEfentoUtils.convertTimestampToUtcString;

class CoapEfentoTransportResourceTest {

    private static CoapEfentoTransportResource coapEfentoTransportResource;

    @BeforeAll
    static void setUp() {
        var ctxMock = mock(CoapTransportContext.class);
        coapEfentoTransportResource = new CoapEfentoTransportResource(ctxMock, "testName");
    }

    @Test
    void checkContinuousSensorWithSomeMeasurements() {
        long tsInSec = Instant.now().getEpochSecond();
        ProtoMeasurements measurements = ProtoMeasurements.newBuilder()
                .setSerialNum(integerToByteString(1234))
                .setCloudToken("test_token")
                .setMeasurementPeriodBase(180)
                .setMeasurementPeriodFactor(5)
                .setBatteryStatus(true)
                .setSignal(0)
                .setNextTransmissionAt(1000)
                .setTransferReason(0)
                .setHash(0)
                .addAllChannels(List.of(MeasurementsProtos.ProtoChannel.newBuilder()
                                .setType(MeasurementType.MEASUREMENT_TYPE_TEMPERATURE)
                                .setTimestamp(Math.toIntExact(tsInSec))
                                .addAllSampleOffsets(List.of(223, 224))
                                .build(),
                        MeasurementsProtos.ProtoChannel.newBuilder()
                                .setType(MeasurementType.MEASUREMENT_TYPE_HUMIDITY)
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
        assertThat(efentoMeasurements.get(1).getTs()).isEqualTo((tsInSec + 180 * 5) * 1000);
        assertThat(efentoMeasurements.get(1).getValues().getAsJsonObject().get("temperature_1").getAsDouble()).isEqualTo(22.4);
        assertThat(efentoMeasurements.get(1).getValues().getAsJsonObject().get("humidity_2").getAsDouble()).isEqualTo(30);
        checkDefaultMeasurements(measurements, efentoMeasurements, 180 * 5, false);
    }

    @ParameterizedTest
    @MethodSource
    void checkContinuousSensor(MeasurementType measurementType, List<Integer> sampleOffsets, String property, double expectedValue) {
        long tsInSec = Instant.now().getEpochSecond();
        ProtoMeasurements measurements = ProtoMeasurements.newBuilder()
                .setSerialNum(integerToByteString(1234))
                .setCloudToken("test_token")
                .setMeasurementPeriodBase(180)
                .setMeasurementPeriodFactor(0)
                .setBatteryStatus(true)
                .setSignal(0)
                .setNextTransmissionAt(1000)
                .setTransferReason(0)
                .setHash(0)
                .addAllChannels(List.of(MeasurementsProtos.ProtoChannel.newBuilder()
                        .setType(measurementType)
                        .setTimestamp(Math.toIntExact(tsInSec))
                        .addAllSampleOffsets(sampleOffsets)
                        .build()
                ))
                .build();
        List<CoapEfentoTransportResource.EfentoTelemetry> efentoMeasurements = coapEfentoTransportResource.getEfentoMeasurements(measurements, UUID.randomUUID());
        assertThat(efentoMeasurements).hasSize(1);
        assertThat(efentoMeasurements.get(0).getTs()).isEqualTo(tsInSec * 1000);
        assertThat(efentoMeasurements.get(0).getValues().getAsJsonObject().get(property).getAsDouble()).isEqualTo(expectedValue);
        checkDefaultMeasurements(measurements, efentoMeasurements, 180, false);
    }

    private static Stream<Arguments> checkContinuousSensor() {
        return Stream.of(
                Arguments.of(MEASUREMENT_TYPE_TEMPERATURE, List.of(223), "temperature_1", 22.3),
                Arguments.of(MEASUREMENT_TYPE_WATER_METER, List.of(1050), "pulse_counter_water_1", 1050),
                Arguments.of(MEASUREMENT_TYPE_HUMIDITY, List.of(20), "humidity_1", 20),
                Arguments.of(MEASUREMENT_TYPE_ATMOSPHERIC_PRESSURE, List.of(1013), "pressure_1", 101.3),
                Arguments.of(MEASUREMENT_TYPE_DIFFERENTIAL_PRESSURE, List.of(500), "pressure_diff_1", 500),
                Arguments.of(MEASUREMENT_TYPE_PULSE_CNT, List.of(300), "pulse_cnt_1", 300),
                Arguments.of(MEASUREMENT_TYPE_IAQ, List.of(150), "iaq_1", 50.0),
                Arguments.of(MEASUREMENT_TYPE_ELECTRICITY_METER, List.of(1200), "watt_hour_1", 1200),
                Arguments.of(MEASUREMENT_TYPE_SOIL_MOISTURE, List.of(35), "soil_moisture_1", 35),
                Arguments.of(MEASUREMENT_TYPE_AMBIENT_LIGHT, List.of(500), "ambient_light_1", 50),
                Arguments.of(MEASUREMENT_TYPE_HIGH_PRESSURE, List.of(200000), "high_pressure_1", 200000),
                Arguments.of(MEASUREMENT_TYPE_DISTANCE_MM, List.of(1500), "distance_mm_1", 1500),
                Arguments.of(MEASUREMENT_TYPE_HUMIDITY_ACCURATE, List.of(525), "humidity_relative_1", 52.5),
                Arguments.of(MEASUREMENT_TYPE_STATIC_IAQ, List.of(110), "static_iaq_1", 36),
                Arguments.of(MEASUREMENT_TYPE_CO2_EQUIVALENT, List.of(450), "co2_1", 150),
                Arguments.of(MEASUREMENT_TYPE_BREATH_VOC, List.of(220), "breath_voc_1", 73),
                Arguments.of(MEASUREMENT_TYPE_PERCENTAGE, List.of(80), "percentage_1", 0.8),
                Arguments.of(MEASUREMENT_TYPE_VOLTAGE, List.of(2400), "voltage_1", 240.0),
                Arguments.of(MEASUREMENT_TYPE_CURRENT, List.of(550), "current_1", 5.5),
                Arguments.of(MEASUREMENT_TYPE_CURRENT_PRECISE, List.of(275), "current_precise_1", 0.275)
        );
    }

    @ParameterizedTest
    @MethodSource
    void checkPulseCounterSensors(MeasurementType minorType, List<Integer> minorSampleOffsets, MeasurementType majorType, List<Integer> majorSampleOffsets,
                                  String totalPropertyName, double expectedTotalValue) {
        long tsInSec = Instant.now().getEpochSecond();
        ProtoMeasurements measurements = ProtoMeasurements.newBuilder()
                .setSerialNum(integerToByteString(1234))
                .setCloudToken("test_token")
                .setMeasurementPeriodBase(180)
                .setMeasurementPeriodFactor(0)
                .setBatteryStatus(true)
                .setSignal(0)
                .setNextTransmissionAt(1000)
                .setTransferReason(0)
                .setHash(0)
                .addAllChannels(Arrays.asList(MeasurementsProtos.ProtoChannel.newBuilder()
                                .setType(majorType)
                                .setTimestamp(Math.toIntExact(tsInSec))
                                .addAllSampleOffsets(majorSampleOffsets)
                                .build(),
                        MeasurementsProtos.ProtoChannel.newBuilder()
                                .setType(minorType)
                                .setTimestamp(Math.toIntExact(tsInSec))
                                .addAllSampleOffsets(minorSampleOffsets)
                                .build()))
                .build();
        List<CoapEfentoTransportResource.EfentoTelemetry> efentoMeasurements = coapEfentoTransportResource.getEfentoMeasurements(measurements, UUID.randomUUID());
        assertThat(efentoMeasurements).hasSize(1);
        assertThat(efentoMeasurements.get(0).getTs()).isEqualTo(tsInSec * 1000);
        assertThat(efentoMeasurements.get(0).getValues().getAsJsonObject().get(totalPropertyName + "_2").getAsDouble()).isEqualTo(expectedTotalValue);
        checkDefaultMeasurements(measurements, efentoMeasurements, 180, false);
    }

    private static Stream<Arguments> checkPulseCounterSensors() {
        return Stream.of(
                Arguments.of(MEASUREMENT_TYPE_WATER_METER_ACC_MINOR, List.of(15*6), MEASUREMENT_TYPE_WATER_METER_ACC_MAJOR,
                        List.of(625*4), "water_cnt_acc_total", 625.0*100 + 15),
                Arguments.of(MEASUREMENT_TYPE_PULSE_CNT_ACC_MINOR, List.of(10*6), MEASUREMENT_TYPE_PULSE_CNT_ACC_MAJOR,
                        List.of(300*4), "pulse_cnt_acc_total", 300.0*1000 + 10),
                Arguments.of(MEASUREMENT_TYPE_ELEC_METER_ACC_MINOR, List.of(12*6), MEASUREMENT_TYPE_ELEC_METER_ACC_MAJOR,
                        List.of(100*4), "elec_meter_acc_total", 100.0*1000 + 12),
                Arguments.of(MEASUREMENT_TYPE_PULSE_CNT_ACC_WIDE_MINOR, List.of(13*6), MEASUREMENT_TYPE_PULSE_CNT_ACC_WIDE_MAJOR,
                        List.of(440*4), "pulse_cnt_acc_wide_total", 440.0*1000000 + 13));
    }


    @Test
    void checkBinarySensor() {
        long tsInSec = Instant.now().getEpochSecond();
        ProtoMeasurements measurements = ProtoMeasurements.newBuilder()
                .setSerialNum(integerToByteString(1234))
                .setCloudToken("test_token")
                .setMeasurementPeriodBase(180)
                .setMeasurementPeriodFactor(0)
                .setBatteryStatus(true)
                .setSignal(0)
                .setNextTransmissionAt(1000)
                .setTransferReason(0)
                .setHash(0)
                .addChannels(MeasurementsProtos.ProtoChannel.newBuilder()
                        .setType(MEASUREMENT_TYPE_OK_ALARM)
                        .setTimestamp(Math.toIntExact(tsInSec))
                        .addAllSampleOffsets(List.of(1, 1))
                        .build())
                .build();
        List<CoapEfentoTransportResource.EfentoTelemetry> efentoMeasurements = coapEfentoTransportResource.getEfentoMeasurements(measurements, UUID.randomUUID());
        assertThat(efentoMeasurements).hasSize(1);
        assertThat(efentoMeasurements.get(0).getTs()).isEqualTo(tsInSec * 1000);
        assertThat(efentoMeasurements.get(0).getValues().getAsJsonObject().get("ok_alarm_1").getAsString()).isEqualTo("ALARM");
        checkDefaultMeasurements(measurements, efentoMeasurements, 180 * 14, true);
    }

    @ParameterizedTest
    @MethodSource
    void checkBinarySensorWhenValueIsVarying(MeasurementType measurementType, String property, String expectedValueWhenOffsetNotOk, String expectedValueWhenOffsetOk) {
        long tsInSec = Instant.now().getEpochSecond();
        ProtoMeasurements measurements = ProtoMeasurements.newBuilder()
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
                        .setType(measurementType)
                        .setTimestamp(Math.toIntExact(tsInSec))
                        .addAllSampleOffsets(List.of(1, -10))
                        .build())
                .build();
        List<CoapEfentoTransportResource.EfentoTelemetry> efentoMeasurements = coapEfentoTransportResource.getEfentoMeasurements(measurements, UUID.randomUUID());
        assertThat(efentoMeasurements).hasSize(2);
        assertThat(efentoMeasurements.get(0).getTs()).isEqualTo(tsInSec * 1000);
        assertThat(efentoMeasurements.get(0).getValues().getAsJsonObject().get(property).getAsString()).isEqualTo(expectedValueWhenOffsetNotOk);
        assertThat(efentoMeasurements.get(1).getTs()).isEqualTo((tsInSec + 9) * 1000);
        assertThat(efentoMeasurements.get(1).getValues().getAsJsonObject().get(property).getAsString()).isEqualTo(expectedValueWhenOffsetOk);
        checkDefaultMeasurements(measurements, efentoMeasurements, 180, true);
    }

    private static Stream<Arguments> checkBinarySensorWhenValueIsVarying() {
        return Stream.of(
                Arguments.of(MEASUREMENT_TYPE_OK_ALARM, "ok_alarm_1", "ALARM", "OK"),
                Arguments.of(MEASUREMENT_TYPE_FLOODING, "flooding_1", "WATER_DETECTED", "OK"),
                Arguments.of(MEASUREMENT_TYPE_OUTPUT_CONTROL, "output_control_1", "ON", "OFF")
        );
    }

    @Test
    void checkExceptionWhenChannelsListIsEmpty() {
        ProtoMeasurements measurements = ProtoMeasurements.newBuilder()
                .setSerialNum(integerToByteString(1234))
                .setCloudToken("test_token")
                .setMeasurementPeriodBase(180)
                .setMeasurementPeriodFactor(1)
                .setBatteryStatus(true)
                .setSignal(0)
                .setNextTransmissionAt(1000)
                .setTransferReason(0)
                .setHash(0)
                .build();
        UUID sessionId = UUID.randomUUID();

        assertThatThrownBy(() -> coapEfentoTransportResource.getEfentoMeasurements(measurements, sessionId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("[" + sessionId + "]: Failed to get Efento measurements, reason: channels list is empty!");
    }

    @Test
    void checkExceptionWhenValuesMapIsEmpty() {
        long tsInSec = Instant.now().getEpochSecond();
        ProtoMeasurements measurements = ProtoMeasurements.newBuilder()
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
                        .setType(MEASUREMENT_TYPE_TEMPERATURE)
                        .setTimestamp(Math.toIntExact(tsInSec))
                        .build())
                .build();
        UUID sessionId = UUID.randomUUID();

        assertThatThrownBy(() -> coapEfentoTransportResource.getEfentoMeasurements(measurements, sessionId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("[" + sessionId + "]: Failed to collect Efento measurements, reason, values map is empty!");
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

    private void checkDefaultMeasurements(ProtoMeasurements incomingMeasurements,
                                          List<CoapEfentoTransportResource.EfentoTelemetry> actualEfentoMeasurements,
                                          long expectedMeasurementInterval,
                                          boolean isBinarySensor) {
        for (int i = 0; i < actualEfentoMeasurements.size(); i++) {
            CoapEfentoTransportResource.EfentoTelemetry actualEfentoMeasurement = actualEfentoMeasurements.get(i);
            assertThat(actualEfentoMeasurement.getValues().getAsJsonObject().get("serial").getAsString()).isEqualTo(CoapEfentoUtils.convertByteArrayToString(incomingMeasurements.getSerialNum().toByteArray()));
            assertThat(actualEfentoMeasurement.getValues().getAsJsonObject().get("battery").getAsString()).isEqualTo(incomingMeasurements.getBatteryStatus() ? "ok" : "low");
            MeasurementsProtos.ProtoChannel protoChannel = incomingMeasurements.getChannelsList().get(0);
            long measuredAt = isBinarySensor ?
                    TimeUnit.SECONDS.toMillis(protoChannel.getTimestamp()) + Math.abs(TimeUnit.SECONDS.toMillis(protoChannel.getSampleOffsetsList().get(i))) - 1000 :
                    TimeUnit.SECONDS.toMillis(protoChannel.getTimestamp() + i * expectedMeasurementInterval);
            assertThat(actualEfentoMeasurement.getValues().getAsJsonObject().get("measured_at").getAsString()).isEqualTo(convertTimestampToUtcString(measuredAt));
            assertThat(actualEfentoMeasurement.getValues().getAsJsonObject().get("next_transmission_at").getAsString()).isEqualTo(convertTimestampToUtcString(TimeUnit.SECONDS.toMillis(incomingMeasurements.getNextTransmissionAt())));
            assertThat(actualEfentoMeasurement.getValues().getAsJsonObject().get("signal").getAsLong()).isEqualTo(incomingMeasurements.getSignal());
            assertThat(actualEfentoMeasurement.getValues().getAsJsonObject().get("measurement_interval").getAsDouble()).isEqualTo(expectedMeasurementInterval);
        }
    }

}
