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
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.transport.snmp.SnmpMapping;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
public class SnmpDeviceProfileTransportConfiguration implements DeviceProfileTransportConfiguration {
    private int pollPeriodMs;
    private int timeoutMs;
    private int retries;
    private List<SnmpMapping> telemetryMappings;
    private List<SnmpMapping> attributesMappings;

    @Override
    public DeviceTransportType getType() {
        return DeviceTransportType.SNMP;
    }

    @JsonIgnore
    public List<SnmpMapping> getAllMappings() {
        if (telemetryMappings != null && attributesMappings != null) {
            return Stream.concat(telemetryMappings.stream(), attributesMappings.stream()).collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public void validate() {
        if (!isValid()) {
            throw new IllegalArgumentException("Transport configuration is not valid");
        }
    }

    @JsonIgnore
    private boolean isValid() {
        List<SnmpMapping> mappings = getAllMappings();
        return pollPeriodMs > 0 && timeoutMs > 0 && retries >= 0 &&
                !mappings.isEmpty() && mappings.stream().allMatch(SnmpMapping::isValid) &&
                mappings.stream().map(SnmpMapping::getOid).distinct().count() == mappings.size();
    }
}
