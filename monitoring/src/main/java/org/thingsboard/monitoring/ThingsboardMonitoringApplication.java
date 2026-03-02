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
package org.thingsboard.monitoring;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.monitoring.data.notification.InfoNotification;
import org.thingsboard.monitoring.notification.NotificationService;
import org.thingsboard.monitoring.service.BaseMonitoringService;
import org.thingsboard.monitoring.service.MonitoringEntityService;
import jakarta.annotation.PreDestroy;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
@EnableScheduling
@Slf4j
@RequiredArgsConstructor
public class ThingsboardMonitoringApplication {

    private final List<BaseMonitoringService<?, ?>> monitoringServices;
    private final MonitoringEntityService entityService;
    private final NotificationService notificationService;

    @Value("${monitoring.monitoring_rate_ms}")
    private int monitoringRateMs;

    ScheduledExecutorService scheduler = ThingsBoardExecutors.newSingleThreadScheduledExecutor("monitoring");

    public static void main(String[] args) {
        new SpringApplicationBuilder(ThingsboardMonitoringApplication.class)
                .properties(Map.of("spring.config.name", "tb-monitoring"))
                .run(args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startMonitoring() {
        entityService.checkEntities();
        monitoringServices.forEach(BaseMonitoringService::init);

        for (int i = 0; i < monitoringServices.size(); i++) {
            int initialDelay = (monitoringRateMs / monitoringServices.size()) * i;
            BaseMonitoringService<?, ?> service = monitoringServices.get(i);
            log.info("Scheduling initialDelay {}, fixedDelay {} for monitoring '{}' ", initialDelay, monitoringRateMs, service.getClass().getSimpleName());
            scheduler.scheduleWithFixedDelay(service::runChecks, initialDelay, monitoringRateMs, TimeUnit.MILLISECONDS);
        }

        String publicDashboardUrl = entityService.getDashboardPublicLink();
        notificationService.sendNotification(new InfoNotification(":rocket: <"+publicDashboardUrl+"|Monitoring> started"));
    }

    @EventListener(ContextClosedEvent.class)
    public void onShutdown(ContextClosedEvent event) {
        log.info("Shutting down monitoring service");
        try {
            var futures = notificationService.sendNotification(new InfoNotification(":warning: Monitoring is shutting down"));
            for (Future<?> future : futures) {
                future.get(5, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            log.warn("Failed to send shutdown notification", e);
        }
    }

    @PreDestroy
    public void shutdownScheduler() {
        scheduler.shutdown();
    }

}
