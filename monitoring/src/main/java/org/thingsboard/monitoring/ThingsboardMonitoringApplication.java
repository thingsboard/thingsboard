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
package org.thingsboard.monitoring;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.monitoring.client.TbClient;
import org.thingsboard.monitoring.config.DeviceConfig;
import org.thingsboard.monitoring.config.MonitoringTargetConfig;
import org.thingsboard.monitoring.config.service.TransportMonitoringServiceConfig;
import org.thingsboard.monitoring.service.TransportMonitoringService;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.device.data.DefaultDeviceConfiguration;
import org.thingsboard.server.common.data.device.data.DefaultDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.data.DeviceData;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.security.DeviceCredentials;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@SpringBootApplication
@EnableScheduling
@Slf4j
public class ThingsboardMonitoringApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(ThingsboardMonitoringApplication.class)
                .properties(Map.of("spring.config.name", "tb-monitoring"))
                .run(args);
    }

    @Bean
    public ApplicationRunner initAndStartMonitoringServices(List<TransportMonitoringServiceConfig> configs, TbClient tbClient, ApplicationContext context) {
        return args -> {
            List<TransportMonitoringService<?>> monitoringServices = new LinkedList<>();
            configs.forEach(config -> {
                config.getTargets().stream()
                        .filter(target -> StringUtils.isNotBlank(target.getBaseUrl()))
                        .peek(target -> checkMonitoringTarget(config, target, tbClient))
                        .forEach(target -> {
                            TransportMonitoringService<?> monitoringService = context.getBean(config.getTransportType().getMonitoringServiceClass(), config, target);
                            monitoringServices.add(monitoringService);
                        });
            });
            monitoringServices.forEach(TransportMonitoringService::startMonitoring);
        };
    }

    private void checkMonitoringTarget(TransportMonitoringServiceConfig config, MonitoringTargetConfig target, TbClient tbClient) {
        DeviceConfig deviceConfig = target.getDevice();
        tbClient.logIn();

        DeviceId deviceId;
        if (deviceConfig == null || deviceConfig.getId() == null) {
            String deviceName = String.format("[%s] Monitoring device (%s)", config.getTransportType(), target.getBaseUrl());
            Device device = tbClient.getTenantDevice(deviceName)
                    .orElseGet(() -> {
                        log.info("Creating new device '{}'", deviceName);
                        Device monitoringDevice = new Device();
                        monitoringDevice.setName(deviceName);
                        monitoringDevice.setType("default");
                        DeviceData deviceData = new DeviceData();
                        deviceData.setConfiguration(new DefaultDeviceConfiguration());
                        deviceData.setTransportConfiguration(new DefaultDeviceTransportConfiguration());
                        return tbClient.saveDevice(monitoringDevice);
                    });
            deviceId = device.getId();
            target.getDevice().setId(deviceId.toString());
        } else {
            deviceId = new DeviceId(deviceConfig.getId());
        }

        log.debug("Loading credentials for device {}", deviceId);
        DeviceCredentials credentials = tbClient.getDeviceCredentialsByDeviceId(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("No credentials found for device " + deviceId));
        target.getDevice().setCredentials(credentials);
    }

    @Bean
    public ScheduledExecutorService monitoringExecutor(@Value("${monitoring.monitoring_executor_thread_pool_size}") int threadPoolSize) {
        return Executors.newScheduledThreadPool(threadPoolSize, ThingsBoardThreadFactory.forName("monitoring-executor"));
    }

}
