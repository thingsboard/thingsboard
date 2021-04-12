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
package org.thingsboard.server.common.data.transport.snmp.configs;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.thingsboard.server.common.data.transport.snmp.SnmpCommunicationSpec;
import org.thingsboard.server.common.data.transport.snmp.SnmpMapping;
import org.thingsboard.server.common.data.transport.snmp.SnmpMethod;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "spec")
@JsonSubTypes({
        @Type(value = TelemetryQueryingSnmpCommunicationConfig.class, name = "TELEMETRY_QUERYING"),
        @Type(value = ClientAttributesQueryingSnmpCommunicationConfig.class, name = "CLIENT_ATTRIBUTES_QUERYING"),
        @Type(value = SharedAttributesSettingSnmpCommunicationConfig.class, name = "SHARED_ATTRIBUTES_SETTING"),
        @Type(value = TelemetryTrapsReceivingSnmpCommunicationConfig.class, name = "TELEMETRY_TRAPS_RECEIVING"),
        @Type(value = ClientAttributesTrapsReceivingSnmpCommunicationConfig.class, name = "CLIENT_ATTRIBUTES_TRAPS_RECEIVING")
})
public abstract class SnmpCommunicationConfig {
    protected List<SnmpMapping> mappings;

    public List<SnmpMapping> getMappings() {
        return mappings;
    }

    public abstract SnmpCommunicationSpec getSpec();

    @JsonIgnore
    public SnmpMethod getMethod() {
        return null;
    }

    @JsonIgnore
    public boolean isValid() {
        return true;
    }

    public void validate() {
        if (!isValid()) {
            throw new IllegalArgumentException("Communication config is not valid");
        }
    }
}
