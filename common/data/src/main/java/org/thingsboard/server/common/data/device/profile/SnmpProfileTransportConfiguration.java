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

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
public class SnmpProfileTransportConfiguration implements DeviceProfileTransportConfiguration {
    private int pollPeriodMs;
    private int timeoutMs;
    private int retries;
    private List<SnmpDeviceProfileKvMapping> attributes;
    private List<SnmpDeviceProfileKvMapping> telemetry;

    @Override
    public DeviceTransportType getType() {
        return DeviceTransportType.SNMP;
    }

    @JsonIgnore
    public List<SnmpDeviceProfileKvMapping> getKvMappings() {
        return Stream.concat(attributes.stream(), telemetry.stream()).collect(Collectors.toList());
    }
}
