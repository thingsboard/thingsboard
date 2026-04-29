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
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.monitoring.client.Lwm2mClient;
import org.thingsboard.monitoring.config.transport.Lwm2mTransportMonitoringConfig;
import org.thingsboard.monitoring.config.transport.RpcInfo;
import org.thingsboard.monitoring.config.transport.TransportMonitoringTarget;
import org.thingsboard.monitoring.config.transport.TransportType;
import org.thingsboard.monitoring.data.ServiceFailureException;
import org.thingsboard.monitoring.service.transport.TransportHealthChecker;
import org.thingsboard.server.common.data.id.DeviceId;

@Service
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class Lwm2mTransportHealthChecker extends TransportHealthChecker<Lwm2mTransportMonitoringConfig> {

    private Lwm2mClient lwm2mClient;

    protected Lwm2mTransportHealthChecker(Lwm2mTransportMonitoringConfig config, TransportMonitoringTarget target) {
        super(config, target);
    }

    @Override
    protected void initClient() throws Exception {
        if (lwm2mClient == null || lwm2mClient.getLeshanClient() == null || lwm2mClient.isDestroyed()) {
            String endpoint = target.getDevice().getCredentials().getCredentialsId();
            lwm2mClient = new Lwm2mClient(target.getBaseUrl(), endpoint);
            lwm2mClient.initClient();
            log.debug("Initialized LwM2M client for endpoint '{}'", endpoint);
        }
    }

    @Override
    protected void sendTestPayload(String payload) throws Exception {
        lwm2mClient.send(payload, 0);
    }

    @Override
    protected String createTestPayload(String testValue) {
        return testValue;
    }

    private static final String READ_RESOURCE_PATH = "/3/0/0";

    @Override
    protected void doRpcCheck() throws Exception {
        if (!target.isRpcEnabled()) {
            return;
        }
        RpcInfo rpcInfo = getRpcInfo();
        ObjectNode body = JacksonUtil.newObjectNode();
        body.put("method", "Read");
        body.set("params", JacksonUtil.newObjectNode().put("key", READ_RESOURCE_PATH));
        body.put("timeout", getRpcTimeoutMs());

        long start = System.nanoTime();
        JsonNode response;
        try {
            response = tbClient.handleTwoWayDeviceRPCRequest(new DeviceId(target.getDeviceId()), body);
        } catch (Throwable e) {
            throw new ServiceFailureException(rpcInfo, e);
        }
        String result = response == null ? null : response.asText(null);
        if (result == null || result.isBlank()) {
            throw new ServiceFailureException(rpcInfo,
                    "LwM2M RPC Read on " + READ_RESOURCE_PATH + " returned blank result");
        }
        reportRpcLatency(System.nanoTime() - start);
    }

    @Override
    protected void destroyClient() throws Exception {
        if (lwm2mClient != null) {
            lwm2mClient.destroy();
            lwm2mClient = null;
        }
    }

    @Override
    protected TransportType getTransportType() {
        return TransportType.LWM2M;
    }

}
