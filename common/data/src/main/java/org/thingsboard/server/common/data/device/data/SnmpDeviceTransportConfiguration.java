// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.device.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.ToString;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.transport.snmp.AuthenticationProtocol;
import org.thingsboard.server.common.data.transport.snmp.PrivacyProtocol;
import org.thingsboard.server.common.data.transport.snmp.SnmpProtocolVersion;

@Data
@ToString(of = {"host", "port", "protocolVersion"})
public class SnmpDeviceTransportConfiguration implements DeviceTransportConfiguration {
    private String host;
    private Integer port;
    private SnmpProtocolVersion protocolVersion;

    /*
     * For SNMP v1 and v2c
     * */
    private String community;

    /*
     * For SNMP v3
     * */
    private String username;
    private String securityName;
    private String contextName;
    private AuthenticationProtocol authenticationProtocol;
    private String authenticationPassphrase;
    private PrivacyProtocol privacyProtocol;
    private String privacyPassphrase;
    private String engineId;

    public SnmpDeviceTransportConfiguration() {
        this.host = "localhost";
        this.port = 161;
        this.protocolVersion = SnmpProtocolVersion.V2C;
        this.community = "public";
    }

    @Override
    public DeviceTransportType getType() {
        return DeviceTransportType.SNMP;
    }

    @Override
    public void validate() {
        if (!isValid()) {
            throw new IllegalArgumentException("Transport configuration is not valid");
        }
    }

    @JsonIgnore
    private boolean isValid() {
        boolean isValid = StringUtils.isNotBlank(host) && port != null && protocolVersion != null;
        if (isValid) {
            switch (protocolVersion) {
                case V1:
                case V2C:
                    isValid = StringUtils.isNotEmpty(community);
                    break;
                case V3:
                    isValid = StringUtils.isNotBlank(username) && StringUtils.isNotBlank(securityName)
                            && contextName != null && authenticationProtocol != null
                            && StringUtils.isNotBlank(authenticationPassphrase)
                            && privacyProtocol != null && StringUtils.isNotBlank(privacyPassphrase) && engineId != null;
                    break;
            }
        }
        return isValid;
    }
}
