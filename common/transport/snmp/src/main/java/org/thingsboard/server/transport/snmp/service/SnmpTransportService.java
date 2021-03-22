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
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.TbTransportService;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.kv.DataType;
import org.thingsboard.server.common.transport.TransportContext;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.transport.adaptor.AdaptorException;
import org.thingsboard.server.common.transport.adaptor.JsonConverter;
import org.thingsboard.server.common.transport.auth.SessionInfoCreator;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.snmp.SnmpTransportContext;
import org.thingsboard.server.queue.util.TbSnmpTransportComponent;
import org.thingsboard.server.transport.snmp.session.DeviceSessionContext;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@TbSnmpTransportComponent
@Service
@Slf4j
public class SnmpTransportService implements TbTransportService {
    private final SnmpTransportContext snmpTransportContext;

    @Getter
    private ExecutorService snmpCallbackExecutor;
    @Getter
    private Snmp snmp;
    private ScheduledExecutorService pollingExecutor;

    public SnmpTransportService(@Lazy SnmpTransportContext snmpTransportContext) {
        this.snmpTransportContext = snmpTransportContext;
    }

    //    @PostConstruct
    private void init() {
        log.info("Starting SNMP transport...");
        pollingExecutor = Executors.newScheduledThreadPool(1, ThingsBoardThreadFactory.forName("snmp-polling"));
        //TODO: Set parallelism value in the config
        snmpCallbackExecutor = Executors.newWorkStealingPool(20);

        initializeSnmp();

        log.info("SNMP transport started!");
    }

    @PreDestroy
    public void shutdown() {
        log.info("Stopping SNMP transport!");
        if (pollingExecutor != null) {
            pollingExecutor.shutdownNow();
        }
        if (snmpCallbackExecutor != null) {
            snmpCallbackExecutor.shutdownNow();
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

    @EventListener(ApplicationReadyEvent.class)
    @Order(value = 10)
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        log.info("Received application ready event. Starting SNMP polling.");
//        startPolling();
    }

    private void initializeSnmp() {
        try {
            this.snmp = new Snmp(new DefaultUdpTransportMapping());
            this.snmp.listen();
        } catch (IOException e) {
            //TODO: what should be done if transport wasn't initialized?
            log.error(e.getMessage(), e);
        }
    }

    private void startPolling() {
        //TODO: Get poll period from configuration;
        int pollPeriodSeconds = 1;

        pollingExecutor.scheduleWithFixedDelay(() -> {
            snmpTransportContext.getSessions().forEach(this::executeSnmpRequest);
        }, 0, pollPeriodSeconds, TimeUnit.SECONDS);
    }

    private void executeSnmpRequest(DeviceSessionContext sessionContext) {
        long timeNow = System.currentTimeMillis();
        long nextRequestExecutionTime = sessionContext.getPreviousRequestExecutedAt() + sessionContext.getProfileTransportConfiguration().getPollPeriodMs();

        if (nextRequestExecutionTime < timeNow) {
            sessionContext.setPreviousRequestExecutedAt(timeNow);

            DeviceProfileId deviceProfileId = sessionContext.getDeviceProfile().getId();
            snmpTransportContext.getProfilesPdus().get(deviceProfileId).forEach(pdu -> {
                try {
                    log.debug("[{}] Sending SNMP message...", pdu.getRequestID());
                    snmp.send(pdu, sessionContext.getTarget(), deviceProfileId, sessionContext);
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }
            });
        }
    }

    public void onNewDeviceResponse(ResponseEvent responseEvent, DeviceSessionContext sessionContext) {
        ((Snmp) responseEvent.getSource()).cancel(responseEvent.getRequest(), sessionContext);
        snmpTransportContext.getSnmpCallbackExecutor().submit(() -> processSnmpResponse(responseEvent, sessionContext));
    }

    private void processSnmpResponse(ResponseEvent event, DeviceSessionContext sessionContext) {
        PDU response = event.getResponse();
        if (event.getError() != null) {
            log.warn("Response error: {}", event.getError().getMessage(), event.getError());
        }

        if (response != null) {
            log.debug("[{}] Processing SNMP response: {}", response.getRequestID(), response);

            DeviceProfileId deviceProfileId = (DeviceProfileId) event.getUserObject();
            TransportService transportService = snmpTransportContext.getTransportService();
            for (int i = 0; i < response.size(); i++) {
                VariableBinding vb = response.get(i);
                snmpTransportContext.getAttributesMapping(deviceProfileId, vb.getOid()).ifPresent(kvMapping -> transportService.process(DeviceTransportType.DEFAULT,
                        TransportProtos.ValidateDeviceTokenRequestMsg.newBuilder().setToken(sessionContext.getToken()).build(),
                        new DeviceAuthCallback(snmpTransportContext, sessionInfo -> {
                            try {
                                transportService.process(sessionInfo,
                                        convertToPostAttributes(kvMapping.getKey(), kvMapping.getType(), vb.toValueString()),
                                        TransportServiceCallback.EMPTY);
                                reportActivity(sessionInfo);
                            } catch (Exception e) {
                                log.warn("Failed to process SNMP response: {}", e.getMessage(), e);
                            }
                        })));
                snmpTransportContext.getTelemetryMapping(deviceProfileId, vb.getOid()).ifPresent(kvMapping -> transportService.process(DeviceTransportType.DEFAULT,
                        TransportProtos.ValidateDeviceTokenRequestMsg.newBuilder().setToken(sessionContext.getToken()).build(),
                        new DeviceAuthCallback(snmpTransportContext, sessionInfo -> {
                            try {
                                transportService.process(sessionInfo,
                                        convertToPostTelemetry(kvMapping.getKey(), kvMapping.getType(), vb.toValueString()),
                                        TransportServiceCallback.EMPTY);
                                reportActivity(sessionInfo);

                            } catch (Exception e) {
                                log.warn("Failed to process SNMP response: {}", e.getMessage(), e);
                            }
                        })));
            }
        } else {
            log.warn("No SNMP response, requestId: {}", event.getRequest().getRequestID());
        }
    }

    private void reportActivity(TransportProtos.SessionInfoProto sessionInfo) {
        snmpTransportContext.getTransportService().process(sessionInfo, TransportProtos.SubscriptionInfoProto.newBuilder()
                .setAttributeSubscription(false)
                .setRpcSubscription(false)
                .setLastActivityTime(System.currentTimeMillis())
                .build(), TransportServiceCallback.EMPTY);
    }

    private TransportProtos.PostAttributeMsg convertToPostAttributes(String keyName, DataType dataType, String payload) throws AdaptorException {
        try {
            return JsonConverter.convertToAttributesProto(getKvJson(keyName, dataType, payload));
        } catch (IllegalStateException | JsonSyntaxException ex) {
            //TODO: change the exception type
            throw new AdaptorException(ex);
        }
    }

    private TransportProtos.PostTelemetryMsg convertToPostTelemetry(String keyName, DataType dataType, String payload) throws AdaptorException {
        try {
            return JsonConverter.convertToTelemetryProto(getKvJson(keyName, dataType, payload));
        } catch (IllegalStateException | JsonSyntaxException ex) {
            //TODO: change the exception type
            throw new AdaptorException(ex);
        }
    }

    private JsonElement getKvJson(String keyName, DataType dataType, String payload) throws AdaptorException {
        JsonObject result = new JsonObject();
        switch (dataType) {
            case LONG:
                result.addProperty(keyName, Long.parseLong(payload));
                break;
            case BOOLEAN:
                result.addProperty(keyName, Boolean.parseBoolean(payload));
                break;
            case DOUBLE:
                result.addProperty(keyName, Double.parseDouble(payload));
                break;
            case STRING:
                result.addProperty(keyName, payload);
                break;
            default:
                //TODO: change the exception type
                throw new AdaptorException("Unsupported data type");
        }
        return new JsonParser().parse(result.toString());
    }

    @AllArgsConstructor
    private static class DeviceAuthCallback implements TransportServiceCallback<ValidateDeviceCredentialsResponse> {
        private final TransportContext transportContext;
        private final Consumer<TransportProtos.SessionInfoProto> onSuccess;

        @Override
        public void onSuccess(ValidateDeviceCredentialsResponse msg) {
            if (msg.hasDeviceInfo()) {
                onSuccess.accept(SessionInfoCreator.create(msg, transportContext, UUID.randomUUID()));
            } else {
                log.warn("Failed to process device auth");
            }
        }

        @Override
        public void onError(Throwable e) {
            log.warn("Failed to process device auth", e);
        }
    }

    @Override
    public String getName() {
        return "snmp";
    }
}
