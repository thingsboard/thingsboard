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
package org.thingsboard.server.transport.snmp.service;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableScheduledFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
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
import org.snmp4j.smi.IpAddress;
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
import org.thingsboard.server.common.adaptor.JsonConverter;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.TbTransportService;
import org.thingsboard.server.common.data.kv.DataType;
import org.thingsboard.server.common.data.transport.snmp.SnmpCommunicationSpec;
import org.thingsboard.server.common.data.transport.snmp.SnmpMapping;
import org.thingsboard.server.common.data.transport.snmp.SnmpMethod;
import org.thingsboard.server.common.data.transport.snmp.config.RepeatingQueryingSnmpCommunicationConfig;
import org.thingsboard.server.common.data.transport.snmp.config.SnmpCommunicationConfig;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbSnmpTransportComponent;
import org.thingsboard.server.transport.snmp.SnmpTransportContext;
import org.thingsboard.server.transport.snmp.session.DeviceSessionContext;
import org.thingsboard.server.transport.snmp.session.ScheduledTask;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@TbSnmpTransportComponent
@Service
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("UnstableApiUsage")
public class SnmpTransportService implements TbTransportService, CommandResponder {
    private final TransportService transportService;
    private final PduService pduService;
    @Autowired @Lazy
    private SnmpTransportContext transportContext;

    @Getter
    private Snmp snmp;
    private ListeningScheduledExecutorService scheduler;
    private ExecutorService executor;

    private final Map<SnmpCommunicationSpec, ResponseDataMapper> responseDataMappers = new EnumMap<>(SnmpCommunicationSpec.class);
    private final Map<SnmpCommunicationSpec, ResponseProcessor> responseProcessors = new EnumMap<>(SnmpCommunicationSpec.class);

    @Value("${transport.snmp.bind_port:0}")
    private Integer snmpBindPort;
    @Value("${transport.snmp.bind_address:0.0.0.0}")
    private String snmpBindAddress;
    @Value("${transport.snmp.response_processing.parallelism_level:4}")
    private int responseProcessingThreadPoolSize;
    @Value("${transport.snmp.scheduler_thread_pool_size:4}")
    private int schedulerThreadPoolSize;
    @Value("${transport.snmp.underlying_protocol}")
    private String snmpUnderlyingProtocol;
    @Value("${transport.snmp.request_chunk_delay_ms:100}")
    private int requestChunkDelayMs;

    @PostConstruct
    private void init() throws IOException {
        scheduler = MoreExecutors.listeningDecorator(ThingsBoardExecutors.newScheduledThreadPool(schedulerThreadPoolSize, "snmp-querying"));
        executor = ThingsBoardExecutors.newWorkStealingPool(responseProcessingThreadPoolSize, "snmp-response-processing");

        initializeSnmp();
        configureResponseDataMappers();
        configureResponseProcessors();

        log.info("SNMP transport service initialized");
    }

    @PreDestroy
    public void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    private void initializeSnmp() throws IOException {
        TransportMapping<?> transportMapping;
        switch (snmpUnderlyingProtocol) {
            case "udp":
                transportMapping = new DefaultUdpTransportMapping(new UdpAddress(InetAddress.getByName(snmpBindAddress), snmpBindPort));
                break;
            case "tcp":
                transportMapping = new DefaultTcpTransportMapping(new TcpAddress(InetAddress.getByName(snmpBindAddress), snmpBindPort));
                break;
            default:
                throw new IllegalArgumentException("Underlying protocol " + snmpUnderlyingProtocol + " for SNMP is not supported");
        }
        snmp = new Snmp(transportMapping);
        snmp.addNotificationListener(transportMapping, transportMapping.getListenAddress(), this);
        snmp.listen();

        SecurityProtocols.getInstance().addPredefinedProtocolSet(SecurityProtocols.SecurityProtocolSet.maxCompatibility);
        USM usm = new USM(SecurityProtocols.getInstance(), new OctetString(MPv3.createLocalEngineID()), 0);
        SecurityModels.getInstance().addSecurityModel(usm);
    }

    public void createQueryingTasks(DeviceSessionContext sessionContext) {
        sessionContext.getProfileTransportConfiguration().getCommunicationConfigs().stream()
                .filter(communicationConfig -> communicationConfig instanceof RepeatingQueryingSnmpCommunicationConfig)
                .forEach(config -> {
                    RepeatingQueryingSnmpCommunicationConfig repeatingCommunicationConfig = (RepeatingQueryingSnmpCommunicationConfig) config;
                    Long queryingFrequency = repeatingCommunicationConfig.getQueryingFrequencyMs();

                    ScheduledTask scheduledTask = new ScheduledTask();
                    scheduledTask.init(() -> {
                        try {
                            if (sessionContext.isActive()) {
                                return sendRequest(sessionContext, repeatingCommunicationConfig);
                            }
                        } catch (Exception e) {
                            log.error("Failed to send SNMP request for device {}: {}", sessionContext.getDeviceId(), e.toString());
                            transportService.errorEvent(sessionContext.getTenantId(), sessionContext.getDeviceId(), config.getSpec().getLabel(), e);
                        }
                        return Futures.immediateVoidFuture();
                    }, queryingFrequency, scheduler);
                    sessionContext.getQueryingTasks().add(scheduledTask);
                });
    }

    public void cancelQueryingTasks(DeviceSessionContext sessionContext) {
        sessionContext.getQueryingTasks().forEach(ScheduledTask::cancel);
        sessionContext.getQueryingTasks().clear();
    }


    private ListenableFuture<Void> sendRequest(DeviceSessionContext sessionContext, SnmpCommunicationConfig communicationConfig) {
        return sendRequest(sessionContext, communicationConfig, Collections.emptyMap());
    }

    private ListenableFuture<Void> sendRequest(DeviceSessionContext sessionContext, SnmpCommunicationConfig communicationConfig, Map<String, String> values) {
        List<PDU> request = pduService.createPdus(sessionContext, communicationConfig, values);
        RequestContext requestContext = RequestContext.builder()
                .communicationSpec(communicationConfig.getSpec())
                .method(communicationConfig.getMethod())
                .responseMappings(communicationConfig.getAllMappings())
                .requestSize(request.size())
                .build();
        return sendRequest(sessionContext, request, requestContext);
    }

    private ListenableFuture<Void> sendRequest(DeviceSessionContext sessionContext, List<PDU> request, RequestContext requestContext) {
        if (request.size() <= 1 || requestChunkDelayMs == 0) {
            for (PDU pdu : request) {
                sendPdu(pdu, requestContext, sessionContext);
            }
            return Futures.immediateVoidFuture();
        }

        List<ListenableFuture<?>> futures = new ArrayList<>();
        for (int i = 0, delay = 0; i < request.size(); i++, delay += requestChunkDelayMs) {
            PDU pdu = request.get(i);
            if (delay == 0) {
                sendPdu(pdu, requestContext, sessionContext);
            } else {
                ListenableScheduledFuture<?> future = scheduler.schedule(() -> {
                    sendPdu(pdu, requestContext, sessionContext);
                }, delay, TimeUnit.MILLISECONDS);
                futures.add(future);
            }
        }
        return Futures.whenAllComplete(futures).call(() -> null, MoreExecutors.directExecutor());
    }

    private void sendPdu(PDU pdu, RequestContext requestContext, DeviceSessionContext sessionContext) {
        log.debug("[{}] Sending SNMP request with {} variable bindings to {}", sessionContext.getDeviceId(), pdu.size(), sessionContext.getTarget().getAddress());
        try {
            snmp.send(pdu, sessionContext.getTarget(), requestContext, sessionContext);
        } catch (Exception e) {
            log.error("[{}] Failed to send SNMP request", sessionContext.getDeviceId(), e);
            transportService.errorEvent(sessionContext.getTenantId(), sessionContext.getDeviceId(), requestContext.getCommunicationSpec().getLabel(), e);
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
            log.warn("[{}] SNMP response error: {}", sessionContext.getDeviceId(), event.getError().toString());
            transportService.errorEvent(sessionContext.getTenantId(), sessionContext.getDeviceId(), requestContext.getCommunicationSpec().getLabel(), new RuntimeException(event.getError()));
            return;
        }

        PDU responsePdu = event.getResponse();
        log.trace("[{}] Received PDU: {}", sessionContext.getDeviceId(), responsePdu);

        List<PDU> response;
        if (requestContext.getRequestSize() == 1) {
            if (responsePdu == null) {
                if (requestContext.getMethod() == SnmpMethod.GET) {
                    log.debug("[{}][{}] Empty response from device", sessionContext.getDeviceId(), event.getRequest().getRequestID());
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
                log.debug("[{}] All {} response parts are collected for request", sessionContext.getDeviceId(), responseParts.size());
            } else {
                log.trace("[{}] Awaiting other response parts for request", sessionContext.getDeviceId());
                return;
            }
        }

        executor.execute(() -> {
            try {
                processResponse(sessionContext, response, requestContext);
            } catch (Exception e) {
                transportService.errorEvent(sessionContext.getTenantId(), sessionContext.getDeviceId(), requestContext.getCommunicationSpec().getLabel(), e);
            }
        });
    }

    /*
     * SNMP notifications handler
     *
     * TODO: add check for host uniqueness when saving device (for backward compatibility - only for the ones using from-device RPC requests)
     *
     * NOTE: SNMP TRAPs support won't work properly when there is more than one SNMP transport,
     *  due to load-balancing of requests from devices: session might not be on this instance
     * */
    @Override
    public void processPdu(CommandResponderEvent event) {
        IpAddress sourceAddress = (IpAddress) event.getPeerAddress();
        List<DeviceSessionContext> sessions = transportContext.getSessions().stream()
                .filter(session -> ((IpAddress) session.getTarget().getAddress()).getInetAddress().equals(sourceAddress.getInetAddress()))
                .collect(Collectors.toList());
        if (sessions.isEmpty()) {
            log.warn("Couldn't find device session for SNMP TRAP for address {}", sourceAddress);
            return;
        } else if (sessions.size() > 1) {
            for (DeviceSessionContext sessionContext : sessions) {
                transportService.errorEvent(sessionContext.getTenantId(), sessionContext.getDeviceId(), SnmpCommunicationSpec.TO_SERVER_RPC_REQUEST.getLabel(),
                        new IllegalStateException("Found multiple devices for host " + sourceAddress.getInetAddress().getHostAddress()));
            }
            return;
        }

        DeviceSessionContext sessionContext = sessions.get(0);
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
            log.warn("[{}] Received empty SNMP trap", sessionContext.getDeviceId());
            throw new IllegalArgumentException("Received TRAP with no data");
        }

        log.debug("[{}] Processing SNMP trap: {}", sessionContext.getDeviceId(), pdu);
        SnmpCommunicationConfig communicationConfig = sessionContext.getProfileTransportConfiguration().getCommunicationConfigs().stream()
                .filter(config -> config.getSpec() == SnmpCommunicationSpec.TO_SERVER_RPC_REQUEST).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No config found for to-server RPC requests"));
        RequestContext requestContext = RequestContext.builder()
                .communicationSpec(communicationConfig.getSpec())
                .responseMappings(communicationConfig.getAllMappings())
                .method(SnmpMethod.TRAP)
                .build();

        executor.execute(() -> {
            processResponse(sessionContext, List.of(pdu), requestContext);
        });
    }

    private void processResponse(DeviceSessionContext sessionContext, List<PDU> response, RequestContext requestContext) {
        ResponseProcessor responseProcessor = responseProcessors.get(requestContext.getCommunicationSpec());
        if (responseProcessor == null) return;

        JsonObject responseData = responseDataMappers.get(requestContext.getCommunicationSpec()).map(response, requestContext);
        if (responseData.size() == 0) {
            log.warn("[{}] No values in the response", sessionContext.getDeviceId());
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
                    .setMethodName(requestContext.getMethod().name())
                    .setParams(JsonConverter.toJson(responseData))
                    .build();
            transportService.process(sessionContext.getSessionInfo(), toServerRpcRequestMsg, null);
        });
    }

    private void reportActivity(TransportProtos.SessionInfoProto sessionInfo) {
        transportService.recordActivity(sessionInfo);
    }


    @Override
    public String getName() {
        return DataConstants.SNMP_TRANSPORT_NAME;
    }

    @PreDestroy
    public void shutdown() {
        log.info("Stopping SNMP transport!");
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        if (executor != null) {
            executor.shutdownNow();
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
