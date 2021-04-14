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
package org.thingsboard.server.common.data.transport.snmp.config.impl;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.kv.DataType;
import org.thingsboard.server.common.data.transport.snmp.SnmpCommunicationSpec;
import org.thingsboard.server.common.data.transport.snmp.SnmpMapping;
import org.thingsboard.server.common.data.transport.snmp.SnmpMethod;
import org.thingsboard.server.common.data.transport.snmp.config.RepeatingQueryingSnmpCommunicationConfig;

import java.util.Collections;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class ToDeviceRpcResponseQueryingSnmpCommunicationConfig extends RepeatingQueryingSnmpCommunicationConfig {
    private SnmpMapping mapping;

    @Override
    public SnmpCommunicationSpec getSpec() {
        return SnmpCommunicationSpec.TO_DEVICE_RPC_RESPONSE_QUERYING;
    }

    @Override
    public SnmpMethod getMethod() {
        return SnmpMethod.GET;
    }

    public void setMapping(SnmpMapping mapping) {
        this.mapping = mapping != null ? new SnmpMapping(mapping.getOid(), RPC_RESPONSE_KEY_NAME, DataType.STRING) : null;
    }

    @Override
    public List<SnmpMapping> getAllMappings() {
        return Collections.singletonList(mapping);
    }

    @Override
    public boolean isValid() {
        return true;
    }

    public static final String RPC_RESPONSE_KEY_NAME = "rpcResponse";

}
