/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.transport.snmp;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.device.profile.SnmpDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.transport.TransportContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service("SnmpTransportContext")
@ConditionalOnExpression("'${service.type:null}'=='tb-transport' || ('${service.type:null}'=='monolith' && '${transport.api_enabled:true}'=='true' && '${transport.snmp.enabled}'=='true')")
@Slf4j
public class SnmpTransportContext extends TransportContext {
    @Getter
    private final Map<DeviceProfileId, SnmpDeviceProfileTransportConfiguration> deviceProfileTransportConfig = new ConcurrentHashMap<>();

}
