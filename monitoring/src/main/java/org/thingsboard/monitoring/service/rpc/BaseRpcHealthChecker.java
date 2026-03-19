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
package org.thingsboard.monitoring.service.rpc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.monitoring.client.TbClient;
import org.thingsboard.monitoring.client.WsClient;
import org.thingsboard.monitoring.config.rpc.RpcMonitoringConfig;
import org.thingsboard.monitoring.config.rpc.RpcMonitoringTarget;
import org.thingsboard.monitoring.config.rpc.RpcTransportType;
import org.thingsboard.monitoring.data.Latencies;
import org.thingsboard.monitoring.data.MonitoredServiceKey;
import org.thingsboard.monitoring.data.ServiceFailureException;
import org.thingsboard.monitoring.service.BaseHealthChecker;
import org.thingsboard.server.common.data.id.DeviceId;

import java.util.UUID;

@Slf4j
public abstract class BaseRpcHealthChecker<C extends RpcMonitoringConfig> extends BaseHealthChecker<C, RpcMonitoringTarget> {

    @Autowired
    private TbClient tbClient;

    protected BaseRpcHealthChecker(C config, RpcMonitoringTarget target) {
        super(config, target);
    }

    @Override
    protected void initialize() {
        try {
            initClient();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize RPC health checker for " + getInfo(), e);
        }
    }

    @Override
    public void check(WsClient wsClient) {
        log.debug("[{}] Checking RPC", getInfo());
        try {
            String testValue = UUID.randomUUID().toString();

            ObjectNode requestBody = JacksonUtil.newObjectNode();
            requestBody.put("method", "monitoringCheck");
            requestBody.set("params", JacksonUtil.newObjectNode().put("value", testValue));
            requestBody.put("timeout", config.getRequestTimeoutMs());

            stopWatch.start();
            JsonNode response;
            try {
                response = tbClient.handleTwoWayDeviceRPCRequest(new DeviceId(target.getDeviceId()), requestBody);
            } catch (Throwable e) {
                throw new ServiceFailureException(getInfo(), e);
            }
            reporter.reportLatency(Latencies.rpcRoundTrip(getKey()), stopWatch.getTime());

            String actualValue = response != null ? response.path("value").asText(null) : null;
            if (!testValue.equals(actualValue)) {
                String got = response != null ? response.toString() : "null";
                throw new ServiceFailureException(getInfo(), "Expected value " + testValue + " but got " + got);
            }

            reporter.serviceIsOk(getInfo());
            reporter.serviceIsOk(MonitoredServiceKey.GENERAL);
        } catch (ServiceFailureException e) {
            reporter.serviceFailure(e.getServiceKey(), e);
        } catch (Exception e) {
            reporter.serviceFailure(MonitoredServiceKey.GENERAL, e);
        }
    }

    @Override
    protected String createTestPayload(String testValue) {
        return testValue;
    }

    @Override
    protected void sendTestPayload(String payload) throws Exception {
        // not used — check() drives the flow directly
    }

    @Override
    protected Object getInfo() {
        return String.format("*%s RPC* %s (%s)", getTransportType().getName(), target.getLabel(), target.getBaseUrl());
    }

    @Override
    protected String getKey() {
        return getTransportType().name().toLowerCase() + "Rpc_" + target.getLabel();
    }

    @Override
    protected boolean isCfMonitoringEnabled() {
        return false;
    }

    protected abstract RpcTransportType getTransportType();

}
