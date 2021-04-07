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
import lombok.ToString;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.transport.snmp.AuthenticationProtocol;
import org.thingsboard.server.common.data.transport.snmp.PrivacyProtocol;
import org.thingsboard.server.common.data.transport.snmp.SnmpProtocolVersion;

import java.util.Objects;

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
     * For SNMP v3 with User Based Security Model
     * */
    private String username;
    private String securityName;
    private String contextName;
    private AuthenticationProtocol authenticationProtocol;
    private String authenticationPassphrase;
    private PrivacyProtocol privacyProtocol;
    private String privacyPassphrase;
    private String engineId;

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
                            && privacyProtocol != null && privacyPassphrase != null && engineId != null;
                    break;
            }
        }
        return isValid;
    }
}
