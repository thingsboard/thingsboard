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
package org.thingsboard.monitoring.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.thingsboard.monitoring.transport.TransportHealthChecker;
import org.thingsboard.monitoring.transport.impl.CoapTransportHealthChecker;
import org.thingsboard.monitoring.transport.impl.HttpTransportHealthChecker;
import org.thingsboard.monitoring.transport.impl.Lwm2mTransportHealthChecker;
import org.thingsboard.monitoring.transport.impl.MqttTransportHealthChecker;

@AllArgsConstructor
@Getter
public enum TransportType {

    MQTT(MqttTransportHealthChecker.class),
    COAP(CoapTransportHealthChecker.class),
    HTTP(HttpTransportHealthChecker.class),
    LWM2M(Lwm2mTransportHealthChecker.class);

    private final Class<? extends TransportHealthChecker<?>> serviceClass;

}
