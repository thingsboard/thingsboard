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
package org.thingsboard.server.transport.snmp.session;

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
import org.snmp4j.event.ResponseListener;
import org.snmp4j.smi.VariableBinding;
import org.thingsboard.server.common.data.DeviceTransportType;
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

import java.util.UUID;
import java.util.function.Consumer;

@Slf4j
@AllArgsConstructor
public class SnmpSessionListener implements ResponseListener {

    @Getter
    private final SnmpTransportContext snmpTransportContext;

    @Getter
    private final String token;

    @Override
    public void onResponse(ResponseEvent event) {
        ((Snmp) event.getSource()).cancel(event.getRequest(), this);

        //TODO: Make data processing in another thread pool - parse and save attributes and telemetry
        PDU response = event.getResponse();
        if (event.getError() != null) {
            log.warn("Response error: {}", event.getError().getMessage(), event.getError());
            return;
        }

        if (response != null) {
            DeviceProfileId deviceProfileId = (DeviceProfileId) event.getUserObject();
            TransportService transportService = snmpTransportContext.getTransportService();
            for (int i = 0; i < response.size(); i++) {
                VariableBinding vb = response.get(i);
                snmpTransportContext.findAttributesMapping(deviceProfileId, vb.getOid()).ifPresent(kvMapping -> transportService.process(DeviceTransportType.DEFAULT,
                        TransportProtos.ValidateDeviceTokenRequestMsg.newBuilder().setToken(token).build(),
                        new DeviceAuthCallback(snmpTransportContext, sessionInfo -> {
                            try {
                                log.debug("[{}] Processing SNMP response: {}", response.getRequestID(), response);
                                transportService.process(sessionInfo,
                                        convertToPostAttributes(kvMapping.getKey(), kvMapping.getType(), vb.toValueString()),
                                        TransportServiceCallback.EMPTY);
                                reportActivity(sessionInfo);
                            } catch (Exception e) {
                                log.error("Failed to process SNMP response: {}", e.getMessage(), e);
                            }
                        })));
                snmpTransportContext.findTelemetryMapping(deviceProfileId, vb.getOid()).ifPresent(kvMapping -> transportService.process(DeviceTransportType.DEFAULT,
                        TransportProtos.ValidateDeviceTokenRequestMsg.newBuilder().setToken(token).build(),
                        new DeviceAuthCallback(snmpTransportContext, sessionInfo -> {
                            try {
                                transportService.process(sessionInfo,
                                        convertToPostTelemetry(kvMapping.getKey(), kvMapping.getType(), vb.toValueString()),
                                        TransportServiceCallback.EMPTY);
                                reportActivity(sessionInfo);

                            } catch (Exception e) {
                                log.error("Failed to process SNMP response: {}", e.getMessage(), e);
                            }
                        })));
            }
        } else {
            log.warn("No SNMP response, requestId: {}", event.getRequest().getRequestID());
        }
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

    private void reportActivity(TransportProtos.SessionInfoProto sessionInfo) {
        snmpTransportContext.getTransportService().process(sessionInfo, TransportProtos.SubscriptionInfoProto.newBuilder()
                .setAttributeSubscription(false)
                .setRpcSubscription(false)
                .setLastActivityTime(System.currentTimeMillis())
                .build(), TransportServiceCallback.EMPTY);
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
}
