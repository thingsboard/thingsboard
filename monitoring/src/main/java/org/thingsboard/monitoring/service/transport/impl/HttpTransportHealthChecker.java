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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.monitoring.config.transport.HttpTransportMonitoringConfig;
import org.thingsboard.monitoring.config.transport.TransportMonitoringTarget;
import org.thingsboard.monitoring.config.transport.TransportType;
import org.thingsboard.monitoring.service.transport.TransportHealthChecker;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class HttpTransportHealthChecker extends TransportHealthChecker<HttpTransportMonitoringConfig> {

    private static final long POLL_TIMEOUT_MS = 1000L;

    RestTemplate restTemplate;
    private final AtomicBoolean rpcPolling = new AtomicBoolean();
    private Thread rpcPollThread;

    protected HttpTransportHealthChecker(HttpTransportMonitoringConfig config, TransportMonitoringTarget target) {
        super(config, target);
    }

    @Override
    protected void initClient() throws Exception {
        if (restTemplate == null) {
            restTemplate = new RestTemplateBuilder()
                    .setConnectTimeout(Duration.ofMillis(config.getRequestTimeoutMs()))
                    .setReadTimeout(Duration.ofMillis(config.getRequestTimeoutMs()))
                    .build();
            log.debug("Initialized HTTP client");
        }
        if (target.isRpcEnabled() && rpcPolling.compareAndSet(false, true)) {
            rpcPollThread = new Thread(this::rpcPollLoop, "http-rpc-poll-" + target.getDeviceId());
            rpcPollThread.setDaemon(true);
            rpcPollThread.start();
            log.debug("Started HTTP RPC poll thread for device {}", target.getDeviceId());
        }
    }

    private void rpcPollLoop() {
        while (rpcPolling.get()) {
            try {
                pollOnce();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Throwable e) {
                log.debug("HTTP RPC poll error: {}", e.getMessage());
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    void pollOnce() throws InterruptedException {
        String accessToken = target.getDevice().getCredentials().getCredentialsId();
        String pollUrl = target.getBaseUrl() + "/api/v1/" + accessToken + "/rpc?timeout=" + POLL_TIMEOUT_MS;
        ResponseEntity<JsonNode> poll;
        try {
            poll = restTemplate.getForEntity(pollUrl, JsonNode.class);
        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode() == HttpStatus.REQUEST_TIMEOUT || e.getStatusCode() == HttpStatus.NO_CONTENT) {
                return;
            }
            throw e;
        }
        if (poll.getStatusCode() != HttpStatus.OK || poll.getBody() == null) {
            return;
        }
        JsonNode rpc = poll.getBody();
        JsonNode idNode = rpc.get("id");
        JsonNode params = rpc.get("params");
        if (idNode == null) {
            log.debug("HTTP RPC poll response missing id: {}", rpc);
            return;
        }
        String responseUrl = target.getBaseUrl() + "/api/v1/" + accessToken + "/rpc/" + idNode.asLong();
        restTemplate.postForLocation(responseUrl, params == null ? null : params);
    }

    @Override
    protected void sendTestPayload(String payload) throws Exception {
        String accessToken = target.getDevice().getCredentials().getCredentialsId();
        restTemplate.postForObject(target.getBaseUrl() + "/api/v1/" + accessToken + "/telemetry", payload, String.class);
    }

    @Override
    protected void doRpcCheck() throws Exception {
        super.doRpcCheck();
    }

    @Override
    protected void destroyClient() throws Exception {
        if (rpcPolling.compareAndSet(true, false) && rpcPollThread != null) {
            rpcPollThread.interrupt();
            rpcPollThread = null;
            log.debug("Stopped HTTP RPC poll thread");
        }
    }

    @Override
    protected TransportType getTransportType() {
        return TransportType.HTTP;
    }

}
