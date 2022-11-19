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

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.monitoring.config.service.TransportMonitoringServiceConfig;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@SpringBootApplication
@EnableScheduling
public class ThingsboardMonitoringApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(ThingsboardMonitoringApplication.class)
                .properties(Map.of("spring.config.name", "tb-monitoring"))
                .run(args);
    }

    @Bean
    public ApplicationRunner initMonitoringServices(List<TransportMonitoringServiceConfig> configs, ApplicationContext context) {
        return args -> {
            configs.forEach(config -> {
                config.getTargets().stream()
                        .filter(target -> StringUtils.isNotBlank(target.getBaseUrl()))
                        .forEach(target -> {
                            context.getBean(config.getTransportType().getMonitoringServiceClass(), config, target);
                        });
            });
        };
    }

    @Bean
    public ScheduledExecutorService monitoringExecutor(List<TransportMonitoringServiceConfig> configs) {
        int targetsCount = configs.stream().mapToInt(config -> config.getTargets().size()).sum();
        return Executors.newScheduledThreadPool(targetsCount, ThingsBoardThreadFactory.forName("monitoring-executor"));
    }

    @Bean
    public ExecutorService requestExecutor() {
        return Executors.newCachedThreadPool(ThingsBoardThreadFactory.forName("request-executor"));
    }

}
