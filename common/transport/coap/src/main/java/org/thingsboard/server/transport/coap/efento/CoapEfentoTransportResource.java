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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.TriConsumer;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.core.server.resources.Resource;
import org.springframework.util.CollectionUtils;
import org.thingsboard.server.common.adaptor.AdaptorException;
import org.thingsboard.server.common.adaptor.ProtoConverter;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.device.profile.CoapDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.EfentoCoapDeviceTypeConfiguration;
import org.thingsboard.server.common.transport.auth.SessionInfoCreator;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.coap.ConfigProtos;
import org.thingsboard.server.gen.transport.coap.DeviceInfoProtos;
import org.thingsboard.server.gen.transport.coap.MeasurementsProtos;
import org.thingsboard.server.gen.transport.coap.MeasurementsProtos.ProtoChannel;
import org.thingsboard.server.transport.coap.AbstractCoapTransportResource;
import org.thingsboard.server.transport.coap.CoapTransportContext;
import org.thingsboard.server.transport.coap.callback.CoapDeviceAuthCallback;
import org.thingsboard.server.transport.coap.callback.CoapEfentoCallback;
import org.thingsboard.server.transport.coap.efento.utils.CoapEfentoUtils;
import org.thingsboard.server.transport.coap.efento.utils.PulseCounterType;

import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.google.gson.JsonParser.parseString;
import static org.thingsboard.server.transport.coap.CoapTransportService.CONFIGURATION;
import static org.thingsboard.server.transport.coap.CoapTransportService.CURRENT_TIMESTAMP;
import static org.thingsboard.server.transport.coap.CoapTransportService.DEVICE_INFO;
import static org.thingsboard.server.transport.coap.CoapTransportService.MEASUREMENTS;
import static org.thingsboard.server.transport.coap.efento.utils.CoapEfentoUtils.BREATH_VOC_METADATA_FACTOR;
import static org.thingsboard.server.transport.coap.efento.utils.CoapEfentoUtils.CO2_EQUIVALENT_METADATA_FACTOR;
import static org.thingsboard.server.transport.coap.efento.utils.CoapEfentoUtils.CO2_GAS_METADATA_FACTOR;
import static org.thingsboard.server.transport.coap.efento.utils.CoapEfentoUtils.ELEC_METER_ACC_MAJOR_METADATA_FACTOR;
import static org.thingsboard.server.transport.coap.efento.utils.CoapEfentoUtils.ELEC_METER_ACC_MINOR_METADATA_FACTOR;
import static org.thingsboard.server.transport.coap.efento.utils.CoapEfentoUtils.IAQ_METADATA_FACTOR;
import static org.thingsboard.server.transport.coap.efento.utils.CoapEfentoUtils.PULSE_CNT_ACC_MAJOR_METADATA_FACTOR;
import static org.thingsboard.server.transport.coap.efento.utils.CoapEfentoUtils.PULSE_CNT_ACC_MINOR_METADATA_FACTOR;
import static org.thingsboard.server.transport.coap.efento.utils.CoapEfentoUtils.PULSE_CNT_ACC_WIDE_MAJOR_METADATA_FACTOR;
import static org.thingsboard.server.transport.coap.efento.utils.CoapEfentoUtils.PULSE_CNT_ACC_WIDE_MINOR_METADATA_FACTOR;
import static org.thingsboard.server.transport.coap.efento.utils.CoapEfentoUtils.STATIC_IAQ_METADATA_FACTOR;
import static org.thingsboard.server.transport.coap.efento.utils.CoapEfentoUtils.WATER_METER_ACC_MAJOR_METADATA_FACTOR;
import static org.thingsboard.server.transport.coap.efento.utils.CoapEfentoUtils.WATER_METER_ACC_MINOR_METADATA_FACTOR;
import static org.thingsboard.server.transport.coap.efento.utils.CoapEfentoUtils.isBinarySensor;
import static org.thingsboard.server.transport.coap.efento.utils.CoapEfentoUtils.isSensorError;
import static org.thingsboard.server.transport.coap.efento.utils.PulseCounterType.ELEC_METER_ACC;
import static org.thingsboard.server.transport.coap.efento.utils.PulseCounterType.PULSE_CNT_ACC;
import static org.thingsboard.server.transport.coap.efento.utils.PulseCounterType.PULSE_CNT_ACC_WIDE;
import static org.thingsboard.server.transport.coap.efento.utils.PulseCounterType.WATER_CNT_ACC;

@Slf4j
public class CoapEfentoTransportResource extends AbstractCoapTransportResource {

    private static final int CHILD_RESOURCE_POSITION = 2;

    public CoapEfentoTransportResource(CoapTransportContext context, String name) {
        super(context, name);
        this.setObservable(true); // enable observing
        this.setObserveType(CoAP.Type.CON); // configure the notification type to CONs
//        this.getAttributes().setObservable(); // mark observable in the Link-Format
    }

    @Override
    protected void processHandleGet(CoapExchange exchange) {
        Exchange advanced = exchange.advanced();
        Request request = advanced.getRequest();
        List<String> uriPath = request.getOptions().getUriPath();
        boolean validPath = uriPath.size() == CHILD_RESOURCE_POSITION && uriPath.get(1).equals(CURRENT_TIMESTAMP);
        if (!validPath) {
            log.trace("Invalid path: [{}]", uriPath);
            exchange.respond(CoAP.ResponseCode.BAD_REQUEST);
        } else {
            int dateInSec = (int) (System.currentTimeMillis() / 1000);
            byte[] bytes = ByteBuffer.allocate(4).putInt(dateInSec).array();
            exchange.respond(CoAP.ResponseCode.CONTENT, bytes);
        }
    }

    @Override
    protected void processHandlePost(CoapExchange exchange) {
        Exchange advanced = exchange.advanced();
        Request request = advanced.getRequest();
        List<String> uriPath = request.getOptions().getUriPath();
        if (uriPath.size() != CHILD_RESOURCE_POSITION) {
            log.trace("Unexpected uri path size, uri path: [{}]", uriPath);
            exchange.respond(CoAP.ResponseCode.BAD_REQUEST);
            return;
        }
        String requestType = uriPath.get(1);
        switch (requestType) {
            case MEASUREMENTS:
                processMeasurementsRequest(exchange);
                break;
            case DEVICE_INFO:
                processDeviceInfoRequest(exchange);
                break;
            case CONFIGURATION:
                processConfigurationRequest(exchange);
                break;
            default:
                log.trace("Unexpected request type: [{}]", requestType);
                exchange.respond(CoAP.ResponseCode.BAD_REQUEST);
                break;
        }
    }

    private void processMeasurementsRequest(CoapExchange exchange) {
        byte[] bytes = exchange.advanced().getRequest().getPayload();
        try {
            MeasurementsProtos.ProtoMeasurements protoMeasurements = MeasurementsProtos.ProtoMeasurements.parseFrom(bytes);
            log.trace("Successfully parsed Efento ProtoMeasurements: [{}]", protoMeasurements.getCloudToken());
            validateAndProcessEffentoMessage(protoMeasurements.getCloudToken(), exchange, (deviceProfile, sessionInfo, sessionId) -> {
                try {
                    List<EfentoTelemetry> measurements = getEfentoMeasurements(protoMeasurements, sessionId);
                    transportService.process(sessionInfo,
                            transportContext.getEfentoCoapAdaptor().convertToPostTelemetry(sessionId, measurements),
                            new CoapEfentoCallback(exchange, CoAP.ResponseCode.CREATED, CoAP.ResponseCode.INTERNAL_SERVER_ERROR));
                } catch (AdaptorException e) {
                    log.error("[{}] Failed to decode Efento ProtoMeasurements: ", sessionId, e);
                    exchange.respond(CoAP.ResponseCode.BAD_REQUEST);
                }
            });
        } catch (Exception e) {
            log.error("Failed to decode Efento ProtoMeasurements: ", e);
            exchange.respond(CoAP.ResponseCode.INTERNAL_SERVER_ERROR);
        }
    }

    private void processDeviceInfoRequest(CoapExchange exchange) {
        byte[] bytes = exchange.advanced().getRequest().getPayload();
        try {
            DeviceInfoProtos.ProtoDeviceInfo protoDeviceInfo = DeviceInfoProtos.ProtoDeviceInfo.parseFrom(bytes);
            String token = protoDeviceInfo.getCloudToken();
            log.trace("Successfully parsed Efento ProtoDeviceInfo: [{}]", token);
            validateAndProcessEffentoMessage(token, exchange, (deviceProfile, sessionInfo, sessionId) -> {
                try {
                    EfentoTelemetry deviceInfo = getEfentoDeviceInfo(protoDeviceInfo);
                    transportService.process(sessionInfo,
                            transportContext.getEfentoCoapAdaptor().convertToPostTelemetry(sessionId, List.of(deviceInfo)),
                            new CoapEfentoCallback(exchange, CoAP.ResponseCode.CREATED, CoAP.ResponseCode.INTERNAL_SERVER_ERROR));
                } catch (AdaptorException e) {
                    log.error("[{}] Failed to decode Efento ProtoDeviceInfo: ", sessionId, e);
                    exchange.respond(CoAP.ResponseCode.BAD_REQUEST);
                }
            });
        } catch (Exception e) {
            log.error("Failed to decode Efento ProtoDeviceInfo: ", e);
            exchange.respond(CoAP.ResponseCode.INTERNAL_SERVER_ERROR);
        }
    }

    private void processConfigurationRequest(CoapExchange exchange) {
        byte[] bytes = exchange.advanced().getRequest().getPayload();
        try {
            ConfigProtos.ProtoConfig protoConfig = ConfigProtos.ProtoConfig.parseFrom(bytes);
            String token = protoConfig.getCloudToken();
            log.trace("Successfully parsed Efento ProtoConfig: [{}]", token);
            validateAndProcessEffentoMessage(token, exchange, (deviceProfile, sessionInfo, sessionId) -> {
                try {
                    JsonElement configuration = getEfentoConfiguration(bytes);
                    transportService.process(sessionInfo,
                            transportContext.getEfentoCoapAdaptor().convertToPostAttributes(sessionId, configuration),
                            new CoapEfentoCallback(exchange, CoAP.ResponseCode.CREATED, CoAP.ResponseCode.INTERNAL_SERVER_ERROR));
                } catch (AdaptorException e) {
                    log.error("[{}] Failed to decode Efento ProtoConfig: ", sessionId, e);
                    exchange.respond(CoAP.ResponseCode.BAD_REQUEST);
                } catch (InvalidProtocolBufferException e) {
                    log.error("[{}] Error while processing efento message: ", sessionId, e);
                    throw new RuntimeException(e);
                }
            });
        } catch (Exception e) {
            log.error("Failed to decode Efento ProtoConfig: ", e);
            exchange.respond(CoAP.ResponseCode.INTERNAL_SERVER_ERROR);
        }
    }

    private void validateAndProcessEffentoMessage(String token, CoapExchange exchange, TriConsumer<DeviceProfile, TransportProtos.SessionInfoProto, UUID> requestProcessor) {
        transportService.process(DeviceTransportType.COAP, TransportProtos.ValidateDeviceTokenRequestMsg.newBuilder().setToken(token).build(),
                new CoapDeviceAuthCallback(exchange, (msg, deviceProfile) -> {
                    TransportProtos.SessionInfoProto sessionInfo = SessionInfoCreator.create(msg, transportContext, UUID.randomUUID());
                    UUID sessionId = new UUID(sessionInfo.getSessionIdMSB(), sessionInfo.getSessionIdLSB());
                    try {
                        validateEfentoTransportConfiguration(deviceProfile);
                        requestProcessor.accept(deviceProfile, sessionInfo, sessionId);
                        reportSubscriptionInfo(sessionInfo, false, false);
                    } catch (AdaptorException e) {
                        log.error("[{}] Failed to decode Efento request: ", sessionId, e);
                        exchange.respond(CoAP.ResponseCode.BAD_REQUEST);
                    }
                }));
    }

    @Override
    public Resource getChild(String name) {
        return this;
    }

    private void validateEfentoTransportConfiguration(DeviceProfile deviceProfile) throws AdaptorException {
        DeviceProfileTransportConfiguration transportConfiguration = deviceProfile.getProfileData().getTransportConfiguration();
        if (transportConfiguration instanceof CoapDeviceProfileTransportConfiguration) {
            CoapDeviceProfileTransportConfiguration coapDeviceProfileTransportConfiguration =
                    (CoapDeviceProfileTransportConfiguration) transportConfiguration;
            if (!(coapDeviceProfileTransportConfiguration.getCoapDeviceTypeConfiguration() instanceof EfentoCoapDeviceTypeConfiguration)) {
                throw new AdaptorException("Invalid CoapDeviceTypeConfiguration type: " + coapDeviceProfileTransportConfiguration.getCoapDeviceTypeConfiguration().getClass().getSimpleName() + "!");
            }
        } else {
            throw new AdaptorException("Invalid DeviceProfileTransportConfiguration type" + transportConfiguration.getClass().getSimpleName() + "!");
        }
    }

    List<EfentoTelemetry> getEfentoMeasurements(MeasurementsProtos.ProtoMeasurements protoMeasurements, UUID sessionId) {
        String serialNumber = CoapEfentoUtils.convertByteArrayToString(protoMeasurements.getSerialNum().toByteArray());
        boolean batteryStatus = protoMeasurements.getBatteryStatus();
        int measurementPeriodBase = protoMeasurements.getMeasurementPeriodBase();
        int measurementPeriodFactor = protoMeasurements.getMeasurementPeriodFactor();
        int signal = protoMeasurements.getSignal();
        long nextTransmissionAtMillis = TimeUnit.SECONDS.toMillis(protoMeasurements.getNextTransmissionAt());

        List<ProtoChannel> channelsList = protoMeasurements.getChannelsList();
        if (CollectionUtils.isEmpty(channelsList)) {
            throw new IllegalStateException("[" + sessionId + "]: Failed to get Efento measurements, reason: channels list is empty!");
        }

        Map<Long, JsonObject> valuesMap = new TreeMap<>();
        for (int channel = 0; channel < channelsList.size(); channel++) {
            ProtoChannel protoChannel = channelsList.get(channel);
            List<Integer> sampleOffsetsList = protoChannel.getSampleOffsetsList();
            if (CollectionUtils.isEmpty(sampleOffsetsList)) {
                log.trace("[{}][{}] sampleOffsetsList list is empty!", sessionId, protoChannel.getType().name());
                continue;
            }
            boolean isBinarySensor = isBinarySensor(protoChannel.getType());
            int channelPeriodFactor = (measurementPeriodFactor == 0 ? (isBinarySensor ? 14 : 1) : measurementPeriodFactor);
            int measurementPeriod = measurementPeriodBase * channelPeriodFactor;
            long measurementPeriodMillis = TimeUnit.SECONDS.toMillis(measurementPeriod);
            long startTimestampMillis = TimeUnit.SECONDS.toMillis(protoChannel.getTimestamp());

            for (int i = 0; i < sampleOffsetsList.size(); i++) {
                int sampleOffset = sampleOffsetsList.get(i);
                if (isSensorError(sampleOffset)) {
                    log.warn("[{}],[{}] Sensor error value! Ignoring.", sessionId, sampleOffset);
                    continue;
                }

                JsonObject values;
                if (isBinarySensor) {
                    boolean currentIsOk = sampleOffset < 0;
                    Integer previousSampleOffset = i > 0 ? sampleOffsetsList.get(i - 1) : null;
                    if (previousSampleOffset != null) {  //compare with previous value
                        boolean previousIsOk = previousSampleOffset < 0;
                        if (currentIsOk == previousIsOk) {
                            break;
                        }
                    }
                    long sampleOffsetMillis = TimeUnit.SECONDS.toMillis(sampleOffset);
                    long measurementTimestamp = startTimestampMillis + Math.abs(sampleOffsetMillis);
                    values = valuesMap.computeIfAbsent(measurementTimestamp - 1000, k ->
                            CoapEfentoUtils.setDefaultMeasurements(serialNumber, batteryStatus, measurementPeriod, nextTransmissionAtMillis, signal, k));
                    addBinarySample(protoChannel, currentIsOk, values, channel + 1, sessionId);
                } else {
                    long timestampMillis = startTimestampMillis + i * measurementPeriodMillis;
                    values = valuesMap.computeIfAbsent(timestampMillis, k -> CoapEfentoUtils.setDefaultMeasurements(
                            serialNumber, batteryStatus, measurementPeriod, nextTransmissionAtMillis, signal, k));
                    addContinuesSample(protoChannel, sampleOffset, values, channel + 1, sessionId);
                }
            }
        }

        if (CollectionUtils.isEmpty(valuesMap)) {
            throw new IllegalStateException("[" + sessionId + "]: Failed to collect Efento measurements, reason, values map is empty!");
        }

        return valuesMap.entrySet().stream()
                .map(entry -> new EfentoTelemetry(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    private void addContinuesSample(ProtoChannel protoChannel, int sampleOffset, JsonObject values, int channelNumber, UUID sessionId) {
        int startPoint = protoChannel.getStartPoint();

        switch (protoChannel.getType()) {
            case MEASUREMENT_TYPE_TEMPERATURE:
                values.addProperty("temperature_" + channelNumber, ((double) (startPoint + sampleOffset)) / 10f);
                break;
            case MEASUREMENT_TYPE_WATER_METER:
                values.addProperty("pulse_counter_water_" + channelNumber, ((double) (startPoint + sampleOffset)));
                break;
            case MEASUREMENT_TYPE_HUMIDITY:
                values.addProperty("humidity_" + channelNumber, (double) (startPoint + sampleOffset));
                break;
            case MEASUREMENT_TYPE_ATMOSPHERIC_PRESSURE:
                values.addProperty("pressure_" + channelNumber, (double) (startPoint + sampleOffset) / 10f);
                break;
            case MEASUREMENT_TYPE_DIFFERENTIAL_PRESSURE:
                values.addProperty("pressure_diff_" + channelNumber, (double) (startPoint + sampleOffset));
                break;
            case MEASUREMENT_TYPE_PULSE_CNT:
                values.addProperty("pulse_cnt_" + channelNumber, (double) (startPoint + sampleOffset));
                break;
            case MEASUREMENT_TYPE_IAQ:
                addPropertiesForMeasurementTypeWithMetadataFactor(values, "iaq_", channelNumber, startPoint + sampleOffset, IAQ_METADATA_FACTOR);
                break;
            case MEASUREMENT_TYPE_ELECTRICITY_METER:
                values.addProperty("watt_hour_" + channelNumber, (double) (startPoint + sampleOffset));
                break;
            case MEASUREMENT_TYPE_SOIL_MOISTURE:
                values.addProperty("soil_moisture_" + channelNumber, (double) (startPoint + sampleOffset));
                break;
            case MEASUREMENT_TYPE_AMBIENT_LIGHT:
                values.addProperty("ambient_light_" + channelNumber, (double) (startPoint + sampleOffset) / 10f);
                break;
            case MEASUREMENT_TYPE_HIGH_PRESSURE:
                values.addProperty("high_pressure_" + channelNumber, (double) (startPoint + sampleOffset));
                break;
            case MEASUREMENT_TYPE_DISTANCE_MM:
                values.addProperty("distance_mm_" + channelNumber, (double) (startPoint + sampleOffset));
                break;
            case MEASUREMENT_TYPE_WATER_METER_ACC_MINOR:
                calculateAccPulseCounterTotalValue(values, WATER_CNT_ACC , channelNumber, startPoint + sampleOffset, WATER_METER_ACC_MINOR_METADATA_FACTOR);
                break;
            case MEASUREMENT_TYPE_WATER_METER_ACC_MAJOR:
                addPropertiesForMeasurementTypeWithMetadataFactor(values, WATER_CNT_ACC.getPrefix(), channelNumber, startPoint + sampleOffset, WATER_METER_ACC_MAJOR_METADATA_FACTOR);
                break;
            case MEASUREMENT_TYPE_HUMIDITY_ACCURATE:
                values.addProperty("humidity_relative_" + channelNumber, (double) (startPoint + sampleOffset) / 10f);
                break;
            case MEASUREMENT_TYPE_STATIC_IAQ:
                addPropertiesForMeasurementTypeWithMetadataFactor(values, "static_iaq_", channelNumber, startPoint + sampleOffset, STATIC_IAQ_METADATA_FACTOR);
                break;
            case MEASUREMENT_TYPE_CO2_GAS:
                addPropertiesForMeasurementTypeWithMetadataFactor(values, "co2_gas_", channelNumber, startPoint + sampleOffset, CO2_GAS_METADATA_FACTOR);
                break;
            case MEASUREMENT_TYPE_CO2_EQUIVALENT:
                addPropertiesForMeasurementTypeWithMetadataFactor(values, "co2_", channelNumber, startPoint + sampleOffset, CO2_EQUIVALENT_METADATA_FACTOR);
                break;
            case MEASUREMENT_TYPE_BREATH_VOC:
                addPropertiesForMeasurementTypeWithMetadataFactor(values, "breath_voc_", channelNumber, startPoint + sampleOffset, BREATH_VOC_METADATA_FACTOR);
                break;
            case MEASUREMENT_TYPE_PERCENTAGE:
                values.addProperty("percentage_" + channelNumber, (double) (startPoint + sampleOffset) / 100f);
                break;
            case MEASUREMENT_TYPE_VOLTAGE:
                values.addProperty("voltage_" + channelNumber, (double) (startPoint + sampleOffset) / 10f);
                break;
            case MEASUREMENT_TYPE_CURRENT:
                values.addProperty("current_" + channelNumber, (double) (startPoint + sampleOffset) / 100f);
                break;
            case MEASUREMENT_TYPE_PULSE_CNT_ACC_MINOR:
                calculateAccPulseCounterTotalValue(values, PULSE_CNT_ACC , channelNumber, startPoint + sampleOffset, PULSE_CNT_ACC_MINOR_METADATA_FACTOR);
                break;
            case MEASUREMENT_TYPE_PULSE_CNT_ACC_MAJOR:
                addPropertiesForMeasurementTypeWithMetadataFactor(values, PULSE_CNT_ACC.getPrefix(), channelNumber, startPoint + sampleOffset, PULSE_CNT_ACC_MAJOR_METADATA_FACTOR);
                break;
            case MEASUREMENT_TYPE_ELEC_METER_ACC_MINOR:
                calculateAccPulseCounterTotalValue(values, ELEC_METER_ACC , channelNumber, startPoint + sampleOffset, ELEC_METER_ACC_MINOR_METADATA_FACTOR);
                break;
            case MEASUREMENT_TYPE_ELEC_METER_ACC_MAJOR:
                addPropertiesForMeasurementTypeWithMetadataFactor(values, ELEC_METER_ACC.getPrefix(), channelNumber, startPoint + sampleOffset, ELEC_METER_ACC_MAJOR_METADATA_FACTOR);
                break;
            case MEASUREMENT_TYPE_PULSE_CNT_ACC_WIDE_MINOR:
                calculateAccPulseCounterTotalValue(values, PULSE_CNT_ACC_WIDE , channelNumber, startPoint + sampleOffset, PULSE_CNT_ACC_WIDE_MINOR_METADATA_FACTOR);
                break;
            case MEASUREMENT_TYPE_PULSE_CNT_ACC_WIDE_MAJOR:
                addPropertiesForMeasurementTypeWithMetadataFactor(values, PULSE_CNT_ACC_WIDE.getPrefix(), channelNumber, startPoint + sampleOffset, PULSE_CNT_ACC_WIDE_MAJOR_METADATA_FACTOR);
                break;
            case MEASUREMENT_TYPE_CURRENT_PRECISE:
                values.addProperty("current_precise_" + channelNumber, (double) (startPoint + sampleOffset) / 1000f);
                break;
            case MEASUREMENT_TYPE_NO_SENSOR:
            case UNRECOGNIZED:
                log.trace("[{}][{}] Sensor error value! Ignoring.", sessionId, protoChannel.getType().name());
                break;
            default:
                log.trace("[{}],[{}] Unsupported measurementType! Ignoring.", sessionId, protoChannel.getType().name());
                break;
        }
    }

    private void addPropertiesForMeasurementTypeWithMetadataFactor(JsonObject values, String prefix, int channelNumber, int value, int metadataFactor) {
        values.addProperty(prefix + channelNumber, value / metadataFactor);
        values.addProperty(prefix + "metadata_" + channelNumber, value % metadataFactor);
    }

    private void calculateAccPulseCounterTotalValue(JsonObject values, PulseCounterType pulseCounterType, int channelNumber, int value, int metadataFactor) {
        int minorValue = value / metadataFactor;
        int majorChannel = value % metadataFactor + 1;
        String majorPropertyKey = pulseCounterType.getPrefix() + majorChannel;
        JsonElement majorProperty = values.get(majorPropertyKey);
        if (majorProperty != null) {
            int totalValue = majorProperty.getAsInt() * pulseCounterType.getMajorResolution() + minorValue;
            values.addProperty(pulseCounterType.getPrefix() + "total_" + channelNumber, totalValue);
            values.remove(majorPropertyKey);
        }
    }

    private void addBinarySample(ProtoChannel protoChannel, boolean valueIsOk, JsonObject values, int channel, UUID sessionId) {
        switch (protoChannel.getType()) {
            case MEASUREMENT_TYPE_OK_ALARM:
                values.addProperty("ok_alarm_" + channel, valueIsOk ? "OK" : "ALARM");
                break;
            case MEASUREMENT_TYPE_FLOODING:
                values.addProperty("flooding_" + channel, valueIsOk ? "OK" : "WATER_DETECTED");
                break;
            case MEASUREMENT_TYPE_OUTPUT_CONTROL:
                values.addProperty("output_control_" + channel, valueIsOk ? "OFF" : "ON");
                break;
            default:
                log.trace("[{}],[{}] Unsupported binary measurementType! Ignoring.", sessionId, protoChannel.getType().name());
                break;
        }
    }

    private EfentoTelemetry getEfentoDeviceInfo(DeviceInfoProtos.ProtoDeviceInfo protoDeviceInfo) {
        JsonObject values = new JsonObject();
        values.addProperty("sw_version", protoDeviceInfo.getSwVersion());

        //memory statistics
        values.addProperty("nv_storage_status", protoDeviceInfo.getMemoryStatistics(0));
        values.addProperty("timestamp_of_the_end_of_collecting_statistics", getDate(protoDeviceInfo.getMemoryStatistics(1)));
        values.addProperty("capacity_of_memory_in_bytes", protoDeviceInfo.getMemoryStatistics(2));
        values.addProperty("used_space_in_bytes", protoDeviceInfo.getMemoryStatistics(3));
        values.addProperty("size_of_invalid_packets_in_bytes", protoDeviceInfo.getMemoryStatistics(4));
        values.addProperty("size_of_corrupted_packets_in_bytes", protoDeviceInfo.getMemoryStatistics(5));
        values.addProperty("number_of_valid_packets", protoDeviceInfo.getMemoryStatistics(6));
        values.addProperty("number_of_invalid_packets", protoDeviceInfo.getMemoryStatistics(7));
        values.addProperty("number_of_corrupted_packets", protoDeviceInfo.getMemoryStatistics(8));
        values.addProperty("number_of_all_samples_for_channel_1", protoDeviceInfo.getMemoryStatistics(9));
        values.addProperty("number_of_all_samples_for_channel_2", protoDeviceInfo.getMemoryStatistics(10));
        values.addProperty("number_of_all_samples_for_channel_3", protoDeviceInfo.getMemoryStatistics(11));
        values.addProperty("number_of_all_samples_for_channel_4", protoDeviceInfo.getMemoryStatistics(12));
        values.addProperty("number_of_all_samples_for_channel_5", protoDeviceInfo.getMemoryStatistics(13));
        values.addProperty("number_of_all_samples_for_channel_6", protoDeviceInfo.getMemoryStatistics(14));
        values.addProperty("timestamp_of_the_first_binary_measurement", getDate(protoDeviceInfo.getMemoryStatistics(15)));
        values.addProperty("timestamp_of_the_last_binary_measurement", getDate(protoDeviceInfo.getMemoryStatistics(16)));
        values.addProperty("timestamp_of_the_first_binary_measurement_sent", getDate(protoDeviceInfo.getMemoryStatistics(17)));
        values.addProperty("timestamp_of_the_first_continuous_measurement", getDate(protoDeviceInfo.getMemoryStatistics(18)));
        values.addProperty("timestamp_of_the_last_continuous_measurement", getDate(protoDeviceInfo.getMemoryStatistics(19)));
        values.addProperty("timestamp_of_the_last_continuous_measurement_sent", getDate(protoDeviceInfo.getMemoryStatistics(20)));
        values.addProperty("nvm_write_counter", protoDeviceInfo.getMemoryStatistics(21));

        //modem info
        DeviceInfoProtos.ProtoModem modem = protoDeviceInfo.getModem();
        values.addProperty("modem_types", modem.getType().toString());
        values.addProperty("sc_EARNFCN_offset", modem.getParameters(0));
        values.addProperty("sc_EARFCN", modem.getParameters(1));
        values.addProperty("sc_PCI", modem.getParameters(2));
        values.addProperty("sc_Cell_id", modem.getParameters(3));
        values.addProperty("sc_RSRP", modem.getParameters(4));
        values.addProperty("sc_RSRQ", modem.getParameters(5));
        values.addProperty("sc_RSSI", modem.getParameters(6));
        values.addProperty("sc_SINR", modem.getParameters(7));
        values.addProperty("sc_Band", modem.getParameters(8));
        values.addProperty("sc_TAC", modem.getParameters(9));
        values.addProperty("sc_ECL", modem.getParameters(10));
        values.addProperty("sc_TX_PWR", modem.getParameters(11));
        values.addProperty("op_mode", modem.getParameters(12));
        values.addProperty("nc_EARFCN", modem.getParameters(13));
        values.addProperty("nc_EARNFCN_offset", modem.getParameters(14));
        values.addProperty("nc_PCI", modem.getParameters(15));
        values.addProperty("nc_RSRP", modem.getParameters(16));
        values.addProperty("RLC_UL_BLER", modem.getParameters(17));
        values.addProperty("RLC_DL_BLER", modem.getParameters(18));
        values.addProperty("MAC_UL_BLER", modem.getParameters(19));
        values.addProperty("MAC_DL_BLER", modem.getParameters(20));
        values.addProperty("MAC_UL_TOTAL_BYTES", modem.getParameters(21));
        values.addProperty("MAC_DL_TOTAL_BYTES", modem.getParameters(22));
        values.addProperty("MAC_UL_total_HARQ_Tx", modem.getParameters(23));
        values.addProperty("MAC_DL_total_HARQ_Tx", modem.getParameters(24));
        values.addProperty("MAC_UL_HARQ_re_Tx", modem.getParameters(25));
        values.addProperty("MAC_DL_HARQ_re_Tx", modem.getParameters(26));
        values.addProperty("RLC_UL_tput", modem.getParameters(27));
        values.addProperty("RLC_DL_tput", modem.getParameters(28));
        values.addProperty("MAC_UL_tput", modem.getParameters(29));
        values.addProperty("MAC_DL_tput", modem.getParameters(30));
        values.addProperty("sleep_duration", modem.getParameters(31));
        values.addProperty("rx_time", modem.getParameters(32));
        values.addProperty("tx_time", modem.getParameters(33));

        //Runtime info
        DeviceInfoProtos.ProtoRuntime runtimeInfo = protoDeviceInfo.getRuntimeInfo();
        values.addProperty("battery_reset_timestamp", getDate(runtimeInfo.getBatteryResetTimestamp()));
        values.addProperty("max_mcu_temp", runtimeInfo.getMaxMcuTemperature());
        values.addProperty("mcu_temp", runtimeInfo.getMcuTemperature());
        values.addProperty("counter_of_confirmable_messages_attempts", runtimeInfo.getMessageCounters(0));
        values.addProperty("counter_of_non_confirmable_messages_attempts", runtimeInfo.getMessageCounters(1));
        values.addProperty("counter_of_succeeded_messages", runtimeInfo.getMessageCounters(2));
        values.addProperty("min_battery_mcu_temp", runtimeInfo.getMinBatteryMcuTemperature());
        values.addProperty("min_battery_voltage", runtimeInfo.getMinBatteryVoltage());
        values.addProperty("min_mcu_temp", runtimeInfo.getMinMcuTemperature());
        values.addProperty("runtime_errors", runtimeInfo.getRuntimeErrorsCount());
        values.addProperty("up_time", runtimeInfo.getUpTime());

        return new EfentoTelemetry(System.currentTimeMillis(), values);
    }

    private JsonElement getEfentoConfiguration(byte[] bytes) throws InvalidProtocolBufferException {
        return parseString(ProtoConverter.dynamicMsgToJson(bytes, ConfigProtos.getDescriptor().getMessageTypes().get(2)));
    }

    private static String getDate(long seconds) {
        if (seconds == -1L || seconds == 4294967295L) {
            return "Undefined";
        }
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd MMM yyyy HH:mm:ss Z");
        return simpleDateFormat.format(new Date(TimeUnit.SECONDS.toMillis(seconds)));
    }

    @Data
    @AllArgsConstructor
    public static class EfentoTelemetry {

        private long ts;
        private JsonElement values;

    }
}
