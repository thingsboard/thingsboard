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
package org.thingsboard.monitoring.service.transport.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.elements.config.SystemConfig;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.monitoring.config.transport.CoapTransportMonitoringConfig;
import org.thingsboard.monitoring.config.transport.TransportMonitoringTarget;
import org.thingsboard.monitoring.config.transport.TransportType;
import org.thingsboard.monitoring.service.transport.TransportHealthChecker;

import java.io.IOException;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class CoapTransportHealthChecker extends TransportHealthChecker<CoapTransportMonitoringConfig> {

    static {
        SystemConfig.register();
    }

    private CoapClient coapClient;
    @VisibleForTesting
    CoapClient rpcCoapClient;
    @VisibleForTesting
    CoapObserveRelation rpcObserveRelation;

    protected CoapTransportHealthChecker(CoapTransportMonitoringConfig config, TransportMonitoringTarget target) {
        super(config, target);
    }

    @Override
    protected void initClient() throws Exception {
        if (coapClient == null) {
            String accessToken = target.getDevice().getCredentials().getCredentialsId();
            String uri = target.getBaseUrl() + "/api/v1/" + accessToken + "/telemetry";
            coapClient = new CoapClient(uri);
            coapClient.setTimeout((long) config.getRequestTimeoutMs());
            log.debug("Initialized CoAP client for URI {}", uri);
        }
        if (target.isRpcEnabled() && rpcObserveRelation == null) {
            String accessToken = target.getDevice().getCredentials().getCredentialsId();
            String rpcUri = target.getBaseUrl() + "/api/v1/" + accessToken + "/rpc";
            if (rpcCoapClient == null) {
                rpcCoapClient = new CoapClient(rpcUri);
                rpcCoapClient.setTimeout((long) config.getRequestTimeoutMs());
            }
            rpcObserveRelation = rpcCoapClient.observe(new CoapHandler() {
                @Override
                public void onLoad(CoapResponse response) {
                    handleRpcNotification(response);
                }

                @Override
                public void onError() {
                    // Sustained observe failure surfaces indirectly: doRpcCheck()'s
                    // server-side RPC send will time out without an echo from this
                    // device, which is the actual signal we want to report.
                    log.debug("CoAP RPC observe failed");
                }
            });
            log.debug("Started CoAP RPC observe on {}", rpcUri);
        }
    }

    @VisibleForTesting
    void handleRpcNotification(CoapResponse response) {
        try {
            String body = response == null ? null : response.getResponseText();
            if (body == null || body.isEmpty()) {
                return;
            }
            JsonNode rpc = JacksonUtil.toJsonNode(body);
            JsonNode idNode = rpc == null ? null : rpc.get("id");
            if (idNode == null || !idNode.isNumber()) {
                log.debug("CoAP RPC notification missing or non-numeric id: {}", body);
                return;
            }
            JsonNode params = rpc.get("params");
            String accessToken = target.getDevice().getCredentials().getCredentialsId();
            String responseUri = target.getBaseUrl() + "/api/v1/" + accessToken + "/rpc/" + idNode.asLong();
            String payload = params == null ? "{}" : JacksonUtil.toString(params);
            postRpcResponse(responseUri, payload);
        } catch (Exception e) {
            log.warn("CoAP RPC echo failed: {}", e.getMessage());
        }
    }

    // Allocates a fresh CoapClient per echo because the observe relation owns
    // rpcCoapClient and reusing it via setURI(...) would race with incoming
    // observe notifications on the same client. The cost is one short-lived
    // client per RPC (rare event). Construction is inside the try block so a
    // future ctor change that opens a socket can't leak it on init failure —
    // the null-guarded shutdown() in finally still runs.
    @VisibleForTesting
    void postRpcResponse(String uri, String payload) {
        CoapClient client = null;
        try {
            client = new CoapClient(uri);
            client.setTimeout((long) config.getRequestTimeoutMs());
            client.post(payload, MediaTypeRegistry.APPLICATION_JSON);
        } catch (Exception e) {
            log.debug("CoAP RPC response post failed for {}: {}", uri, e.getMessage());
        } finally {
            if (client != null) {
                client.shutdown();
            }
        }
    }

    @Override
    protected void sendTestPayload(String payload) throws Exception {
        CoapResponse response = coapClient.post(payload, MediaTypeRegistry.APPLICATION_JSON);
        CoAP.ResponseCode code = response.getCode();
        if (code.codeClass != CoAP.CodeClass.SUCCESS_RESPONSE.value) {
            throw new IOException("COAP client didn't receive success response from transport");
        }
    }

    @VisibleForTesting
    @Override
    protected void doRpcCheck() throws Exception {
        super.doRpcCheck();
    }

    @Override
    protected void destroyClient() throws Exception {
        if (rpcObserveRelation != null) {
            rpcObserveRelation.proactiveCancel();
            rpcObserveRelation = null;
        }
        if (rpcCoapClient != null) {
            rpcCoapClient.shutdown();
            rpcCoapClient = null;
        }
        if (coapClient != null) {
            coapClient.shutdown();
            coapClient = null;
            log.info("Disconnected CoAP client");
        }
    }

    @Override
    protected TransportType getTransportType() {
        return TransportType.COAP;
    }

}
