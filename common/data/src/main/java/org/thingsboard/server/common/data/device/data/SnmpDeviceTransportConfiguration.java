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
package org.thingsboard.server.common.data.device.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.thingsboard.server.common.data.DeviceTransportType;

@Data
public class SnmpDeviceTransportConfiguration implements DeviceTransportConfiguration {

    private String address;
    private int port;
    private String community;
    private String protocolVersion;

    @Override
    public DeviceTransportType getType() {
        return DeviceTransportType.SNMP;
    }

    @JsonIgnore
    public boolean isValid() {
        return StringUtils.isNotEmpty(this.address)
                && this.port > 0
                && StringUtils.isNotEmpty(this.community)
                && StringUtils.isNotEmpty(this.protocolVersion);
    }
}
