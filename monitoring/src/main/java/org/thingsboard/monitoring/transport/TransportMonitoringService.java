/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.monitoring.transport;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.monitoring.client.TbClient;
import org.thingsboard.monitoring.client.WsClient;
import org.thingsboard.monitoring.client.WsClientFactory;
import org.thingsboard.monitoring.config.DeviceConfig;
import org.thingsboard.monitoring.config.MonitoringTargetConfig;
import org.thingsboard.monitoring.config.TransportType;
import org.thingsboard.monitoring.config.service.TransportMonitoringConfig;
import org.thingsboard.monitoring.data.Latencies;
import org.thingsboard.monitoring.data.MonitoredServiceKey;
import org.thingsboard.monitoring.service.MonitoringReporter;
import org.thingsboard.monitoring.util.ResourceUtils;
import org.thingsboard.monitoring.util.TbStopWatch;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MBootstrapClientCredentials;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MDeviceCredentials;
import org.thingsboard.server.common.data.device.credentials.lwm2m.NoSecBootstrapClientCredential;
import org.thingsboard.server.common.data.device.credentials.lwm2m.NoSecClientCredential;
import org.thingsboard.server.common.data.device.data.DefaultDeviceConfiguration;
import org.thingsboard.server.common.data.device.data.DefaultDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.data.DeviceData;
import org.thingsboard.server.common.data.device.data.Lwm2mDeviceTransportConfiguration;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;

import javax.annotation.PostConstruct;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public final class TransportMonitoringService {

    private final List<TransportMonitoringConfig> configs;
    private final List<TransportHealthChecker<?>> transportHealthCheckers = new LinkedList<>();
    private final List<UUID> devices = new LinkedList<>();

    private final TbClient tbClient;
    private final WsClientFactory wsClientFactory;
    private final TbStopWatch stopWatch;
    private final MonitoringReporter reporter;
    private final ApplicationContext applicationContext;
    private ScheduledExecutorService scheduler;
    @Value("${monitoring.transports.monitoring_rate_ms}")
    private int monitoringRateMs;

    @PostConstruct
    private void init() {
        configs.forEach(config -> {
            config.getTargets().stream()
                    .filter(target -> StringUtils.isNotBlank(target.getBaseUrl()))
                    .peek(target -> checkMonitoringTarget(config, target, tbClient))
                    .forEach(target -> {
                        TransportHealthChecker<?> transportHealthChecker = applicationContext.getBean(config.getTransportType().getServiceClass(), config, target);
                        transportHealthCheckers.add(transportHealthChecker);
                        devices.add(target.getDevice().getId());
                    });
        });
        scheduler = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("monitoring-executor"));
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startMonitoring() {
        scheduler.scheduleWithFixedDelay(() -> {
            try {
                log.debug("Starting transports check");
                stopWatch.start();
                String accessToken = tbClient.logIn();
                reporter.reportLatency(Latencies.LOG_IN, stopWatch.getTime());

                try (WsClient wsClient = wsClientFactory.createClient(accessToken)) {
                    wsClient.subscribeForTelemetry(devices, TransportHealthChecker.TEST_TELEMETRY_KEY).waitForReply();

                    for (TransportHealthChecker<?> transportHealthChecker : transportHealthCheckers) {
                        transportHealthChecker.check(wsClient);
                    }
                }
                reporter.reportLatencies(tbClient);
                log.debug("Finished transports check");
            } catch (Throwable error) {
                try {
                    reporter.serviceFailure(MonitoredServiceKey.GENERAL, error);
                } catch (Throwable reportError) {
                    log.error("Error occurred during service failure reporting", reportError);
                }
            }
        }, 0, monitoringRateMs, TimeUnit.MILLISECONDS);
    }

    private void checkMonitoringTarget(TransportMonitoringConfig config, MonitoringTargetConfig target, TbClient tbClient) {
        DeviceConfig deviceConfig = target.getDevice();
        tbClient.logIn();

        DeviceId deviceId;
        if (deviceConfig == null || deviceConfig.getId() == null) {
            String deviceName = String.format("[%s] Monitoring device (%s)", config.getTransportType(), target.getBaseUrl());
            Device device = tbClient.getTenantDevice(deviceName)
                    .orElseGet(() -> {
                        log.info("Creating new device '{}'", deviceName);
                        return createDevice(config.getTransportType(), deviceName, tbClient);
                    });
            deviceId = device.getId();
            target.getDevice().setId(deviceId.toString());
        } else {
            deviceId = new DeviceId(deviceConfig.getId());
        }

        log.info("Using device {} for {} monitoring", deviceId, config.getTransportType());
        DeviceCredentials credentials = tbClient.getDeviceCredentialsByDeviceId(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("No credentials found for device " + deviceId));
        target.getDevice().setCredentials(credentials);
    }

    private Device createDevice(TransportType transportType, String name, TbClient tbClient) {
        Device device = new Device();
        device.setName(name);

        DeviceCredentials credentials = new DeviceCredentials();
        credentials.setCredentialsId(RandomStringUtils.randomAlphabetic(20));

        DeviceData deviceData = new DeviceData();
        deviceData.setConfiguration(new DefaultDeviceConfiguration());
        if (transportType != TransportType.LWM2M) {
            device.setType("default");
            deviceData.setTransportConfiguration(new DefaultDeviceTransportConfiguration());
            credentials.setCredentialsType(DeviceCredentialsType.ACCESS_TOKEN);
        } else {
            tbClient.getResources(new PageLink(1, 0, "lwm2m monitoring")).getData()
                    .stream().findFirst()
                    .orElseGet(() -> {
                        TbResource newResource = ResourceUtils.getResource("lwm2m/resource.json", TbResource.class);
                        log.info("Creating LwM2M resource");
                        return tbClient.saveResource(newResource);
                    });
            String profileName = "LwM2M Monitoring";
            DeviceProfile profile = tbClient.getDeviceProfiles(new PageLink(1, 0, profileName)).getData()
                    .stream().findFirst()
                    .orElseGet(() -> {
                        DeviceProfile newProfile = ResourceUtils.getResource("lwm2m/device_profile.json", DeviceProfile.class);
                        newProfile.setName(profileName);
                        log.info("Creating LwM2M device profile");
                        return tbClient.saveDeviceProfile(newProfile);
                    });
            device.setType(profileName);
            device.setDeviceProfileId(profile.getId());
            deviceData.setTransportConfiguration(new Lwm2mDeviceTransportConfiguration());

            credentials.setCredentialsType(DeviceCredentialsType.LWM2M_CREDENTIALS);
            LwM2MDeviceCredentials lwm2mCreds = new LwM2MDeviceCredentials();
            NoSecClientCredential client = new NoSecClientCredential();
            client.setEndpoint(credentials.getCredentialsId());
            lwm2mCreds.setClient(client);
            LwM2MBootstrapClientCredentials bootstrap = new LwM2MBootstrapClientCredentials();
            bootstrap.setBootstrapServer(new NoSecBootstrapClientCredential());
            bootstrap.setLwm2mServer(new NoSecBootstrapClientCredential());
            lwm2mCreds.setBootstrap(bootstrap);
            credentials.setCredentialsValue(JacksonUtil.toString(lwm2mCreds));
        }
        return tbClient.saveDeviceWithCredentials(device, credentials).get();
    }

}
