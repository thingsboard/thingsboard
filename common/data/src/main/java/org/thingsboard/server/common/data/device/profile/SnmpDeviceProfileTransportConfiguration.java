// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.device.profile;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.transport.snmp.config.SnmpCommunicationConfig;

import java.util.List;

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
                && communicationConfigs != null
                && communicationConfigs.stream().allMatch(config -> config != null && config.isValid());
    }

}
