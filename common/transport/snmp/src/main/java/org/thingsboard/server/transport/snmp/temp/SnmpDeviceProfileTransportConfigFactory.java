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
package org.thingsboard.server.transport.snmp.temp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.thingsboard.server.common.data.device.profile.SnmpDeviceProfileTransportConfiguration;

public class SnmpDeviceProfileTransportConfigFactory {

    private static final ObjectMapper mapper = new ObjectMapper();

    private SnmpDeviceProfileTransportConfigFactory() {
    }

    public static SnmpDeviceProfileTransportConfiguration getDeviceProfileTransportConfig() {
        SnmpDeviceProfileTransportConfiguration config = null;
        try {
            String jsonConfig = "{\"type\":\"SNMP\",\"poolPeriodMs\":10000,\"timeoutMs\":5000,\"retries\":5,\"attributes\":[{\"key\":\"snmpNodeManagerEmail\",\"type\":\"STRING\",\"method\":\"get\",\"oid\":\".1.3.6.1.2.1.1.4.0\"}],\"telemetry\":[{\"key\":\"snmpNodeSysUpTime\",\"type\":\"LONG\",\"method\":\"get\",\"oid\":\".1.3.6.1.2.1.25.1.1.0\"}]}";
            config = mapper.readValue(jsonConfig, SnmpDeviceProfileTransportConfiguration.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return config;
    }
}
