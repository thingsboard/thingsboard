// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.mqtt;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jmx.export.MBeanExporter;
import org.thingsboard.server.common.transport.service.DefaultTransportService;
import org.thingsboard.server.queue.util.TbTransportComponent;

import java.util.HashMap;
import java.util.Map;

@Configuration
@TbTransportComponent
@RequiredArgsConstructor
public class DefaultTransportMBeanConfiguration {

    private final DefaultTransportService transportService;

    @Bean
    public HashMapObserver hashMapObserver() {
        return new HashMapObserver(transportService.sessions);
    }

    @Bean
    public MBeanExporter mBeanExporter() {
        MBeanExporter exporter = new MBeanExporter();
        Map<String, Object> beans = new HashMap<>();
        beans.put("org.thingsboard:type=TransportSessionMapObserver", hashMapObserver());
        exporter.setBeans(beans);
        return exporter;
    }

}
