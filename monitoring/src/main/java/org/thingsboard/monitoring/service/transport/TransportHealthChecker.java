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
package org.thingsboard.monitoring.service.transport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.monitoring.client.TbClient;
import org.thingsboard.monitoring.config.transport.RpcInfo;
import org.thingsboard.monitoring.config.transport.TransportInfo;
import org.thingsboard.monitoring.config.transport.TransportMonitoringConfig;
import org.thingsboard.monitoring.config.transport.TransportMonitoringTarget;
import org.thingsboard.monitoring.config.transport.TransportType;
import org.thingsboard.monitoring.data.ServiceFailureException;
import org.thingsboard.monitoring.service.BaseHealthChecker;
import org.thingsboard.server.common.data.id.DeviceId;

import java.util.UUID;

@Slf4j
public abstract class TransportHealthChecker<C extends TransportMonitoringConfig> extends BaseHealthChecker<C, TransportMonitoringTarget> {

    @Value("${monitoring.calculated_fields.enabled:true}")
    private boolean calculatedFieldsMonitoringEnabled;

    @Value("${monitoring.rest.request_timeout_ms}")
    private int restRequestTimeoutMs;

    @Autowired
    protected TbClient tbClient;

    public TransportHealthChecker(C config, TransportMonitoringTarget target) {
        super(config, target);
    }

    protected RpcInfo getRpcInfo() {
        return new RpcInfo(new TransportInfo(getTransportType(), target));
    }

    protected int getRpcTimeoutMs() {
        Integer perTarget = target.getRpc() != null ? target.getRpc().getRequestTimeoutMs() : null;
        return perTarget != null ? perTarget : config.getRequestTimeoutMs();
    }

    @Override
    protected void doRpcCheck() throws Exception {
        if (!target.isRpcEnabled()) {
            return;
        }
        RpcInfo rpcInfo = getRpcInfo();
        String testValue = UUID.randomUUID().toString();
        ObjectNode body = JacksonUtil.newObjectNode();
        body.put("method", "monitoringCheck");
        body.set("params", JacksonUtil.newObjectNode().put("value", testValue));
        body.put("timeout", getRpcTimeoutMs());

        long start = System.nanoTime();
        JsonNode response;
        try {
            response = tbClient.handleTwoWayDeviceRPCRequest(new DeviceId(target.getDeviceId()), body);
        } catch (Throwable e) {
            throw new ServiceFailureException(rpcInfo, e);
        }
        String actual = response == null ? null : response.path("value").asText(null);
        if (!testValue.equals(actual)) {
            throw new ServiceFailureException(rpcInfo,
                    "RPC echo mismatch: expected " + testValue + " but got " + actual);
        }
        reportRpcLatency(System.nanoTime() - start);
    }

    @Override
    protected void initialize() {
        entityService.checkEntities(config, target);
        if (target.isRpcEnabled()) {
            int rpcTimeoutMs = getRpcTimeoutMs();
            if (rpcTimeoutMs >= restRequestTimeoutMs) {
                throw new IllegalStateException("RPC request timeout (" + rpcTimeoutMs + " ms) for "
                        + getTransportType() + " target " + target.getBaseUrl()
                        + " must be < monitoring.rest.request_timeout_ms (" + restRequestTimeoutMs
                        + " ms); otherwise tbClient times out before TB times out the RPC, producing false negatives.");
            }
        }
    }

    @Override
    protected String createTestPayload(String testValue) {
        return JacksonUtil.newObjectNode().set(TEST_TELEMETRY_KEY, new TextNode(testValue)).toString();
    }

    @Override
    protected Object getInfo() {
        return new TransportInfo(getTransportType(), target);
    }

    @Override
    protected String getKey() {
        return getTransportType().name().toLowerCase() + (target.getQueue().equals("Main") ? "" : target.getQueue()) + "Transport";
    }

    protected abstract TransportType getTransportType();

    @Override
    protected boolean isCfMonitoringEnabled() {
        return calculatedFieldsMonitoringEnabled;
    }

}
