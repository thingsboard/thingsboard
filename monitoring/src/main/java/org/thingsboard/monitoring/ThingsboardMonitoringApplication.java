// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.monitoring;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.monitoring.service.BaseMonitoringService;
import org.thingsboard.monitoring.service.MonitoringEntityService;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
@EnableScheduling
@Slf4j
public class ThingsboardMonitoringApplication {

    @Autowired
    private List<BaseMonitoringService<?, ?>> monitoringServices;
    @Autowired
    private MonitoringEntityService entityService;

    @Value("${monitoring.monitoring_rate_ms}")
    private int monitoringRateMs;

    public static void main(String[] args) {
        new SpringApplicationBuilder(ThingsboardMonitoringApplication.class)
                .properties(Map.of("spring.config.name", "tb-monitoring"))
                .run(args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startMonitoring() {
        entityService.checkEntities();
        monitoringServices.forEach(BaseMonitoringService::init);

        ScheduledExecutorService scheduler = ThingsBoardExecutors.newSingleThreadScheduledExecutor("monitoring-executor");
        scheduler.scheduleWithFixedDelay(() -> {
            monitoringServices.forEach(monitoringService -> {
                monitoringService.runChecks();
            });
        }, 0, monitoringRateMs, TimeUnit.MILLISECONDS);
    }

}
