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
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.monitoring.config.transport.HttpTransportMonitoringConfig;
import org.thingsboard.monitoring.config.transport.TransportMonitoringTarget;
import org.thingsboard.monitoring.config.transport.TransportType;
import org.thingsboard.monitoring.service.transport.TransportHealthChecker;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class HttpTransportHealthChecker extends TransportHealthChecker<HttpTransportMonitoringConfig> {

    static final long POLL_TIMEOUT_MS = 1000L;
    private static final long POLL_READ_TIMEOUT_SLACK_MS = 1000L;
    private static final long POLL_BACKOFF_INITIAL_MS = 500L;
    private static final long POLL_BACKOFF_MAX_MS = 5_000L;
    private static final long SHUTDOWN_TIMEOUT_MS = 5_000L;
    private static final AtomicInteger POOL_COUNTER = new AtomicInteger();

    private RestTemplate restTemplate;
    private ScheduledExecutorService rpcPoller;
    private Future<?> rpcPollFuture;
    private long backoffMs;

    protected HttpTransportHealthChecker(HttpTransportMonitoringConfig config, TransportMonitoringTarget target) {
        super(config, target);
    }

    @Override
    protected void initClient() throws Exception {
        if (restTemplate == null) {
            restTemplate = new RestTemplateBuilder()
                    .setConnectTimeout(Duration.ofMillis(config.getRequestTimeoutMs()))
                    .setReadTimeout(Duration.ofMillis(POLL_TIMEOUT_MS + POLL_READ_TIMEOUT_SLACK_MS))
                    .build();
            log.debug("Initialized HTTP client");
        }
        if (target.isRpcEnabled() && (rpcPollFuture == null || rpcPollFuture.isDone())) {
            if (rpcPoller == null || rpcPoller.isShutdown()) {
                rpcPoller = Executors.newSingleThreadScheduledExecutor(daemonThreadFactory());
            }
            backoffMs = 0L;
            rpcPollFuture = rpcPoller.scheduleWithFixedDelay(this::pollTask, 0L, 1L, TimeUnit.MILLISECONDS);
            log.debug("Started HTTP RPC poll loop for device {}", target.getDeviceId());
        }
    }

    private ThreadFactory daemonThreadFactory() {
        String name = "http-rpc-poll-" + POOL_COUNTER.incrementAndGet() + "-" + target.getDeviceId();
        return r -> {
            Thread t = new Thread(r, name);
            t.setDaemon(true);
            return t;
        };
    }

    void pollTask() {
        try {
            pollOnce();
            backoffMs = 0L;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.warn("HTTP RPC poll error: {}", e.getMessage());
            try {
                Thread.sleep(nextBackoffMs());
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private long nextBackoffMs() {
        long base = backoffMs == 0L ? POLL_BACKOFF_INITIAL_MS : Math.min(backoffMs * 2L, POLL_BACKOFF_MAX_MS);
        backoffMs = base;
        long jitter = ThreadLocalRandom.current().nextLong(0L, Math.max(1L, base / 2L));
        return base + jitter;
    }

    void pollOnce() throws InterruptedException {
        String accessToken = target.getDevice().getCredentials().getCredentialsId();
        // POLL_TIMEOUT_MS is sent to the server as the long-poll wait window.
        // The RestTemplate read timeout above is sized to POLL_TIMEOUT_MS + slack so
        // a slow server still terminates within bounded time and destroyClient() can
        // unblock the poller via Future#cancel(true).
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
        if (idNode == null || !idNode.isNumber()) {
            log.debug("HTTP RPC poll response missing or non-numeric id: {}", rpc);
            return;
        }
        String responseUrl = target.getBaseUrl() + "/api/v1/" + accessToken + "/rpc/" + idNode.asLong();
        JsonNode body = params == null ? JacksonUtil.newObjectNode() : params;
        restTemplate.postForLocation(responseUrl, body);
    }

    @Override
    protected void sendTestPayload(String payload) throws Exception {
        String accessToken = target.getDevice().getCredentials().getCredentialsId();
        restTemplate.postForObject(target.getBaseUrl() + "/api/v1/" + accessToken + "/telemetry", payload, String.class);
    }

    // Package-access bridge: TransportHealthChecker#doRpcCheck is `protected` and lives in
    // a different package than the test, so the unit test can only call it via a same-package
    // subclass override. Keep this delegating override to preserve that test seam.
    @Override
    protected void doRpcCheck() throws Exception {
        super.doRpcCheck();
    }

    @Override
    protected void destroyClient() throws Exception {
        if (rpcPollFuture != null) {
            rpcPollFuture.cancel(true);
            rpcPollFuture = null;
        }
        if (rpcPoller != null) {
            rpcPoller.shutdownNow();
            try {
                if (!rpcPoller.awaitTermination(SHUTDOWN_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                    log.warn("HTTP RPC poller did not terminate within {} ms", SHUTDOWN_TIMEOUT_MS);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            rpcPoller = null;
            log.debug("Stopped HTTP RPC poller");
        }
    }

    @Override
    protected TransportType getTransportType() {
        return TransportType.HTTP;
    }

}
