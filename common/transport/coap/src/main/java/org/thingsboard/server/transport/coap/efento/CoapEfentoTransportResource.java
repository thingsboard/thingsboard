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
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.device.profile.CoapDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.EfentoCoapDeviceTypeConfiguration;
import org.thingsboard.server.common.transport.adaptor.AdaptorException;
import org.thingsboard.server.common.transport.adaptor.ProtoConverter;
import org.thingsboard.server.common.transport.auth.SessionInfoCreator;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.coap.ConfigProtos;
import org.thingsboard.server.gen.transport.coap.DeviceInfoProtos;
import org.thingsboard.server.gen.transport.coap.MeasurementTypeProtos;
import org.thingsboard.server.gen.transport.coap.MeasurementsProtos;
import org.thingsboard.server.transport.coap.AbstractCoapTransportResource;
import org.thingsboard.server.transport.coap.CoapTransportContext;
import org.thingsboard.server.transport.coap.callback.CoapDeviceAuthCallback;
import org.thingsboard.server.transport.coap.callback.CoapEfentoCallback;
import org.thingsboard.server.transport.coap.efento.utils.CoapEfentoUtils;

import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.google.gson.JsonParser.parseString;
import static org.thingsboard.server.transport.coap.CoapTransportService.CONFIGURATION;
import static org.thingsboard.server.transport.coap.CoapTransportService.CURRENT_TIMESTAMP;
import static org.thingsboard.server.transport.coap.CoapTransportService.DEVICE_INFO;
import static org.thingsboard.server.transport.coap.CoapTransportService.MEASUREMENTS;

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

    private List<EfentoTelemetry> getEfentoMeasurements(MeasurementsProtos.ProtoMeasurements protoMeasurements, UUID sessionId) {
        String serialNumber = CoapEfentoUtils.convertByteArrayToString(protoMeasurements.getSerialNum().toByteArray());
        boolean batteryStatus = protoMeasurements.getBatteryStatus();
        int measurementPeriodBase = protoMeasurements.getMeasurementPeriodBase();
        int measurementPeriodFactor = protoMeasurements.getMeasurementPeriodFactor();
        int signal = protoMeasurements.getSignal();
        List<MeasurementsProtos.ProtoChannel> channelsList = protoMeasurements.getChannelsList();
        Map<Long, JsonObject> valuesMap = new TreeMap<>();
        if (!CollectionUtils.isEmpty(channelsList)) {
            int channel = 0;
            JsonObject values;
            for (MeasurementsProtos.ProtoChannel protoChannel : channelsList) {
                channel++;
                boolean isBinarySensor = false;
                MeasurementTypeProtos.MeasurementType measurementType = protoChannel.getType();
                String measurementTypeName = measurementType.name();
                if (measurementType.equals(MeasurementTypeProtos.MeasurementType.OK_ALARM)
                        || measurementType.equals(MeasurementTypeProtos.MeasurementType.FLOODING)) {
                    isBinarySensor = true;
                }
                if (measurementPeriodFactor == 0 && isBinarySensor) {
                    measurementPeriodFactor = 14;
                } else {
                    measurementPeriodFactor = 1;
                }
                int measurementPeriod = measurementPeriodBase * measurementPeriodFactor;
                long measurementPeriodMillis = TimeUnit.SECONDS.toMillis(measurementPeriod);
                long nextTransmissionAtMillis = TimeUnit.SECONDS.toMillis(protoMeasurements.getNextTransmissionAt());
                int startPoint = protoChannel.getStartPoint();
                int startTimestamp = protoChannel.getTimestamp();
                long startTimestampMillis = TimeUnit.SECONDS.toMillis(startTimestamp);
                List<Integer> sampleOffsetsList = protoChannel.getSampleOffsetsList();
                if (!CollectionUtils.isEmpty(sampleOffsetsList)) {
                    int sampleOfssetsListSize = sampleOffsetsList.size();
                    for (int i = 0; i < sampleOfssetsListSize; i++) {
                        int sampleOffset = sampleOffsetsList.get(i);
                        Integer previousSampleOffset = isBinarySensor && i > 0 ? sampleOffsetsList.get(i - 1) : null;
                        if (sampleOffset == -32768) {
                            log.warn("[{}],[{}] Sensor error value! Ignoring.", sessionId, sampleOffset);
                        } else {
                            switch (measurementType) {
                                case TEMPERATURE:
                                    values = valuesMap.computeIfAbsent(startTimestampMillis, k ->
                                            CoapEfentoUtils.setDefaultMeasurements(serialNumber, batteryStatus, measurementPeriod, nextTransmissionAtMillis, signal, k));
                                    values.addProperty("temperature_" + channel, ((double) (startPoint + sampleOffset)) / 10f);
                                    startTimestampMillis = startTimestampMillis + measurementPeriodMillis;
                                    break;
                                case WATER_METER:
                                    values = valuesMap.computeIfAbsent(startTimestampMillis, k ->
                                            CoapEfentoUtils.setDefaultMeasurements(serialNumber, batteryStatus, measurementPeriod, nextTransmissionAtMillis, signal, k));
                                    values.addProperty("pulse_counter_water_" + channel, ((double) (startPoint + sampleOffset)));
                                    startTimestampMillis = startTimestampMillis + measurementPeriodMillis;
                                    break;
                                case HUMIDITY:
                                    values = valuesMap.computeIfAbsent(startTimestampMillis, k ->
                                            CoapEfentoUtils.setDefaultMeasurements(serialNumber, batteryStatus, measurementPeriod, nextTransmissionAtMillis, signal, k));
                                    values.addProperty("humidity_" + channel, (double) (startPoint + sampleOffset));
                                    startTimestampMillis = startTimestampMillis + measurementPeriodMillis;
                                    break;
                                case ATMOSPHERIC_PRESSURE:
                                    values = valuesMap.computeIfAbsent(startTimestampMillis, k ->
                                            CoapEfentoUtils.setDefaultMeasurements(serialNumber, batteryStatus, measurementPeriod, nextTransmissionAtMillis, signal, k));
                                    values.addProperty("pressure_" + channel, (double) (startPoint + sampleOffset) / 10f);
                                    startTimestampMillis = startTimestampMillis + measurementPeriodMillis;
                                    break;
                                case DIFFERENTIAL_PRESSURE:
                                    values = valuesMap.computeIfAbsent(startTimestampMillis, k ->
                                            CoapEfentoUtils.setDefaultMeasurements(serialNumber, batteryStatus, measurementPeriod, nextTransmissionAtMillis, signal, k));
                                    values.addProperty("pressure_diff_" + channel, (double) (startPoint + sampleOffset));
                                    startTimestampMillis = startTimestampMillis + measurementPeriodMillis;
                                    break;
                                case OK_ALARM:
                                    boolean currentIsOk = sampleOffset < 0;
                                    if (previousSampleOffset != null) {
                                        boolean previousIsOk = previousSampleOffset < 0;
                                        boolean isOk = previousIsOk && currentIsOk;
                                        boolean isAlarm = !previousIsOk && !currentIsOk;
                                        if (isOk || isAlarm) {
                                            break;
                                        }
                                    }
                                    String data = currentIsOk ? "OK" : "ALARM";
                                    long sampleOffsetMillis = TimeUnit.SECONDS.toMillis(sampleOffset);
                                    long measurementTimestamp = startTimestampMillis + Math.abs(sampleOffsetMillis);
                                    values = valuesMap.computeIfAbsent(measurementTimestamp - 1000, k ->
                                            CoapEfentoUtils.setDefaultMeasurements(serialNumber, batteryStatus, measurementPeriod, nextTransmissionAtMillis, signal, k));
                                    values.addProperty("ok_alarm_" + channel, data);
                                    break;
                                case NO_SENSOR:
                                case UNRECOGNIZED:
                                    log.trace("[{}][{}] Sensor error value! Ignoring.", sessionId, measurementTypeName);
                                    break;
                                default:
                                    log.trace("[{}],[{}] Unsupported measurementType! Ignoring.", sessionId, measurementTypeName);
                                    break;
                            }
                        }
                    }
                } else {
                    log.trace("[{}][{}] sampleOffsetsList list is empty!", sessionId, measurementTypeName);
                }
            }
        } else {
            throw new IllegalStateException("[" + sessionId + "]: Failed to get Efento measurements, reason: channels list is empty!");
        }
        if (!CollectionUtils.isEmpty(valuesMap)) {
            List<EfentoTelemetry> efentoMeasurements = new ArrayList<>();
            for (Long ts : valuesMap.keySet()) {
                EfentoTelemetry measurement = new EfentoTelemetry(ts, valuesMap.get(ts));
                efentoMeasurements.add(measurement);
            }
            return efentoMeasurements;
        } else {
            throw new IllegalStateException("[" + sessionId + "]: Failed to collect Efento measurements, reason, values map is empty!");
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
