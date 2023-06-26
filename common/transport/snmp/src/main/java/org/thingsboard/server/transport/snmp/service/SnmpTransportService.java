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
package org.thingsboard.server.transport.snmp.service;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.snmp4j.CommandResponder;
import org.snmp4j.CommandResponderEvent;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.MPv3;
import org.snmp4j.security.SecurityModels;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.security.USM;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.TcpAddress;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.transport.DefaultTcpTransportMapping;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.TbTransportService;
import org.thingsboard.server.common.data.kv.DataType;
import org.thingsboard.server.common.data.transport.snmp.SnmpCommunicationSpec;
import org.thingsboard.server.common.data.transport.snmp.SnmpMapping;
import org.thingsboard.server.common.data.transport.snmp.SnmpMethod;
import org.thingsboard.server.common.data.transport.snmp.config.RepeatingQueryingSnmpCommunicationConfig;
import org.thingsboard.server.common.data.transport.snmp.config.SnmpCommunicationConfig;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.adaptor.JsonConverter;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbSnmpTransportComponent;
import org.thingsboard.server.transport.snmp.SnmpTransportContext;
import org.thingsboard.server.transport.snmp.session.DeviceSessionContext;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.ArrayList;
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
public class SnmpTransportService implements TbTransportService, CommandResponder {
    private final TransportService transportService;
    private final PduService pduService;
    @Autowired @Lazy
    private SnmpTransportContext transportContext;

    @Getter
    private Snmp snmp;
    private ScheduledExecutorService queryingExecutor;
    private ExecutorService responseProcessingExecutor;

    private final Map<SnmpCommunicationSpec, ResponseDataMapper> responseDataMappers = new EnumMap<>(SnmpCommunicationSpec.class);
    private final Map<SnmpCommunicationSpec, ResponseProcessor> responseProcessors = new EnumMap<>(SnmpCommunicationSpec.class);

    @Value("${transport.snmp.bind_port:1620}")
    private Integer snmpBindPort;
    @Value("${transport.snmp.response_processing.parallelism_level}")
    private Integer responseProcessingParallelismLevel;
    @Value("${transport.snmp.underlying_protocol}")
    private String snmpUnderlyingProtocol;

    @PostConstruct
    private void init() throws IOException {
        queryingExecutor = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors(), ThingsBoardThreadFactory.forName("snmp-querying"));
        responseProcessingExecutor = ThingsBoardExecutors.newWorkStealingPool(responseProcessingParallelismLevel, "snmp-response-processing");

        initializeSnmp();
        configureResponseDataMappers();
        configureResponseProcessors();

        log.info("SNMP transport service initialized");
    }

    @PreDestroy
    public void stop() {
        if (queryingExecutor != null) {
            queryingExecutor.shutdownNow();
        }
        if (responseProcessingExecutor != null) {
            responseProcessingExecutor.shutdownNow();
        }
    }

    private void initializeSnmp() throws IOException {
        TransportMapping<?> transportMapping;
        switch (snmpUnderlyingProtocol) {
            case "udp":
                transportMapping = new DefaultUdpTransportMapping(new UdpAddress(snmpBindPort));
                break;
            case "tcp":
                transportMapping = new DefaultTcpTransportMapping(new TcpAddress(snmpBindPort));
                break;
            default:
                throw new IllegalArgumentException("Underlying protocol " + snmpUnderlyingProtocol + " for SNMP is not supported");
        }
        snmp = new Snmp(transportMapping);
        snmp.addNotificationListener(transportMapping, transportMapping.getListenAddress(), this);
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
                            transportService.errorEvent(sessionContext.getTenantId(), sessionContext.getDeviceId(), config.getSpec().getLabel(), e);
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
        List<PDU> request = pduService.createPdus(sessionContext, communicationConfig, values);
        RequestContext requestContext = RequestContext.builder()
                .communicationSpec(communicationConfig.getSpec())
                .method(communicationConfig.getMethod())
                .responseMappings(communicationConfig.getAllMappings())
                .requestSize(request.size())
                .build();
        sendRequest(sessionContext, request, requestContext);
    }

    private void sendRequest(DeviceSessionContext sessionContext, List<PDU> request, RequestContext requestContext) {
        for (PDU pdu : request) {
            log.debug("Executing SNMP request for device {} with {} variable bindings", sessionContext.getDeviceId(), pdu.size());
            try {
                snmp.send(pdu, sessionContext.getTarget(), requestContext, sessionContext);
            } catch (IOException e) {
                log.error("Failed to send SNMP request to device {}: {}", sessionContext.getDeviceId(), e.toString());
                transportService.errorEvent(sessionContext.getTenantId(), sessionContext.getDeviceId(), requestContext.getCommunicationSpec().getLabel(), e);
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

        String key = Optional.ofNullable(params.get("key")).map(JsonElement::getAsString).orElse(null);
        String value = Optional.ofNullable(params.get("value")).map(JsonElement::getAsString).orElse(null);

        if (value == null && snmpMethod == SnmpMethod.SET) {
            throw new IllegalArgumentException("Value must be specified for SNMP method 'SET'");
        }

        SnmpCommunicationConfig communicationConfig = sessionContext.getProfileTransportConfiguration().getCommunicationConfigs().stream()
                .filter(config -> config.getSpec() == SnmpCommunicationSpec.TO_DEVICE_RPC_REQUEST)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No communication config found with RPC spec"));
        SnmpMapping snmpMapping = communicationConfig.getAllMappings().stream()
                .filter(mapping -> mapping.getKey().equals(key))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No SNMP mapping found in the config for specified key"));

        String oid = snmpMapping.getOid();
        DataType dataType = snmpMapping.getDataType();

        PDU request = pduService.createSingleVariablePdu(sessionContext, snmpMethod, oid, value, dataType);
        RequestContext requestContext = RequestContext.builder()
                .requestId(toDeviceRpcRequestMsg.getRequestId())
                .communicationSpec(communicationConfig.getSpec())
                .method(snmpMethod)
                .responseMappings(communicationConfig.getAllMappings())
                .requestSize(1)
                .build();
        sendRequest(sessionContext, List.of(request), requestContext);
    }


    public void processResponseEvent(DeviceSessionContext sessionContext, ResponseEvent event) {
        ((Snmp) event.getSource()).cancel(event.getRequest(), sessionContext);
        RequestContext requestContext = (RequestContext) event.getUserObject();
        if (event.getError() != null) {
            log.warn("SNMP response error: {}", event.getError().toString());
            transportService.errorEvent(sessionContext.getTenantId(), sessionContext.getDeviceId(), requestContext.getCommunicationSpec().getLabel(), new RuntimeException(event.getError()));
            return;
        }

        PDU responsePdu = event.getResponse();
        if (log.isTraceEnabled()) {
            log.trace("Received PDU for device {}: {}", sessionContext.getDeviceId(), responsePdu);
        }

        List<PDU> response;
        if (requestContext.getRequestSize() == 1) {
            if (responsePdu == null) {
                log.debug("No response from SNMP device {}, requestId: {}", sessionContext.getDeviceId(), event.getRequest().getRequestID());
                if (requestContext.getMethod() == SnmpMethod.GET) {
                    transportService.errorEvent(sessionContext.getTenantId(), sessionContext.getDeviceId(), requestContext.getCommunicationSpec().getLabel(), new RuntimeException("No response from device"));
                }
                return;
            }
            response = List.of(responsePdu);
        } else {
            List<PDU> responseParts = requestContext.getResponseParts();
            responseParts.add(responsePdu);
            if (responseParts.size() == requestContext.getRequestSize()) {
                response = new ArrayList<>();
                for (PDU responsePart : responseParts) {
                    if (responsePart != null) {
                        response.add(responsePart);
                    }
                }
                log.debug("All response parts are collected for request to device {}", sessionContext.getDeviceId());
            } else {
                log.trace("Awaiting other response parts for request to device {}", sessionContext.getDeviceId());
                return;
            }
        }

        responseProcessingExecutor.execute(() -> {
            try {
                processResponse(sessionContext, response, requestContext);
            } catch (Exception e) {
                transportService.errorEvent(sessionContext.getTenantId(), sessionContext.getDeviceId(), requestContext.getCommunicationSpec().getLabel(), e);
            }
        });
    }

    /* SNMP notifications handler */
    @Override
    public void processPdu(CommandResponderEvent event) {
        Address sourceAddress = event.getPeerAddress();
        DeviceSessionContext sessionContext = transportContext.getSessions().stream()
                .filter(session -> session.getTarget().getAddress().equals(sourceAddress))
                .findFirst().orElse(null);
        if (sessionContext == null) {
            log.warn("SNMP TRAP processing failed: couldn't find device session for address {}", sourceAddress);
            return;
        }

        try {
            processIncomingTrap(sessionContext, event);
        } catch (Throwable e) {
            transportService.errorEvent(sessionContext.getTenantId(), sessionContext.getDeviceId(),
                    SnmpCommunicationSpec.TO_SERVER_RPC_REQUEST.getLabel(), e);
        }
    }

    private void processIncomingTrap(DeviceSessionContext sessionContext, CommandResponderEvent event) {
        PDU pdu = event.getPDU();
        if (pdu == null) {
            log.warn("Got empty trap from device {}", sessionContext.getDeviceId());
            throw new IllegalArgumentException("Received TRAP with no data");
        }

        log.debug("Processing SNMP trap from device {} (PDU: {}}", sessionContext.getDeviceId(), pdu);
        SnmpCommunicationConfig communicationConfig = sessionContext.getProfileTransportConfiguration().getCommunicationConfigs().stream()
                .filter(config -> config.getSpec() == SnmpCommunicationSpec.TO_SERVER_RPC_REQUEST).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No config found for to-server RPC requests"));
        RequestContext requestContext = RequestContext.builder()
                .communicationSpec(communicationConfig.getSpec())
                .responseMappings(communicationConfig.getAllMappings())
                .build();

        responseProcessingExecutor.execute(() -> {
            processResponse(sessionContext, List.of(pdu), requestContext);
        });
    }

    private void processResponse(DeviceSessionContext sessionContext, List<PDU> response, RequestContext requestContext) {
        ResponseProcessor responseProcessor = responseProcessors.get(requestContext.getCommunicationSpec());
        if (responseProcessor == null) return;

        JsonObject responseData = responseDataMappers.get(requestContext.getCommunicationSpec()).map(response, requestContext);
        if (responseData.size() == 0) {
            log.warn("No values in the SNMP response for device {}", sessionContext.getDeviceId());
            throw new IllegalArgumentException("No values in the response");
        }

        responseProcessor.process(responseData, requestContext, sessionContext);
        reportActivity(sessionContext.getSessionInfo());
    }

    private void configureResponseDataMappers() {
        responseDataMappers.put(SnmpCommunicationSpec.TO_DEVICE_RPC_REQUEST, (pdus, requestContext) -> {
            JsonObject responseData = new JsonObject();
            pduService.processPdus(pdus).forEach((oid, value) -> {
                requestContext.getResponseMappings().stream()
                        .filter(snmpMapping -> snmpMapping.getOid().equals(oid.toDottedString()))
                        .findFirst()
                        .ifPresent(snmpMapping -> {
                            pduService.processValue(snmpMapping.getKey(), snmpMapping.getDataType(), value, responseData);
                        });
            });
            return responseData;
        });

        ResponseDataMapper defaultResponseDataMapper = (pdus, requestContext) -> {
            return pduService.processPdus(pdus, requestContext.getResponseMappings());
        };
        Arrays.stream(SnmpCommunicationSpec.values())
                .forEach(communicationSpec -> {
                    responseDataMappers.putIfAbsent(communicationSpec, defaultResponseDataMapper);
                });
    }

    private void configureResponseProcessors() {
        responseProcessors.put(SnmpCommunicationSpec.TELEMETRY_QUERYING, (responseData, requestContext, sessionContext) -> {
            TransportProtos.PostTelemetryMsg postTelemetryMsg = JsonConverter.convertToTelemetryProto(responseData);
            transportService.process(sessionContext.getSessionInfo(), postTelemetryMsg, null);
            log.debug("Posted telemetry for SNMP device {}: {}", sessionContext.getDeviceId(), responseData);
        });

        responseProcessors.put(SnmpCommunicationSpec.CLIENT_ATTRIBUTES_QUERYING, (responseData, requestContext, sessionContext) -> {
            TransportProtos.PostAttributeMsg postAttributesMsg = JsonConverter.convertToAttributesProto(responseData);
            transportService.process(sessionContext.getSessionInfo(), postAttributesMsg, null);
            log.debug("Posted attributes for SNMP device {}: {}", sessionContext.getDeviceId(), responseData);
        });

        responseProcessors.put(SnmpCommunicationSpec.TO_DEVICE_RPC_REQUEST, (responseData, requestContext, sessionContext) -> {
            TransportProtos.ToDeviceRpcResponseMsg rpcResponseMsg = TransportProtos.ToDeviceRpcResponseMsg.newBuilder()
                    .setRequestId(requestContext.getRequestId())
                    .setPayload(JsonConverter.toJson(responseData))
                    .build();
            transportService.process(sessionContext.getSessionInfo(), rpcResponseMsg, null);
            log.debug("Posted RPC response {} for device {}", responseData, sessionContext.getDeviceId());
        });

        responseProcessors.put(SnmpCommunicationSpec.TO_SERVER_RPC_REQUEST, (responseData, requestContext, sessionContext) -> {
            TransportProtos.ToServerRpcRequestMsg toServerRpcRequestMsg = TransportProtos.ToServerRpcRequestMsg.newBuilder()
                    .setRequestId(0)
                    .setMethodName("TRAP")
                    .setParams(JsonConverter.toJson(responseData))
                    .build();
            transportService.process(sessionContext.getSessionInfo(), toServerRpcRequestMsg, null);
        });
    }

    private void reportActivity(TransportProtos.SessionInfoProto sessionInfo) {
        transportService.reportActivity(sessionInfo);
    }


    @Override
    public String getName() {
        return DataConstants.SNMP_TRANSPORT_NAME;
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
    private static class RequestContext {
        private final Integer requestId;
        private final SnmpCommunicationSpec communicationSpec;
        private final SnmpMethod method;
        private final List<SnmpMapping> responseMappings;

        private final int requestSize;
        private List<PDU> responseParts;

        @Builder
        public RequestContext(Integer requestId, SnmpCommunicationSpec communicationSpec, SnmpMethod method, List<SnmpMapping> responseMappings, int requestSize) {
            this.requestId = requestId;
            this.communicationSpec = communicationSpec;
            this.method = method;
            this.responseMappings = responseMappings;
            this.requestSize = requestSize;
            if (requestSize > 1) {
                this.responseParts = Collections.synchronizedList(new ArrayList<>());
            }
        }
    }

    private interface ResponseDataMapper {
        JsonObject map(List<PDU> pdus, RequestContext requestContext);
    }

    private interface ResponseProcessor {
        void process(JsonObject responseData, RequestContext requestContext, DeviceSessionContext sessionContext);
    }

}
