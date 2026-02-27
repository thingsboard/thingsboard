/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.common.data.transport.snmp.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import org.thingsboard.server.common.data.transport.snmp.SnmpCommunicationSpec;
import org.thingsboard.server.common.data.transport.snmp.SnmpMapping;
import org.thingsboard.server.common.data.transport.snmp.SnmpMethod;
import org.thingsboard.server.common.data.transport.snmp.config.impl.ClientAttributesQueryingSnmpCommunicationConfig;
import org.thingsboard.server.common.data.transport.snmp.config.impl.SharedAttributesSettingSnmpCommunicationConfig;
import org.thingsboard.server.common.data.transport.snmp.config.impl.TelemetryQueryingSnmpCommunicationConfig;
import org.thingsboard.server.common.data.transport.snmp.config.impl.ToDeviceRpcRequestSnmpCommunicationConfig;

import java.io.Serializable;
import java.util.List;

@Schema(
        description = "SNMP communication configuration",
        discriminatorProperty = "spec",
        discriminatorMapping = {
                @DiscriminatorMapping(value = "TELEMETRY_QUERYING", schema = TelemetryQueryingSnmpCommunicationConfig.class),
                @DiscriminatorMapping(value = "CLIENT_ATTRIBUTES_QUERYING", schema = ClientAttributesQueryingSnmpCommunicationConfig.class),
                @DiscriminatorMapping(value = "SHARED_ATTRIBUTES_SETTING", schema = SharedAttributesSettingSnmpCommunicationConfig.class),
                @DiscriminatorMapping(value = "TO_DEVICE_RPC_REQUEST", schema = ToDeviceRpcRequestSnmpCommunicationConfig.class),
                @DiscriminatorMapping(value = "TO_SERVER_RPC_REQUEST", schema = ToServerRpcRequestSnmpCommunicationConfig.class)
        }
)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "spec")
@JsonSubTypes({
        @Type(value = TelemetryQueryingSnmpCommunicationConfig.class, name = "TELEMETRY_QUERYING"),
        @Type(value = ClientAttributesQueryingSnmpCommunicationConfig.class, name = "CLIENT_ATTRIBUTES_QUERYING"),
        @Type(value = SharedAttributesSettingSnmpCommunicationConfig.class, name = "SHARED_ATTRIBUTES_SETTING"),
        @Type(value = ToDeviceRpcRequestSnmpCommunicationConfig.class, name = "TO_DEVICE_RPC_REQUEST"),
        @Type(value = ToServerRpcRequestSnmpCommunicationConfig.class, name = "TO_SERVER_RPC_REQUEST")
})
public interface SnmpCommunicationConfig extends Serializable {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Specification of the SNMP communication")
    SnmpCommunicationSpec getSpec();

    @JsonIgnore
    default SnmpMethod getMethod() {
        return null;
    }

    @JsonIgnore
    List<SnmpMapping> getAllMappings();

    @JsonIgnore
    boolean isValid();

}
