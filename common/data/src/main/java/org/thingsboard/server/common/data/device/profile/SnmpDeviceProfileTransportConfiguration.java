/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.common.data.device.profile;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.apache.commons.lang3.ArrayUtils;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.transport.snmp.SnmpMapping;
import org.thingsboard.server.common.data.transport.snmp.configs.SnmpCommunicationConfig;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
public class SnmpDeviceProfileTransportConfiguration implements DeviceProfileTransportConfiguration {
    private Integer timeoutMs;
    private Integer retries;
    private List<SnmpCommunicationConfig> communicationConfigs;

    @Override
    public DeviceTransportType getType() {
        return DeviceTransportType.SNMP;
    }

    @Override
    public void validate() {
        if (!isValid()) {
            throw new IllegalArgumentException("SNMP transport configuration is not valid");
        }
    }

    @JsonIgnore
    private boolean isValid() {
        return timeoutMs != null && timeoutMs >= 0 && retries != null && retries >= 0
                && communicationConfigs != null && !communicationConfigs.isEmpty()
                && communicationConfigs.stream().allMatch(config -> config != null && config.isValid())
                && communicationConfigs.stream().flatMap(config -> config.getMappings().stream()).map(SnmpMapping::getOid)
                .distinct().count() == communicationConfigs.stream().mapToInt(config -> config.getMappings().size()).sum();
    }
}
