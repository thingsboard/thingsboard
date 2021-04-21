/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.transport.snmp.service;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.MPv3;
import org.snmp4j.security.SecurityModels;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.security.USM;
import org.snmp4j.smi.OctetString;
import org.snmp4j.transport.DefaultTcpTransportMapping;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.TbTransportService;
import org.thingsboard.server.common.data.kv.DataType;
import org.thingsboard.server.common.data.transport.snmp.SnmpCommunicationSpec;
import org.thingsboard.server.common.data.transport.snmp.SnmpMapping;
import org.thingsboard.server.common.data.transport.snmp.SnmpMethod;
import org.thingsboard.server.common.data.transport.snmp.config.RepeatingQueryingSnmpCommunicationConfig;
import org.thingsboard.server.common.data.transport.snmp.config.SnmpCommunicationConfig;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.transport.adaptor.JsonConverter;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbSnmpTransportComponent;
import org.thingsboard.server.transport.snmp.session.DeviceSessionContext;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@TbSnmpTransportComponent
@Service
@Slf4j
@RequiredArgsConstructor
public class SnmpTransportService implements TbTransportService {
    private final TransportService transportService;
    private final PduService pduService;

    @Getter
    private Snmp snmp;
    private ScheduledExecutorService queryingExecutor;
    private ExecutorService responseProcessingExecutor;

    private final Map<SnmpCommunicationSpec, ResponseDataMapper> responseDataMappers = new EnumMap<>(SnmpCommunicationSpec.class);
    private final Map<SnmpCommunicationSpec, ResponseProcessor> responseProcessors = new EnumMap<>(SnmpCommunicationSpec.class);

    @Value("${transport.snmp.response_processing.parallelism_level}")
    private Integer responseProcessingParallelismLevel;
    @Value("${transport.snmp.underlying_protocol}")
    private String snmpUnderlyingProtocol;

    @PostConstruct
    private void init() throws IOException {
        queryingExecutor = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors(), ThingsBoardThreadFactory.forName("snmp-querying"));
        responseProcessingExecutor = Executors.newWorkStealingPool(responseProcessingParallelismLevel);

        initializeSnmp();
        configureResponseDataMappers();
        configureResponseProcessors();

        log.info("SNMP transport service initialized");
    }

    private void initializeSnmp() throws IOException {
        TransportMapping<?> transportMapping;
        switch (snmpUnderlyingProtocol) {
            case "udp":
                transportMapping = new DefaultUdpTransportMapping();
                break;
            case "tcp":
                transportMapping = new DefaultTcpTransportMapping();
                break;
            default:
                throw new IllegalArgumentException("Underlying protocol " + snmpUnderlyingProtocol + " for SNMP is not supported");
        }
        snmp = new Snmp(transportMapping);
        snmp.listen();

        USM usm = new USM(SecurityProtocols.getInstance(), new OctetString(MPv3.createLocalEngineID()), 0);
        SecurityModels.getInstance().addSecurityModel(usm);
    }

    public void createQueryingTasks(DeviceSessionContext sessionContext) {
        List<ScheduledFuture<?>> queryingTasks = sessionContext.getProfileTransportConfiguration().getCommunicationConfigs().stream()
                .filter(communicationConfig -> communicationConfig instanceof RepeatingQueryingSnmpCommunicationConfig)
                .map(config -> {
                    RepeatingQueryingSnmpCommunicationConfig repeatingCommunicationConfig = (RepeatingQueryingSnmpCommunicationConfig) config;
                    Long queryingFrequency = repeatingCommunicationConfig.getQueryingFrequencyMs();

                    return queryingExecutor.scheduleWithFixedDelay(() -> {
                        try {
                            if (sessionContext.isActive()) {
                                sendRequest(sessionContext, repeatingCommunicationConfig);
                            }
                        } catch (Exception e) {
                            log.error("Failed to send SNMP request for device {}: {}", sessionContext.getDeviceId(), e.toString());
                        }
                    }, queryingFrequency, queryingFrequency, TimeUnit.MILLISECONDS);
                })
                .collect(Collectors.toList());
        sessionContext.getQueryingTasks().addAll(queryingTasks);
    }

    public void cancelQueryingTasks(DeviceSessionContext sessionContext) {
        sessionContext.getQueryingTasks().forEach(task -> task.cancel(true));
        sessionContext.getQueryingTasks().clear();
    }


    private void sendRequest(DeviceSessionContext sessionContext, SnmpCommunicationConfig communicationConfig) {
        sendRequest(sessionContext, communicationConfig, Collections.emptyMap());
    }

    private void sendRequest(DeviceSessionContext sessionContext, SnmpCommunicationConfig communicationConfig, Map<String, String> values) {
        PDU request = pduService.createPdu(sessionContext, communicationConfig, values);
        RequestInfo requestInfo = new RequestInfo(communicationConfig.getSpec(), communicationConfig.getAllMappings());
        sendRequest(sessionContext, request, requestInfo);
    }

    private void sendRequest(DeviceSessionContext sessionContext, PDU request, RequestInfo requestInfo) {
        if (request.size() > 0) {
            log.trace("Executing SNMP request for device {}. Variables bindings: {}", sessionContext.getDeviceId(), request.getVariableBindings());
            try {
                snmp.send(request, sessionContext.getTarget(), requestInfo, sessionContext);
            } catch (IOException e) {
                log.error("Failed to send SNMP request to device {}: {}", sessionContext.getDeviceId(), e.toString());
            }
        }
    }

    public void onAttributeUpdate(DeviceSessionContext sessionContext, TransportProtos.AttributeUpdateNotificationMsg attributeUpdateNotification) {
        sessionContext.getProfileTransportConfiguration().getCommunicationConfigs().stream()
                .filter(config -> config.getSpec() == SnmpCommunicationSpec.SHARED_ATTRIBUTES_SETTING)
                .findFirst()
                .ifPresent(communicationConfig -> {
                    Map<String, String> sharedAttributes = JsonConverter.toJson(attributeUpdateNotification).entrySet().stream()
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey,
                                    entry -> entry.getValue().isJsonPrimitive() ? entry.getValue().getAsString() : entry.getValue().toString()
                            ));
                    sendRequest(sessionContext, communicationConfig, sharedAttributes);
                });
    }

    public void onToDeviceRpcRequest(DeviceSessionContext sessionContext, TransportProtos.ToDeviceRpcRequestMsg toDeviceRpcRequestMsg) {
        SnmpMethod snmpMethod = SnmpMethod.valueOf(toDeviceRpcRequestMsg.getMethodName());
        JsonObject params = JsonConverter.parse(toDeviceRpcRequestMsg.getParams()).getAsJsonObject();

        String oid = Optional.ofNullable(params.get("oid")).map(JsonElement::getAsString).orElse(null);
        String value = Optional.ofNullable(params.get("value")).map(JsonElement::getAsString).orElse(null);
        DataType dataType = Optional.ofNullable(params.get("dataType")).map(e -> DataType.valueOf(e.getAsString())).orElse(DataType.STRING);

        if (oid == null || oid.isEmpty()) {
            throw new IllegalArgumentException("OID in to-device RPC request is not specified");
        }
        if (value == null && snmpMethod == SnmpMethod.SET) {
            throw new IllegalArgumentException("Value must be specified for SNMP method 'SET'");
        }

        PDU request = pduService.createSingleVariablePdu(sessionContext, snmpMethod, oid, value, dataType);
        sendRequest(sessionContext, request, new RequestInfo(toDeviceRpcRequestMsg.getRequestId(), SnmpCommunicationSpec.TO_DEVICE_RPC_REQUEST));
    }


    public void processResponseEvent(DeviceSessionContext sessionContext, ResponseEvent event) {
        ((Snmp) event.getSource()).cancel(event.getRequest(), sessionContext);

        if (event.getError() != null) {
            log.warn("SNMP response error: {}", event.getError().toString());
            return;
        }

        PDU response = event.getResponse();
        if (response == null) {
            log.debug("No response from SNMP device {}, requestId: {}", sessionContext.getDeviceId(), event.getRequest().getRequestID());
            return;
        }

        RequestInfo requestInfo = (RequestInfo) event.getUserObject();
        responseProcessingExecutor.execute(() -> {
            processResponse(sessionContext, response, requestInfo);
        });
    }

    private void processResponse(DeviceSessionContext sessionContext, PDU response, RequestInfo requestInfo) {
        ResponseProcessor responseProcessor = responseProcessors.get(requestInfo.getCommunicationSpec());
        if (responseProcessor == null) return;

        JsonObject responseData = responseDataMappers.get(requestInfo.getCommunicationSpec()).map(response, requestInfo);

        if (responseData.entrySet().isEmpty()) {
            log.debug("No values is the SNMP response for device {}. Request id: {}", sessionContext.getDeviceId(), response.getRequestID());
            return;
        }

        responseProcessor.process(responseData, requestInfo, sessionContext);
        reportActivity(sessionContext.getSessionInfo());
    }

    private void configureResponseDataMappers() {
        responseDataMappers.put(SnmpCommunicationSpec.TO_DEVICE_RPC_REQUEST, (pdu, requestInfo) -> {
            JsonObject responseData = new JsonObject();
            pduService.processPdu(pdu).forEach((oid, value) -> {
                responseData.addProperty(oid.toDottedString(), value);
            });
            return responseData;
        });

        ResponseDataMapper defaultResponseDataMapper = (pdu, requestInfo) -> {
            return pduService.processPdu(pdu, requestInfo.getResponseMappings());
        };
        Arrays.stream(SnmpCommunicationSpec.values())
                .forEach(communicationSpec -> {
                    responseDataMappers.putIfAbsent(communicationSpec, defaultResponseDataMapper);
                });
    }

    private void configureResponseProcessors() {
        responseProcessors.put(SnmpCommunicationSpec.TELEMETRY_QUERYING, (responseData, requestInfo, sessionContext) -> {
            TransportProtos.PostTelemetryMsg postTelemetryMsg = JsonConverter.convertToTelemetryProto(responseData);
            transportService.process(sessionContext.getSessionInfo(), postTelemetryMsg, null);
            log.debug("Posted telemetry for SNMP device {}: {}", sessionContext.getDeviceId(), responseData);
        });

        responseProcessors.put(SnmpCommunicationSpec.CLIENT_ATTRIBUTES_QUERYING, (responseData, requestInfo, sessionContext) -> {
            TransportProtos.PostAttributeMsg postAttributesMsg = JsonConverter.convertToAttributesProto(responseData);
            transportService.process(sessionContext.getSessionInfo(), postAttributesMsg, null);
            log.debug("Posted attributes for SNMP device {}: {}", sessionContext.getDeviceId(), responseData);
        });

        responseProcessors.put(SnmpCommunicationSpec.TO_DEVICE_RPC_REQUEST, (responseData, requestInfo, sessionContext) -> {
            TransportProtos.ToDeviceRpcResponseMsg rpcResponseMsg = TransportProtos.ToDeviceRpcResponseMsg.newBuilder()
                    .setRequestId(requestInfo.getRequestId())
                    .setPayload(JsonConverter.toJson(responseData))
                    .build();
            transportService.process(sessionContext.getSessionInfo(), rpcResponseMsg, null);
            log.debug("Posted RPC response {} for device {}", responseData, sessionContext.getDeviceId());
        });
    }

    private void reportActivity(TransportProtos.SessionInfoProto sessionInfo) {
        transportService.process(sessionInfo, TransportProtos.SubscriptionInfoProto.newBuilder()
                .setAttributeSubscription(true)
                .setRpcSubscription(true)
                .setLastActivityTime(System.currentTimeMillis())
                .build(), TransportServiceCallback.EMPTY);
    }


    @Override
    public String getName() {
        return "SNMP";
    }

    @PreDestroy
    public void shutdown() {
        log.info("Stopping SNMP transport!");
        if (queryingExecutor != null) {
            queryingExecutor.shutdownNow();
        }
        if (responseProcessingExecutor != null) {
            responseProcessingExecutor.shutdownNow();
        }
        if (snmp != null) {
            try {
                snmp.close();
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
        log.info("SNMP transport stopped!");
    }

    @Data
    private static class RequestInfo {
        private Integer requestId;
        private SnmpCommunicationSpec communicationSpec;
        private List<SnmpMapping> responseMappings;

        public RequestInfo(Integer requestId, SnmpCommunicationSpec communicationSpec) {
            this.requestId = requestId;
            this.communicationSpec = communicationSpec;
        }

        public RequestInfo(SnmpCommunicationSpec communicationSpec) {
            this.communicationSpec = communicationSpec;
        }

        public RequestInfo(SnmpCommunicationSpec communicationSpec, List<SnmpMapping> responseMappings) {
            this.communicationSpec = communicationSpec;
            this.responseMappings = responseMappings;
        }
    }

    private interface ResponseDataMapper {
        JsonObject map(PDU pdu, RequestInfo requestInfo);
    }

    private interface ResponseProcessor {
        void process(JsonObject responseData, RequestInfo requestInfo, DeviceSessionContext sessionContext);
    }

}
