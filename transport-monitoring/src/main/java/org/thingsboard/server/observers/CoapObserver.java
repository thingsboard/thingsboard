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
package org.thingsboard.server.observers;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.elements.exception.ConnectorException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.thingsboard.server.transport.TransportType;
import org.thingsboard.server.utils.AccessTokenHttpProvider;
import org.thingsboard.server.websocket.WebSocketClientImpl;

import java.io.IOException;
import java.util.UUID;

@Component
@ConditionalOnProperty(
        value="coap.enabled",
        havingValue = "true")
@Slf4j
public class CoapObserver extends AbstractTransportObserver {

    private CoapClient coapClient;

    @Value("${coap.host}")
    private String coapBaseUrl;

    @Value("${coap.monitoring_rate}")
    private int monitoringRate;

    @Value("${coap.test_device.id}")
    private UUID testDeviceUuid;

    @Value("${coap.test_device.access_token}")
    private String testDeviceAccessToken;

    @Value("${coap.timeout}")
    private long timeout;

    public CoapObserver(AccessTokenHttpProvider tokenHttpProvider) {
        super(tokenHttpProvider);
    }

    @Override
    protected void publishMsg(String payload) throws Exception {
        if (coapClient == null) {
            coapClient = buildCoapClient();
        }
        CoapResponse response = coapClient.setTimeout(timeout).post(payload, MediaTypeRegistry.APPLICATION_JSON);
        CoAP.ResponseCode code = response.getCode();
        if (code.codeClass != CoAP.CodeClass.SUCCESS_RESPONSE.value) {
            throw new IOException("COAP client didn't receive success response from transport");
        }
    }

    @Override
    public UUID getTestDeviceUuid() {
        return testDeviceUuid;
    }

    @Override
    public int getMonitoringRate() {
        return monitoringRate;
    }

    @Override
    public TransportType getTransportType() {
        return TransportType.COAP;
    }

    private CoapClient buildCoapClient() {
        return new CoapClient(coapBaseUrl + "/api/v1/" + testDeviceAccessToken + "/" + "telemetry");
    }

}
