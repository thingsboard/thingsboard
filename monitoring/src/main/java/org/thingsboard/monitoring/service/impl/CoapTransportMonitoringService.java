/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.monitoring.service.impl;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.thingsboard.monitoring.config.TransportType;
import org.thingsboard.monitoring.config.MonitoringTargetConfig;
import org.thingsboard.monitoring.config.service.CoapTransportMonitoringServiceConfig;
import org.thingsboard.monitoring.service.TransportMonitoringService;

import java.io.IOException;

@Service
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class CoapTransportMonitoringService extends TransportMonitoringService<CoapTransportMonitoringServiceConfig> {

    private CoapClient coapClient;

    protected CoapTransportMonitoringService(CoapTransportMonitoringServiceConfig config, MonitoringTargetConfig target) {
        super(config, target);
    }

    @Override
    protected void initClient() throws Exception {
        String uri = target.getBaseUrl() + "/api/v1/" + target.getDevice().getAccessToken() + "/telemetry";
        coapClient = new CoapClient(uri);
        coapClient.setTimeout((long) config.getRequestTimeoutMs());
    }

    @Override
    protected void sendTestPayload(String payload) throws Exception {
        CoapResponse response = coapClient.post(payload, MediaTypeRegistry.APPLICATION_JSON);
        CoAP.ResponseCode code = response.getCode();
        if (code.codeClass != CoAP.CodeClass.SUCCESS_RESPONSE.value) {
            throw new IOException("COAP client didn't receive success response from transport");
        }
    }

    @Override
    protected void destroyClient() throws Exception {
        coapClient.shutdown();
        coapClient = null;
    }

    @Override
    protected TransportType getTransportType() {
        return TransportType.COAP;
    }

}
