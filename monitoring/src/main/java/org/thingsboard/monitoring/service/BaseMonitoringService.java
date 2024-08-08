/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.monitoring.service;

import jakarta.annotation.PostConstruct;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.thingsboard.monitoring.client.TbClient;
import org.thingsboard.monitoring.client.WsClient;
import org.thingsboard.monitoring.client.WsClientFactory;
import org.thingsboard.monitoring.config.MonitoringConfig;
import org.thingsboard.monitoring.config.MonitoringTarget;
import org.thingsboard.monitoring.data.Latencies;
import org.thingsboard.monitoring.data.MonitoredServiceKey;
import org.thingsboard.monitoring.service.transport.TransportHealthChecker;
import org.thingsboard.monitoring.util.TbStopWatch;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
public abstract class BaseMonitoringService<C extends MonitoringConfig<T>, T extends MonitoringTarget> {

    @Autowired(required = false)
    private List<C> configs;
    private final List<BaseHealthChecker<C, T>> healthCheckers = new LinkedList<>();
    private final List<UUID> devices = new LinkedList<>();

    @Autowired
    private TbClient tbClient;
    @Autowired
    private WsClientFactory wsClientFactory;
    @Autowired
    private TbStopWatch stopWatch;
    @Autowired
    private MonitoringReporter reporter;
    @Autowired
    protected ApplicationContext applicationContext;

    @PostConstruct
    private void init() {
        if (configs == null || configs.isEmpty()) {
            return;
        }
        tbClient.logIn();
        configs.forEach(config -> {
            config.getTargets().forEach(target -> {
                BaseHealthChecker<C, T> healthChecker = initHealthChecker(target, config);
                healthCheckers.add(healthChecker);

                if (target.isCheckDomainIps()) {
                    getAssociatedUrls(target.getBaseUrl()).forEach(url -> {
                        healthChecker.getAssociates().put(url, initHealthChecker(createTarget(url), config));
                    });
                }
            });
        });
    }

    private BaseHealthChecker<C, T> initHealthChecker(T target, C config) {
        BaseHealthChecker<C, T> healthChecker = (BaseHealthChecker<C, T>) createHealthChecker(config, target);
        log.info("Initializing {} for {}", healthChecker.getClass().getSimpleName(), target.getBaseUrl());
        healthChecker.initialize(tbClient);
        devices.add(target.getDeviceId());
        return healthChecker;
    }

    public final void runChecks() {
        if (healthCheckers.isEmpty()) {
            return;
        }
        try {
            log.info("Starting {}", getName());
            stopWatch.start();
            String accessToken = tbClient.logIn();
            reporter.reportLatency(Latencies.LOG_IN, stopWatch.getTime());

            try (WsClient wsClient = wsClientFactory.createClient(accessToken)) {
                stopWatch.start();
                wsClient.subscribeForTelemetry(devices, TransportHealthChecker.TEST_TELEMETRY_KEY).waitForReply();
                reporter.reportLatency(Latencies.WS_SUBSCRIBE, stopWatch.getTime());

                for (BaseHealthChecker<C, T> healthChecker : healthCheckers) {
                    check(healthChecker, wsClient);
                }
            }
            reporter.reportLatencies(tbClient);
            log.debug("Finished {}", getName());
        } catch (Throwable error) {
            try {
                reporter.serviceFailure(MonitoredServiceKey.GENERAL, error);
            } catch (Throwable reportError) {
                log.error("Error occurred during service failure reporting", reportError);
            }
        }
    }

    private void check(BaseHealthChecker<C, T> healthChecker, WsClient wsClient) throws Exception {
        healthChecker.check(wsClient);

        T target = healthChecker.getTarget();
        if (target.isCheckDomainIps()) {
            Set<String> associatedUrls = getAssociatedUrls(target.getBaseUrl());
            Map<String, BaseHealthChecker<C, T>> associates = healthChecker.getAssociates();
            Set<String> prevAssociatedUrls = new HashSet<>(associates.keySet());

            boolean changed = false;
            for (String url : associatedUrls) {
                if (!prevAssociatedUrls.contains(url)) {
                    BaseHealthChecker<C, T> associate = initHealthChecker(createTarget(url), healthChecker.getConfig());
                    associates.put(url, associate);
                    changed = true;
                }
            }
            for (String url : prevAssociatedUrls) {
                if (!associatedUrls.contains(url)) {
                    stopHealthChecker(healthChecker);
                    associates.remove(url);
                    changed = true;
                }
            }
            if (changed) {
                log.info("Updated IPs for {}: {} (old list: {})", target.getBaseUrl(), associatedUrls, prevAssociatedUrls);
            }
        }
    }

    @SneakyThrows
    private Set<String> getAssociatedUrls(String baseUrl) {
        URI url = new URI(baseUrl);
        return Arrays.stream(InetAddress.getAllByName(url.getHost()))
                .map(InetAddress::getHostAddress)
                .map(ip -> {
                    try {
                        return new URI(url.getScheme(), null, ip, url.getPort(), "", null, null).toString();
                    } catch (URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toSet());
    }

    private void stopHealthChecker(BaseHealthChecker<C, T> healthChecker) throws Exception {
        healthChecker.destroyClient();
        devices.remove(healthChecker.getTarget().getDeviceId());
        log.info("Stopped {} for {}", healthChecker.getClass().getSimpleName(), healthChecker.getTarget().getBaseUrl());
    }

    protected abstract BaseHealthChecker<?, ?> createHealthChecker(C config, T target);

    protected abstract T createTarget(String baseUrl);

    protected abstract String getName();

}
