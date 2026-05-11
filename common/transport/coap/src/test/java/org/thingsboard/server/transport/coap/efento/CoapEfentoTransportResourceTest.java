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
import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.thingsboard.server.gen.transport.coap.ConfigProtos;
import org.thingsboard.server.gen.transport.coap.ConfigTypesProtos;
import org.thingsboard.server.gen.transport.coap.DeviceInfoProtos;
import org.thingsboard.server.gen.transport.coap.MeasurementTypeProtos.MeasurementType;
import org.thingsboard.server.gen.transport.coap.MeasurementsProtos;
import org.thingsboard.server.gen.transport.coap.MeasurementsProtos.ProtoMeasurements;
import org.thingsboard.server.gen.transport.coap.ProtoRuleProtos;
import org.thingsboard.server.transport.coap.CoapTransportContext;
import org.thingsboard.server.transport.coap.efento.utils.CoapEfentoUtils;

import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
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
                .setSerialNumber(integerToByteString(1234))
                .setCloudToken("test_token")
                .setMeasurementPeriodBase(180)
                .setMeasurementPeriodFactor(5)
                .setBatteryStatus(true)
                .setSignal(0)
                .setNextTransmissionAt(1000)
                .setTransferReason(0)
                .setConfigurationHash(0)
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
        checkDefaultMeasurements(measurements, efentoMeasurements, 180 * 5);
    }

    @ParameterizedTest
    @MethodSource
    void checkContinuousSensor(MeasurementType measurementType, List<Integer> sampleOffsets, String property, double expectedValue) {
        long tsInSec = Instant.now().getEpochSecond();
        ProtoMeasurements measurements = ProtoMeasurements.newBuilder()
                .setSerialNumber(integerToByteString(1234))
                .setCloudToken("test_token")
                .setMeasurementPeriodBase(180)
                .setMeasurementPeriodFactor(0)
                .setBatteryStatus(true)
                .setSignal(0)
                .setNextTransmissionAt(1000)
                .setTransferReason(0)
                .setConfigurationHash(0)
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
        checkDefaultMeasurements(measurements, efentoMeasurements, 180);
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
                .setSerialNumber(integerToByteString(1234))
                .setCloudToken("test_token")
                .setMeasurementPeriodBase(180)
                .setMeasurementPeriodFactor(0)
                .setBatteryStatus(true)
                .setSignal(0)
                .setNextTransmissionAt(1000)
                .setTransferReason(0)
                .setConfigurationHash(0)
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
        checkDefaultMeasurements(measurements, efentoMeasurements, 180);
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
                .setSerialNumber(integerToByteString(1234))
                .setCloudToken("test_token")
                .setMeasurementPeriodBase(180)
                .setMeasurementPeriodFactor(0)
                .setBatteryStatus(true)
                .setSignal(0)
                .setNextTransmissionAt(1000)
                .setTransferReason(0)
                .setConfigurationHash(0)
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
        checkDefaultMeasurements(measurements, efentoMeasurements, 180 * 14);
    }

    @ParameterizedTest
    @MethodSource
    void checkBinarySensorWhenValueIsVarying(MeasurementType measurementType, String property, String expectedValueWhenOffsetNotOk, String expectedValueWhenOffsetOk) {
        long tsInSec = Instant.now().getEpochSecond();
        ProtoMeasurements measurements = ProtoMeasurements.newBuilder()
                .setSerialNumber(integerToByteString(1234))
                .setCloudToken("test_token")
                .setMeasurementPeriodBase(180)
                .setMeasurementPeriodFactor(1)
                .setBatteryStatus(true)
                .setSignal(0)
                .setNextTransmissionAt(1000)
                .setTransferReason(0)
                .setConfigurationHash(0)
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
        checkDefaultMeasurements(measurements, efentoMeasurements, 180);
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
                .setSerialNumber(integerToByteString(1234))
                .setCloudToken("test_token")
                .setMeasurementPeriodBase(180)
                .setMeasurementPeriodFactor(1)
                .setBatteryStatus(true)
                .setSignal(0)
                .setNextTransmissionAt(1000)
                .setTransferReason(0)
                .setConfigurationHash(0)
                .build();
        UUID sessionId = UUID.randomUUID();

        assertThatThrownBy(() -> coapEfentoTransportResource.getEfentoMeasurements(measurements, sessionId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("[" + sessionId + "]: Failed to get Efento measurements, reason: channels list is empty!");
    }

    // -------------------------------------------------------------------------
    // ProtoDeviceInfo parsing tests
    // -------------------------------------------------------------------------

    @Test
    void getEfentoDeviceInfo_parsesSwVersion() {
        DeviceInfoProtos.ProtoDeviceInfo deviceInfo = minimalDeviceInfo()
                .setSwVersion(1546)   // ver 06.10 => 0x060A => 1546
                .build();

        CoapEfentoTransportResource.EfentoTelemetry result = coapEfentoTransportResource.getEfentoDeviceInfo(deviceInfo);

        assertThat(result.getValues().getAsJsonObject().get("sw_version").getAsInt()).isEqualTo(1546);
    }

    @Test
    void getEfentoDeviceInfo_parsesAllMemoryStatistics() {
        // proto uint32 maps to Java int; the "undefined" sentinel is 0xFFFFFFFF = -1 in signed int
        int undefinedTs = -1;
        int knownTs = 1_700_000_000; // arbitrary known Unix timestamp fitting in uint32
        // clearMemoryStatistics() is needed because minimalDeviceInfo() pre-populates 22 zeros;
        // addAllMemoryStatistics() appends in proto, so without clear the test values land at indices 22-43
        DeviceInfoProtos.ProtoDeviceInfo.Builder builder = minimalDeviceInfo().clearMemoryStatistics();
        builder.addAllMemoryStatistics(List.of(
                0,          // [0] nv_storage_status: 0 = no errors
                knownTs,    // [1] timestamp_of_the_end_of_collecting_statistics
                1048576,    // [2] capacity_of_memory_in_bytes
                512000,     // [3] used_space_in_bytes
                1024,       // [4] size_of_invalid_packets_in_bytes
                256,        // [5] size_of_corrupted_packets_in_bytes
                100,        // [6] number_of_valid_packets
                10,         // [7] number_of_invalid_packets
                2,          // [8] number_of_corrupted_packets
                500,        // [9] number_of_all_samples_for_channel_1
                400,        // [10] number_of_all_samples_for_channel_2
                300,        // [11] number_of_all_samples_for_channel_3
                200,        // [12] number_of_all_samples_for_channel_4
                100,        // [13] number_of_all_samples_for_channel_5
                50,         // [14] number_of_all_samples_for_channel_6
                undefinedTs, // [15] timestamp_of_the_first_binary_measurement (undefined)
                undefinedTs, // [16] timestamp_of_the_last_binary_measurement (undefined)
                undefinedTs, // [17] timestamp_of_the_first_binary_measurement_sent (undefined)
                knownTs,    // [18] timestamp_of_the_first_continuous_measurement
                knownTs,    // [19] timestamp_of_the_last_continuous_measurement
                knownTs,    // [20] timestamp_of_the_last_continuous_measurement_sent
                42          // [21] nvm_write_counter
        ));

        CoapEfentoTransportResource.EfentoTelemetry result = coapEfentoTransportResource.getEfentoDeviceInfo(builder.build());
        var json = result.getValues().getAsJsonObject();

        assertThat(json.get("nv_storage_status").getAsLong()).isEqualTo(0);
        assertThat(json.get("timestamp_of_the_end_of_collecting_statistics").getAsString()).isEqualTo(formatDate(knownTs));
        assertThat(json.get("capacity_of_memory_in_bytes").getAsLong()).isEqualTo(1048576L);
        assertThat(json.get("used_space_in_bytes").getAsLong()).isEqualTo(512000L);
        assertThat(json.get("size_of_invalid_packets_in_bytes").getAsLong()).isEqualTo(1024L);
        assertThat(json.get("size_of_corrupted_packets_in_bytes").getAsLong()).isEqualTo(256L);
        assertThat(json.get("number_of_valid_packets").getAsLong()).isEqualTo(100L);
        assertThat(json.get("number_of_invalid_packets").getAsLong()).isEqualTo(10L);
        assertThat(json.get("number_of_corrupted_packets").getAsLong()).isEqualTo(2L);
        assertThat(json.get("number_of_all_samples_for_channel_1").getAsLong()).isEqualTo(500L);
        assertThat(json.get("number_of_all_samples_for_channel_2").getAsLong()).isEqualTo(400L);
        assertThat(json.get("number_of_all_samples_for_channel_3").getAsLong()).isEqualTo(300L);
        assertThat(json.get("number_of_all_samples_for_channel_4").getAsLong()).isEqualTo(200L);
        assertThat(json.get("number_of_all_samples_for_channel_5").getAsLong()).isEqualTo(100L);
        assertThat(json.get("number_of_all_samples_for_channel_6").getAsLong()).isEqualTo(50L);
        assertThat(json.get("timestamp_of_the_first_binary_measurement").getAsString()).isEqualTo("Undefined");
        assertThat(json.get("timestamp_of_the_last_binary_measurement").getAsString()).isEqualTo("Undefined");
        assertThat(json.get("timestamp_of_the_first_binary_measurement_sent").getAsString()).isEqualTo("Undefined");
        assertThat(json.get("timestamp_of_the_first_continuous_measurement").getAsString()).isEqualTo(formatDate(knownTs));
        assertThat(json.get("timestamp_of_the_last_continuous_measurement").getAsString()).isEqualTo(formatDate(knownTs));
        assertThat(json.get("timestamp_of_the_last_continuous_measurement_sent").getAsString()).isEqualTo(formatDate(knownTs));
        assertThat(json.get("nvm_write_counter").getAsLong()).isEqualTo(42L);
    }

    @Test
    void getEfentoDeviceInfo_parsesModemInfo() {
        // 34 modem parameters (indices 0-33) as defined in proto_device_info.proto
        List<Integer> params = List.of(
                0,    // [0]  sc_EARNFCN_offset
                1000, // [1]  sc_EARFCN
                42,   // [2]  sc_PCI
                123456, // [3]  sc_Cell_id
                -90,  // [4]  sc_RSRP
                -10,  // [5]  sc_RSRQ
                -80,  // [6]  sc_RSSI
                15,   // [7]  sc_SINR
                3,    // [8]  sc_Band
                1234, // [9]  sc_TAC
                1,    // [10] sc_ECL
                -30,  // [11] sc_TX_PWR
                2,    // [12] op_mode
                999,  // [13] nc_EARFCN
                1,    // [14] nc_EARNFCN_offset
                100,  // [15] nc_PCI
                -95,  // [16] nc_RSRP
                5,    // [17] RLC_UL_BLER
                3,    // [18] RLC_DL_BLER
                4,    // [19] MAC_UL_BLER
                2,    // [20] MAC_DL_BLER
                50000,// [21] MAC_UL_TOTAL_BYTES
                60000,// [22] MAC_DL_TOTAL_BYTES
                200,  // [23] MAC_UL_total_HARQ_Tx
                150,  // [24] MAC_DL_total_HARQ_Tx
                10,   // [25] MAC_UL_HARQ_re_Tx
                8,    // [26] MAC_DL_HARQ_re_Tx
                1000, // [27] RLC_UL_tput
                1200, // [28] RLC_DL_tput
                900,  // [29] MAC_UL_tput
                1100, // [30] MAC_DL_tput
                5000, // [31] sleep_duration
                300,  // [32] rx_time
                100   // [33] tx_time
        );
        DeviceInfoProtos.ProtoDeviceInfo deviceInfo = minimalDeviceInfo()
                .setModem(DeviceInfoProtos.ProtoModem.newBuilder()
                        .setType(DeviceInfoProtos.ModemType.MODEM_TYPE_BC66)
                        .addAllParameters(params)
                        .build())
                .build();

        CoapEfentoTransportResource.EfentoTelemetry result = coapEfentoTransportResource.getEfentoDeviceInfo(deviceInfo);
        var json = result.getValues().getAsJsonObject();

        assertThat(json.get("modem_types").getAsString()).isEqualTo("MODEM_TYPE_BC66");
        assertThat(json.get("sc_EARNFCN_offset").getAsInt()).isEqualTo(0);
        assertThat(json.get("sc_EARFCN").getAsInt()).isEqualTo(1000);
        assertThat(json.get("sc_PCI").getAsInt()).isEqualTo(42);
        assertThat(json.get("sc_Cell_id").getAsInt()).isEqualTo(123456);
        assertThat(json.get("sc_RSRP").getAsInt()).isEqualTo(-90);
        assertThat(json.get("sc_RSRQ").getAsInt()).isEqualTo(-10);
        assertThat(json.get("sc_RSSI").getAsInt()).isEqualTo(-80);
        assertThat(json.get("sc_SINR").getAsInt()).isEqualTo(15);
        assertThat(json.get("sc_Band").getAsInt()).isEqualTo(3);
        assertThat(json.get("sc_TAC").getAsInt()).isEqualTo(1234);
        assertThat(json.get("sc_ECL").getAsInt()).isEqualTo(1);
        assertThat(json.get("sc_TX_PWR").getAsInt()).isEqualTo(-30);
        assertThat(json.get("op_mode").getAsInt()).isEqualTo(2);
        assertThat(json.get("nc_EARFCN").getAsInt()).isEqualTo(999);
        assertThat(json.get("nc_EARNFCN_offset").getAsInt()).isEqualTo(1);
        assertThat(json.get("nc_PCI").getAsInt()).isEqualTo(100);
        assertThat(json.get("nc_RSRP").getAsInt()).isEqualTo(-95);
        assertThat(json.get("RLC_UL_BLER").getAsInt()).isEqualTo(5);
        assertThat(json.get("RLC_DL_BLER").getAsInt()).isEqualTo(3);
        assertThat(json.get("MAC_UL_BLER").getAsInt()).isEqualTo(4);
        assertThat(json.get("MAC_DL_BLER").getAsInt()).isEqualTo(2);
        assertThat(json.get("MAC_UL_TOTAL_BYTES").getAsInt()).isEqualTo(50000);
        assertThat(json.get("MAC_DL_TOTAL_BYTES").getAsInt()).isEqualTo(60000);
        assertThat(json.get("MAC_UL_total_HARQ_Tx").getAsInt()).isEqualTo(200);
        assertThat(json.get("MAC_DL_total_HARQ_Tx").getAsInt()).isEqualTo(150);
        assertThat(json.get("MAC_UL_HARQ_re_Tx").getAsInt()).isEqualTo(10);
        assertThat(json.get("MAC_DL_HARQ_re_Tx").getAsInt()).isEqualTo(8);
        assertThat(json.get("RLC_UL_tput").getAsInt()).isEqualTo(1000);
        assertThat(json.get("RLC_DL_tput").getAsInt()).isEqualTo(1200);
        assertThat(json.get("MAC_UL_tput").getAsInt()).isEqualTo(900);
        assertThat(json.get("MAC_DL_tput").getAsInt()).isEqualTo(1100);
        assertThat(json.get("sleep_duration").getAsInt()).isEqualTo(5000);
        assertThat(json.get("rx_time").getAsInt()).isEqualTo(300);
        assertThat(json.get("tx_time").getAsInt()).isEqualTo(100);
    }

    @Test
    void getEfentoDeviceInfo_parsesModemInfoBC660() {
        // 22 modem parameters for MODEM_TYPE_BC660 as defined in proto_device_info.proto
        List<Integer> params = List.of(
                1000, // [0]  sc_EARFCN
                5,    // [1]  sc_EARNFCN_offset
                42,   // [2]  sc_PCI
                123456, // [3]  sc_Cell_id
                -90,  // [4]  sc_RSRP
                -10,  // [5]  sc_RSRQ
                -80,  // [6]  sc_RSSI
                15,   // [7]  sc_SINR
                3,    // [8]  sc_Band
                1234, // [9]  sc_TAC
                1,    // [10] sc_ECL
                -30,  // [11] sc_TX_PWR
                2,    // [12] op_mode
                999,  // [13] nc_EARFCN
                100,  // [14] nc_PCI
                -95,  // [15] nc_RSRP
                -12,  // [16] nc_RSRQ
                5000, // [17] sleep_duration
                300,  // [18] rx_time
                100,  // [19] tx_time
                1,    // [20] PLMN_state
                26201 // [21] select_PLMN
        );
        DeviceInfoProtos.ProtoDeviceInfo deviceInfo = minimalDeviceInfo()
                .setModem(DeviceInfoProtos.ProtoModem.newBuilder()
                        .setType(DeviceInfoProtos.ModemType.MODEM_TYPE_BC660)
                        .addAllParameters(params)
                        .setSimCardIdentification("89001012012341234120")
                        .setFirmwareVersion(DeviceInfoProtos.ModemFirmwareVersion.MODEM_FIRMWARE_VERSION_BC660_V2)
                        .setModemIdentification("123456789012345")
                        .addAllModemStatistics(List.of(10, 3600, 7200, 600))
                        .build())
                .build();

        CoapEfentoTransportResource.EfentoTelemetry result = coapEfentoTransportResource.getEfentoDeviceInfo(deviceInfo);
        var json = result.getValues().getAsJsonObject();

        assertThat(json.get("modem_types").getAsString()).isEqualTo("MODEM_TYPE_BC660");
        assertThat(json.get("sim_card_identification").getAsString()).isEqualTo("89001012012341234120");
        assertThat(json.get("firmware_version").getAsString()).isEqualTo("MODEM_FIRMWARE_VERSION_BC660_V2");
        assertThat(json.get("modem_identification").getAsString()).isEqualTo("123456789012345");
        assertThat(json.get("modem_transmissions_count").getAsInt()).isEqualTo(10);
        assertThat(json.get("modem_time_since_last_devinfo").getAsInt()).isEqualTo(3600);
        assertThat(json.get("modem_total_psm_time").getAsInt()).isEqualTo(7200);
        assertThat(json.get("modem_total_active_time").getAsInt()).isEqualTo(600);
        assertThat(json.get("sc_EARFCN").getAsInt()).isEqualTo(1000);
        assertThat(json.get("sc_EARNFCN_offset").getAsInt()).isEqualTo(5);
        assertThat(json.get("sc_PCI").getAsInt()).isEqualTo(42);
        assertThat(json.get("sc_Cell_id").getAsInt()).isEqualTo(123456);
        assertThat(json.get("sc_RSRP").getAsInt()).isEqualTo(-90);
        assertThat(json.get("sc_RSRQ").getAsInt()).isEqualTo(-10);
        assertThat(json.get("sc_RSSI").getAsInt()).isEqualTo(-80);
        assertThat(json.get("sc_SINR").getAsInt()).isEqualTo(15);
        assertThat(json.get("sc_Band").getAsInt()).isEqualTo(3);
        assertThat(json.get("sc_TAC").getAsInt()).isEqualTo(1234);
        assertThat(json.get("sc_ECL").getAsInt()).isEqualTo(1);
        assertThat(json.get("sc_TX_PWR").getAsInt()).isEqualTo(-30);
        assertThat(json.get("op_mode").getAsInt()).isEqualTo(2);
        assertThat(json.get("nc_EARFCN").getAsInt()).isEqualTo(999);
        assertThat(json.get("nc_PCI").getAsInt()).isEqualTo(100);
        assertThat(json.get("nc_RSRP").getAsInt()).isEqualTo(-95);
        assertThat(json.get("nc_RSRQ").getAsInt()).isEqualTo(-12);
        assertThat(json.get("sleep_duration").getAsInt()).isEqualTo(5000);
        assertThat(json.get("rx_time").getAsInt()).isEqualTo(300);
        assertThat(json.get("tx_time").getAsInt()).isEqualTo(100);
        assertThat(json.get("PLMN_state").getAsInt()).isEqualTo(1);
        assertThat(json.get("select_PLMN").getAsInt()).isEqualTo(26201);
        // BC66-specific fields must not be present
        assertThat(json.has("nc_EARNFCN_offset")).isFalse();
        assertThat(json.has("RLC_UL_BLER")).isFalse();
    }

    @Test
    void getEfentoDeviceInfo_parsesModemInfoSharedModem() {
        // 4 modem parameters for MODEM_TYPE_SHARED_MODEM as defined in proto_device_info.proto
        List<Integer> params = List.of(
                -90,  // [0] RSRP
                -10,  // [1] RSRQ
                -80,  // [2] RSSI
                15    // [3] SINR
        );
        DeviceInfoProtos.ProtoDeviceInfo deviceInfo = minimalDeviceInfo()
                .setModem(DeviceInfoProtos.ProtoModem.newBuilder()
                        .setType(DeviceInfoProtos.ModemType.MODEM_TYPE_SHARED_MODEM)
                        .addAllParameters(params)
                        .setModemIdentification("SN-ABCDEF")
                        .build())
                .build();

        CoapEfentoTransportResource.EfentoTelemetry result = coapEfentoTransportResource.getEfentoDeviceInfo(deviceInfo);
        var json = result.getValues().getAsJsonObject();

        assertThat(json.get("modem_types").getAsString()).isEqualTo("MODEM_TYPE_SHARED_MODEM");
        assertThat(json.get("modem_identification").getAsString()).isEqualTo("SN-ABCDEF");
        assertThat(json.get("RSRP").getAsInt()).isEqualTo(-90);
        assertThat(json.get("RSRQ").getAsInt()).isEqualTo(-10);
        assertThat(json.get("RSSI").getAsInt()).isEqualTo(-80);
        assertThat(json.get("SINR").getAsInt()).isEqualTo(15);
        // BC66/BC660-specific fields must not be present
        assertThat(json.has("sc_EARFCN")).isFalse();
        assertThat(json.has("sc_EARNFCN_offset")).isFalse();
    }

    @Test
    void getEfentoDeviceInfo_parsesNewModemFields() {
        // New ProtoModem fields: sim_card_identification, firmware_version, modem_identification, modem_statistics
        DeviceInfoProtos.ProtoDeviceInfo deviceInfo = minimalDeviceInfo()
                .setModem(DeviceInfoProtos.ProtoModem.newBuilder()
                        .setType(DeviceInfoProtos.ModemType.MODEM_TYPE_BC66)
                        .addAllParameters(java.util.Collections.nCopies(34, 0))
                        .setSimCardIdentification("89012345678901234567")
                        .setFirmwareVersion(DeviceInfoProtos.ModemFirmwareVersion.MODEM_FIRMWARE_VERSION_READING_ERROR)
                        .setModemIdentification("352519100417272")
                        .addAllModemStatistics(List.of(5, 1800, 3600, 120))
                        .build())
                .build();

        CoapEfentoTransportResource.EfentoTelemetry result = coapEfentoTransportResource.getEfentoDeviceInfo(deviceInfo);
        var json = result.getValues().getAsJsonObject();

        assertThat(json.get("sim_card_identification").getAsString()).isEqualTo("89012345678901234567");
        assertThat(json.get("firmware_version").getAsString()).isEqualTo("MODEM_FIRMWARE_VERSION_READING_ERROR");
        assertThat(json.get("modem_identification").getAsString()).isEqualTo("352519100417272");
        assertThat(json.get("modem_transmissions_count").getAsInt()).isEqualTo(5);
        assertThat(json.get("modem_time_since_last_devinfo").getAsInt()).isEqualTo(1800);
        assertThat(json.get("modem_total_psm_time").getAsInt()).isEqualTo(3600);
        assertThat(json.get("modem_total_active_time").getAsInt()).isEqualTo(120);
    }

    @Test
    void getEfentoDeviceInfo_parsesRuntimeInfo() {
        long batteryResetTs = 1_700_000_000L;
        DeviceInfoProtos.ProtoDeviceInfo deviceInfo = minimalDeviceInfo()
                .setRuntimeInfo(DeviceInfoProtos.ProtoRuntime.newBuilder()
                        .setUpTime(3600)
                        .addAllMessageCounters(List.of(10, 5, 9))
                        .setMcuTemperature(25)
                        .setBatteryVoltage(3200)
                        .setMinBatteryMcuTemperature(20)
                        .setBatteryResetTimestamp((int) batteryResetTs)
                        .setMaxMcuTemperature(40)
                        .setMinMcuTemperature(10)
                        .addAllRuntimeErrors(List.of(0, 1))
                        .build())
                .build();

        CoapEfentoTransportResource.EfentoTelemetry result = coapEfentoTransportResource.getEfentoDeviceInfo(deviceInfo);
        var json = result.getValues().getAsJsonObject();

        assertThat(json.get("up_time").getAsLong()).isEqualTo(3600);
        assertThat(json.get("mcu_temp").getAsInt()).isEqualTo(25);
        assertThat(json.get("min_battery_voltage").getAsLong()).isEqualTo(3200);
        assertThat(json.get("min_battery_mcu_temp").getAsInt()).isEqualTo(20);
        assertThat(json.get("battery_reset_timestamp").getAsString()).isEqualTo(formatDate(batteryResetTs));
        assertThat(json.get("max_mcu_temp").getAsInt()).isEqualTo(40);
        assertThat(json.get("min_mcu_temp").getAsInt()).isEqualTo(10);
        assertThat(json.get("counter_of_confirmable_messages_attempts").getAsInt()).isEqualTo(10);
        assertThat(json.get("counter_of_non_confirmable_messages_attempts").getAsInt()).isEqualTo(5);
        assertThat(json.get("counter_of_succeeded_messages").getAsInt()).isEqualTo(9);
        assertThat(json.get("runtime_errors").getAsInt()).isEqualTo(2); // count of errors, not values
    }

    @Test
    void getEfentoDeviceInfo_undefinedTimestampRenderedAsUndefinedString() {
        // -1 as signed int == 0xFFFFFFFF == uint32 max (4294967295), the "Undefined" sentinel
        DeviceInfoProtos.ProtoDeviceInfo deviceInfo = minimalDeviceInfo().clearMemoryStatistics()
                .addAllMemoryStatistics(List.of(
                        0, -1, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0,
                        -1, -1, -1,
                        -1, -1, -1,
                        0))
                .build();

        CoapEfentoTransportResource.EfentoTelemetry result = coapEfentoTransportResource.getEfentoDeviceInfo(deviceInfo);
        var json = result.getValues().getAsJsonObject();

        assertThat(json.get("timestamp_of_the_end_of_collecting_statistics").getAsString()).isEqualTo("Undefined");
        assertThat(json.get("timestamp_of_the_first_binary_measurement").getAsString()).isEqualTo("Undefined");
        assertThat(json.get("timestamp_of_the_last_binary_measurement").getAsString()).isEqualTo("Undefined");
        assertThat(json.get("timestamp_of_the_first_binary_measurement_sent").getAsString()).isEqualTo("Undefined");
        assertThat(json.get("timestamp_of_the_first_continuous_measurement").getAsString()).isEqualTo("Undefined");
        assertThat(json.get("timestamp_of_the_last_continuous_measurement").getAsString()).isEqualTo("Undefined");
        assertThat(json.get("timestamp_of_the_last_continuous_measurement_sent").getAsString()).isEqualTo("Undefined");
    }

    @Test
    void getEfentoDeviceInfo_tsIsCurrentTimeMillis() {
        long before = System.currentTimeMillis();
        CoapEfentoTransportResource.EfentoTelemetry result =
                coapEfentoTransportResource.getEfentoDeviceInfo(minimalDeviceInfo().build());
        long after = System.currentTimeMillis();

        assertThat(result.getTs()).isBetween(before, after);
    }

    // -------------------------------------------------------------------------
    // ProtoConfig / getEfentoConfiguration parsing tests
    // -------------------------------------------------------------------------

    @Test
    void getEfentoConfiguration_parsesServerCommunicationFields() throws InvalidProtocolBufferException {
        byte[] bytes = ConfigProtos.ProtoConfig.newBuilder()
                .setDataServerIp("18.184.24.239")
                .setDataServerPort(5683)
                .setUpdateServerIp("efento.update.io")
                .setUpdateServerPortCoap(5684)
                .setUpdateServerPortUdp(5685)
                .setTransmissionInterval(300)
                .setAckInterval(600)
                .setSupervisionPeriod(3600)
                .build()
                .toByteArray();

        var json = coapEfentoTransportResource.getEfentoConfiguration(bytes).getAsJsonObject();

        assertThat(json.get("dataServerIp").getAsString()).isEqualTo("18.184.24.239");
        assertThat(json.get("dataServerPort").getAsLong()).isEqualTo(5683);
        assertThat(json.get("updateServerIp").getAsString()).isEqualTo("efento.update.io");
        assertThat(json.get("updateServerPortCoap").getAsLong()).isEqualTo(5684);
        assertThat(json.get("updateServerPortUdp").getAsLong()).isEqualTo(5685);
        assertThat(json.get("transmissionInterval").getAsLong()).isEqualTo(300);
        assertThat(json.get("ackInterval").getAsLong()).isEqualTo(600);
        assertThat(json.get("supervisionPeriod").getAsLong()).isEqualTo(3600);
    }

    @Test
    void getEfentoConfiguration_parsesMeasurementPeriod() throws InvalidProtocolBufferException {
        byte[] bytes = ConfigProtos.ProtoConfig.newBuilder()
                .setMeasurementPeriodBase(60)
                .setMeasurementPeriodFactor(5)
                .build()
                .toByteArray();

        var json = coapEfentoTransportResource.getEfentoConfiguration(bytes).getAsJsonObject();

        assertThat(json.get("measurementPeriodBase").getAsLong()).isEqualTo(60);
        assertThat(json.get("measurementPeriodFactor").getAsLong()).isEqualTo(5);
    }

    @Test
    void getEfentoConfiguration_parsesBooleanRequestFields() throws InvalidProtocolBufferException {
        byte[] bytes = ConfigProtos.ProtoConfig.newBuilder()
                .setDeviceInfoRequest(true)
                .setUpdateSoftwareRequest(true)
                .setConfigurationRequest(true)
                .setAcceptWithoutTestingRequest(true)
                .setResetMemoryRequest(true)
                .build()
                .toByteArray();

        var json = coapEfentoTransportResource.getEfentoConfiguration(bytes).getAsJsonObject();

        assertThat(json.get("deviceInfoRequest").getAsBoolean()).isTrue();
        assertThat(json.get("updateSoftwareRequest").getAsBoolean()).isTrue();
        assertThat(json.get("configurationRequest").getAsBoolean()).isTrue();
        assertThat(json.get("acceptWithoutTestingRequest").getAsBoolean()).isTrue();
        assertThat(json.get("resetMemoryRequest").getAsBoolean()).isTrue();
    }

    @Test
    void getEfentoConfiguration_parsesModemAndNetworkFields() throws InvalidProtocolBufferException {
        byte[] bytes = ConfigProtos.ProtoConfig.newBuilder()
                .setApn("internet.example.com")
                .setApnUsername("user")
                .setApnPassword("pass")
                .setPlmnSelection(26001)
                .setModemBandsMask(2084) // bands 3, 8, 20
                .setNetworkTroubleshooting(2)
                .build()
                .toByteArray();

        var json = coapEfentoTransportResource.getEfentoConfiguration(bytes).getAsJsonObject();

        assertThat(json.get("apn").getAsString()).isEqualTo("internet.example.com");
        assertThat(json.get("apnUsername").getAsString()).isEqualTo("user");
        assertThat(json.get("apnPassword").getAsString()).isEqualTo("pass");
        assertThat(json.get("plmnSelection").getAsLong()).isEqualTo(26001);
        assertThat(json.get("modemBandsMask").getAsLong()).isEqualTo(2084);
        assertThat(json.get("networkTroubleshooting").getAsLong()).isEqualTo(2);
    }

    @Test
    void getEfentoConfiguration_parsesCloudTokenFields() throws InvalidProtocolBufferException {
        byte[] bytes = ConfigProtos.ProtoConfig.newBuilder()
                .setCloudToken("my-device-token")
                .setCloudTokenConfig(1)
                .setCloudTokenCoapOption(65000)
                .build()
                .toByteArray();

        var json = coapEfentoTransportResource.getEfentoConfiguration(bytes).getAsJsonObject();

        assertThat(json.get("cloudToken").getAsString()).isEqualTo("my-device-token");
        assertThat(json.get("cloudTokenConfig").getAsLong()).isEqualTo(1);
        assertThat(json.get("cloudTokenCoapOption").getAsLong()).isEqualTo(65000);
    }

    @Test
    void getEfentoConfiguration_parsesEndpointFields() throws InvalidProtocolBufferException {
        byte[] bytes = ConfigProtos.ProtoConfig.newBuilder()
                .setDataEndpoint("/m")
                .setConfigurationEndpoint("/c")
                .setDeviceInfoEndpoint("/i")
                .setTimeEndpoint("/t")
                .build()
                .toByteArray();

        var json = coapEfentoTransportResource.getEfentoConfiguration(bytes).getAsJsonObject();

        assertThat(json.get("dataEndpoint").getAsString()).isEqualTo("/m");
        assertThat(json.get("configurationEndpoint").getAsString()).isEqualTo("/c");
        assertThat(json.get("deviceInfoEndpoint").getAsString()).isEqualTo("/i");
        assertThat(json.get("timeEndpoint").getAsString()).isEqualTo("/t");
    }

    @Test
    void getEfentoConfiguration_parsesBleAdvertisingPeriod() throws InvalidProtocolBufferException {
        byte[] bytes = ConfigProtos.ProtoConfig.newBuilder()
                .setBleAdvertisingPeriod(ConfigTypesProtos.ProtoBleAdvertisingPeriod.newBuilder()
                        .setMode(ConfigTypesProtos.BleAdvertisingPeriodMode.BLE_ADVERTISING_PERIOD_MODE_NORMAL)
                        .setNormal(1600)
                        .setFast(320)
                        .build())
                .build()
                .toByteArray();

        var json = coapEfentoTransportResource.getEfentoConfiguration(bytes).getAsJsonObject();
        var ble = json.get("bleAdvertisingPeriod").getAsJsonObject();

        assertThat(ble.get("mode").getAsString()).isEqualTo("BLE_ADVERTISING_PERIOD_MODE_NORMAL");
        assertThat(ble.get("normal").getAsLong()).isEqualTo(1600);
        assertThat(ble.get("fast").getAsLong()).isEqualTo(320);
    }

    @Test
    void getEfentoConfiguration_parsesRepeatedChannelTypes() throws InvalidProtocolBufferException {
        byte[] bytes = ConfigProtos.ProtoConfig.newBuilder()
                .addChannelTypes(MeasurementType.MEASUREMENT_TYPE_TEMPERATURE)
                .addChannelTypes(MeasurementType.MEASUREMENT_TYPE_HUMIDITY)
                .addChannelTypes(MeasurementType.MEASUREMENT_TYPE_ATMOSPHERIC_PRESSURE)
                .build()
                .toByteArray();

        var json = coapEfentoTransportResource.getEfentoConfiguration(bytes).getAsJsonObject();
        var channelTypes = json.get("channelTypes").getAsJsonArray();

        assertThat(channelTypes).hasSize(3);
        assertThat(channelTypes.get(0).getAsString()).isEqualTo("MEASUREMENT_TYPE_TEMPERATURE");
        assertThat(channelTypes.get(1).getAsString()).isEqualTo("MEASUREMENT_TYPE_HUMIDITY");
        assertThat(channelTypes.get(2).getAsString()).isEqualTo("MEASUREMENT_TYPE_ATMOSPHERIC_PRESSURE");
    }

    @Test
    void getEfentoConfiguration_parsesEdgeLogicRules() throws InvalidProtocolBufferException {
        // ProtoRule uses channel_mask (bit mask), condition, parameters, action
        byte[] bytes = ConfigProtos.ProtoConfig.newBuilder()
                .addRules(ProtoRuleProtos.ProtoRule.newBuilder()
                        .setChannelMask(1)  // channel 1 → bit mask 0b000001
                        .setCondition(ProtoRuleProtos.Condition.CONDITION_HIGH_THRESHOLD)
                        .addParameters(500)  // threshold value
                        .setAction(ProtoRuleProtos.Action.ACTION_TRIGGER_TRANSMISSION)
                        .build())
                .build()
                .toByteArray();

        var json = coapEfentoTransportResource.getEfentoConfiguration(bytes).getAsJsonObject();
        var rules = json.get("rules").getAsJsonArray();

        assertThat(rules).hasSize(1);
        var rule = rules.get(0).getAsJsonObject();
        assertThat(rule.get("channelMask").getAsInt()).isEqualTo(1);
        assertThat(rule.get("condition").getAsString()).isEqualTo("CONDITION_HIGH_THRESHOLD");
        assertThat(rule.get("parameters").getAsJsonArray().get(0).getAsInt()).isEqualTo(500);
        assertThat(rule.get("action").getAsString()).isEqualTo("ACTION_TRIGGER_TRANSMISSION");
    }

    @Test
    void getEfentoConfiguration_emptyConfigProducesJsonWithDefaultValues() throws InvalidProtocolBufferException {
        byte[] bytes = ConfigProtos.ProtoConfig.newBuilder().build().toByteArray();

        var json = coapEfentoTransportResource.getEfentoConfiguration(bytes).getAsJsonObject();

        // JsonFormat with includingDefaultValueFields prints all fields — verify a representative subset
        assertThat(json.get("measurementPeriodBase").getAsLong()).isEqualTo(0);
        assertThat(json.get("transmissionInterval").getAsLong()).isEqualTo(0);
        assertThat(json.get("dataServerIp").getAsString()).isEmpty();
        assertThat(json.get("deviceInfoRequest").getAsBoolean()).isFalse();
        assertThat(json.get("cloudToken").getAsString()).isEmpty();
    }

    /**
     * Builds a ProtoDeviceInfo with the minimum set of repeated fields required by getEfentoDeviceInfo:
     * 22 memory_statistics, 34 modem parameters and 3 message_counters — all set to 0.
     */
    private static DeviceInfoProtos.ProtoDeviceInfo.Builder minimalDeviceInfo() {
        List<Integer> zeroMemStats = java.util.Collections.nCopies(22, 0);
        List<Integer> zeroModemParams = java.util.Collections.nCopies(34, 0);
        return DeviceInfoProtos.ProtoDeviceInfo.newBuilder()
                .setSerialNumber(integerToByteString(1234))
                .setCloudToken("test_token")
                .setSwVersion(0)
                .addAllMemoryStatistics(zeroMemStats)
                .setModem(DeviceInfoProtos.ProtoModem.newBuilder()
                        .setType(DeviceInfoProtos.ModemType.MODEM_TYPE_UNSPECIFIED)
                        .addAllParameters(zeroModemParams)
                        .build())
                .setRuntimeInfo(DeviceInfoProtos.ProtoRuntime.newBuilder()
                        .setUpTime(0)
                        .addAllMessageCounters(List.of(0, 0, 0))
                        .build());
    }

    private static String formatDate(long seconds) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm:ss Z");
        return sdf.format(new Date(TimeUnit.SECONDS.toMillis(seconds)));
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
                                          long expectedMeasurementInterval) {
        CoapEfentoTransportResource.EfentoTelemetry efentoTelemetry = actualEfentoMeasurements.stream()
                .sorted(Comparator.comparing(CoapEfentoTransportResource.EfentoTelemetry::getTs))
                .toList().get(0);

        assertThat(efentoTelemetry.getValues().getAsJsonObject().get("serial").getAsString()).isEqualTo(CoapEfentoUtils.convertByteArrayToString(incomingMeasurements.getSerialNumber().toByteArray()));
        assertThat(efentoTelemetry.getValues().getAsJsonObject().get("battery").getAsString()).isEqualTo(incomingMeasurements.getBatteryStatus() ? "ok" : "low");
        assertThat(efentoTelemetry.getValues().getAsJsonObject().get("next_transmission_at").getAsString()).isEqualTo(convertTimestampToUtcString(TimeUnit.SECONDS.toMillis(incomingMeasurements.getNextTransmissionAt())));
        assertThat(efentoTelemetry.getValues().getAsJsonObject().get("signal").getAsLong()).isEqualTo(incomingMeasurements.getSignal());
        for (int i = 1; i < incomingMeasurements.getChannelsCount() + 1; i++) {
            assertThat(efentoTelemetry.getValues().getAsJsonObject().get("measurement_interval_" + i).getAsDouble()).isEqualTo(expectedMeasurementInterval);
        }
    }

}
