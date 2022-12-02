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
package org.thingsboard.monitoring.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.thingsboard.monitoring.service.TransportMonitoringService;
import org.thingsboard.monitoring.service.impl.CoapTransportMonitoringService;
import org.thingsboard.monitoring.service.impl.HttpTransportMonitoringService;
import org.thingsboard.monitoring.service.impl.MqttTransportMonitoringService;

@AllArgsConstructor
@Getter
public enum TransportType {
    MQTT(MqttTransportMonitoringService.class),
    COAP(CoapTransportMonitoringService.class),
    HTTP(HttpTransportMonitoringService.class);

    private final Class<? extends TransportMonitoringService<?>> monitoringServiceClass;

}
