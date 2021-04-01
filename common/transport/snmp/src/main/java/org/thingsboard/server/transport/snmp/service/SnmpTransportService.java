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

import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.snmp4j.CommandResponder;
import org.snmp4j.CommandResponderEvent;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.smi.Null;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.TcpAddress;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultTcpTransportMapping;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.TbTransportService;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.kv.DataType;
import org.thingsboard.server.common.data.transport.snmp.SnmpCommunicationSpec;
import org.thingsboard.server.common.data.transport.snmp.SnmpMapping;
import org.thingsboard.server.common.data.transport.snmp.configs.RepeatingQueryingSnmpCommunicationConfig;
import org.thingsboard.server.common.data.transport.snmp.configs.SnmpCommunicationConfig;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.transport.adaptor.JsonConverter;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbSnmpTransportComponent;
import org.thingsboard.server.transport.snmp.SnmpTransportContext;
import org.thingsboard.server.transport.snmp.session.DeviceSessionContext;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@TbSnmpTransportComponent
@Service
@Slf4j
public class SnmpTransportService implements TbTransportService, CommandResponder {
    private final SnmpTransportContext snmpTransportContext;
    private final TransportService transportService;

    @Getter
    private Snmp snmp;
    private ScheduledExecutorService queryingExecutor;
    private ExecutorService responseProcessingExecutor;

    private final Map<SnmpCommunicationSpec, BiConsumer<JsonObject, DeviceSessionContext>> responseProcessors = new EnumMap<>(SnmpCommunicationSpec.class);

    @Value("${transport.snmp.response_processing.parallelism_level}")
    private Integer responseProcessingParallelismLevel;
    @Value("${transport.snmp.underlying_protocol}")
    private String snmpUnderlyingProtocol;

    public SnmpTransportService(@Lazy SnmpTransportContext snmpTransportContext,
                                TransportService transportService) {
        this.snmpTransportContext = snmpTransportContext;
        this.transportService = transportService;
    }

    @PostConstruct
    private void init() throws IOException {
        log.info("Initializing SNMP transport service");

        queryingExecutor = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors(), ThingsBoardThreadFactory.forName("snmp-querying"));
        responseProcessingExecutor = Executors.newWorkStealingPool(responseProcessingParallelismLevel);

        initializeSnmp();
        initializeTrapsListener();
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
    }

    private void initializeTrapsListener() throws IOException {
        int trapsListeningPort = 1062;
        String bindingAddress = "0.0.0.0/" + trapsListeningPort;

        TransportMapping<?> transportMapping;
        switch (snmpUnderlyingProtocol) {
            case "udp":
                transportMapping = new DefaultUdpTransportMapping(new UdpAddress(bindingAddress));
                break;
            case "tcp":
                transportMapping = new DefaultTcpTransportMapping(new TcpAddress(bindingAddress));
                break;
            default:
                throw new IllegalArgumentException("Underlying protocol " + snmpUnderlyingProtocol + " for SNMP is not supported");
        }


        Snmp trapsSnmp = new Snmp(transportMapping);
        trapsSnmp.addCommandResponder(this);

        transportMapping.listen();
    }

    public void createQueryingTasks(DeviceSessionContext sessionContext) {
        List<ScheduledFuture<?>> queryingTasks = sessionContext.getProfileTransportConfiguration().getCommunicationConfigs().stream()
                .filter(config -> config.getSpec().isRepeatingQuerying())
                .map(config -> {
                    RepeatingQueryingSnmpCommunicationConfig repeatingCommunicationConfig = (RepeatingQueryingSnmpCommunicationConfig) config;
                    return createQueryingTaskForConfig(sessionContext, repeatingCommunicationConfig);
                })
                .collect(Collectors.toList());
        sessionContext.setQueryingTasks(queryingTasks);
    }

    private ScheduledFuture<?> createQueryingTaskForConfig(DeviceSessionContext sessionContext, RepeatingQueryingSnmpCommunicationConfig communicationConfig) {
        Long queryingFrequency = communicationConfig.getQueryingFrequencyMs();
        return queryingExecutor.scheduleWithFixedDelay(() -> {
            try {
                if (sessionContext.isActive()) {
                    sendRequest(sessionContext, communicationConfig);
                }
            } catch (Exception e) {
                log.error("Failed to send SNMP request for device {}: {}", sessionContext.getDeviceId(), e.getMessage());
            }
        }, queryingFrequency, queryingFrequency, TimeUnit.MILLISECONDS);
    }

    public void cancelQueryingTasks(DeviceSessionContext sessionContext) {
        sessionContext.getQueryingTasks().forEach(task -> task.cancel(true));
        sessionContext.getQueryingTasks().clear();
    }

    public void sendRequest(DeviceSessionContext sessionContext, SnmpCommunicationConfig communicationConfig) throws IOException {
        PDU request = createPdu(communicationConfig);
        executeRequest(sessionContext, request);
    }

    public void sendRequest(DeviceSessionContext sessionContext, SnmpCommunicationConfig communicationConfig, Map<String, String> values) throws IOException {
        PDU request = createPduWithValues(communicationConfig, values);
        executeRequest(sessionContext, request);
    }

    private void executeRequest(DeviceSessionContext sessionContext, PDU request) throws IOException {
        if (request.size() > 0) {
            log.trace("Executing SNMP request for device {}. Variables bindings: {}", sessionContext.getDeviceId(), request.getVariableBindings());
            snmp.send(request, sessionContext.getTarget(), sessionContext.getDeviceProfile().getId(), sessionContext);
        }
    }

    private PDU createPdu(SnmpCommunicationConfig communicationConfig) {
        PDU pdu = new PDU();
        pdu.setType(communicationConfig.getMethod().getCode());
        pdu.addAll(communicationConfig.getMappings().stream()
                .map(mapping -> new VariableBinding(new OID(mapping.getOid())))
                .collect(Collectors.toList()));
        return pdu;
    }

    private PDU createPduWithValues(SnmpCommunicationConfig communicationConfig, Map<String, String> values) {
        PDU pdu = new PDU();
        pdu.setType(communicationConfig.getMethod().getCode());
        pdu.addAll(communicationConfig.getMappings().stream()
                .filter(mapping -> values.containsKey(mapping.getKey()))
                .map(mapping -> {
                    String value = values.get(mapping.getKey());
                    return new VariableBinding(new OID(mapping.getOid()), new OctetString(value));
                })
                .collect(Collectors.toList()));
        return pdu;
    }


    private void processTrap(CommandResponderEvent event) {
        if (event.getPDU().getType() != PDU.TRAP) return;

        snmpTransportContext.getSessions().stream()
                .filter(sessionContext -> {
                    // TODO: SNMP v3 support
                    return sessionContext.getTarget().getSecurityName().equals(OctetString.fromByteArray(event.getSecurityName())) &&
                            sessionContext.getTarget().getAddress().equals(event.getPeerAddress());
                })
                .findFirst()
                .ifPresentOrElse(sessionContext -> {
                    responseProcessingExecutor.execute(() -> processResponse(sessionContext, event.getPDU()));
                }, () -> {
                    log.debug("SNMP event is from unknown source: {}", event);
                });
    }

    public void processResponseEvent(DeviceSessionContext sessionContext, ResponseEvent event) {
        ((Snmp) event.getSource()).cancel(event.getRequest(), sessionContext);

        if (event.getError() != null) {
            log.warn("Response error: {}", event.getError().getMessage(), event.getError());
            return;
        }

        PDU response = event.getResponse();
        if (response == null) {
            log.warn("No response from SNMP device {}, requestId: {}", sessionContext.getDeviceId(), event.getRequest().getRequestID());
            return;
        }
        DeviceProfileId deviceProfileId = (DeviceProfileId) event.getUserObject();
        log.debug("[{}] Processing SNMP response for device {} with device profile {}: {}",
                response.getRequestID(), sessionContext.getDeviceId(), deviceProfileId, response);

        responseProcessingExecutor.execute(() -> processResponse(sessionContext, response));
    }

    private void processResponse(DeviceSessionContext sessionContext, PDU responsePdu) {
        Map<OID, SnmpMapping> mappings = new HashMap<>();
        Map<OID, SnmpCommunicationConfig> configs = new HashMap<>();
        Map<SnmpCommunicationSpec, JsonObject> responses = new EnumMap<>(SnmpCommunicationSpec.class);

        for (SnmpCommunicationConfig config : sessionContext.getProfileTransportConfiguration().getCommunicationConfigs()) {
            for (SnmpMapping mapping : config.getMappings()) {
                OID oid = new OID(mapping.getOid());
                mappings.put(oid, mapping);
                configs.put(oid, config);
            }
            responses.put(config.getSpec(), new JsonObject());
        }

        for (int i = 0; i < responsePdu.size(); i++) {
            VariableBinding variableBinding = responsePdu.get(i);
            log.trace("Processing variable binding {}: {}", i, variableBinding);

            if (variableBinding.getVariable() instanceof Null) {
                log.debug("Response variable is empty");
                continue;
            }

            OID oid = variableBinding.getOid();
            if (!mappings.containsKey(oid)) {
                log.debug("No SNMP mapping for oid {}", oid);
                continue;
            }

            SnmpCommunicationSpec spec = configs.get(oid).getSpec();
            if (!responseProcessors.containsKey(spec)) {
                log.debug("No response processor found for spec {}", spec);
                continue;
            }

            SnmpMapping mapping = mappings.get(oid);
            processValue(mapping.getKey(), mapping.getDataType(), variableBinding.toValueString(), responses.get(spec));
        }

        if (responses.values().stream().allMatch(response -> response.entrySet().isEmpty())) {
            log.debug("No values is the SNMP response for device {}. Request id: {}", sessionContext.getDeviceId(), responsePdu.getRequestID());
            return;
        }

        responses.forEach((spec, response) -> {
            Optional.ofNullable(responseProcessors.get(spec))
                    .ifPresent(responseProcessor -> {
                        responseProcessor.accept(response, sessionContext);
                    });
        });

        reportActivity(sessionContext.getSessionInfo());
    }

    private void configureResponseProcessors() {
        Stream.of(SnmpCommunicationSpec.TELEMETRY_QUERYING, SnmpCommunicationSpec.TELEMETRY_TRAPS_RECEIVING)
                .forEach(telemetrySpec -> {
                    responseProcessors.put(telemetrySpec, (response, sessionContext) -> {
                        TransportProtos.PostTelemetryMsg postTelemetryMsg = JsonConverter.convertToTelemetryProto(response);
                        transportService.process(sessionContext.getSessionInfo(), postTelemetryMsg, TransportServiceCallback.EMPTY);
                        log.debug("Posted telemetry for device {}: {}", sessionContext.getDeviceId(), response);
                    });
                });

        Stream.of(SnmpCommunicationSpec.CLIENT_ATTRIBUTES_QUERYING, SnmpCommunicationSpec.CLIENT_ATTRIBUTES_TRAPS_RECEIVING)
                .forEach(clientAttributesSpec -> {
                    responseProcessors.put(clientAttributesSpec, (response, sessionContext) -> {
                        TransportProtos.PostAttributeMsg postAttributesMsg = JsonConverter.convertToAttributesProto(response);
                        transportService.process(sessionContext.getSessionInfo(), postAttributesMsg, TransportServiceCallback.EMPTY);
                        log.debug("Posted attributes for device {}: {}", sessionContext.getDeviceId(), response);
                    });
                });
    }

    private void reportActivity(TransportProtos.SessionInfoProto sessionInfo) {
        transportService.process(sessionInfo, TransportProtos.SubscriptionInfoProto.newBuilder()
                .setAttributeSubscription(false)
                .setRpcSubscription(false)
                .setLastActivityTime(System.currentTimeMillis())
                .build(), TransportServiceCallback.EMPTY);
    }

    private void processValue(String key, DataType dataType, String value, JsonObject result) {
        if (StringUtils.isEmpty(value)) return;

        switch (dataType) {
            case LONG:
                result.addProperty(key, Long.parseLong(value));
                break;
            case BOOLEAN:
                result.addProperty(key, Boolean.parseBoolean(value));
                break;
            case DOUBLE:
                result.addProperty(key, Double.parseDouble(value));
                break;
            default:
                result.addProperty(key, value);
        }
    }

    @Override
    public void processPdu(CommandResponderEvent event) {
        processTrap(event);
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
}
