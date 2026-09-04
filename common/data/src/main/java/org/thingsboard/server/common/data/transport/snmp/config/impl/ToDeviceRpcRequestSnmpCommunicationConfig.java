// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.transport.snmp.config.impl;

import org.thingsboard.server.common.data.transport.snmp.SnmpCommunicationSpec;
import org.thingsboard.server.common.data.transport.snmp.config.MultipleMappingsSnmpCommunicationConfig;

public class ToDeviceRpcRequestSnmpCommunicationConfig extends MultipleMappingsSnmpCommunicationConfig {

    private static final long serialVersionUID = -8607312598073845460L;

    @Override
    public SnmpCommunicationSpec getSpec() {
        return SnmpCommunicationSpec.TO_DEVICE_RPC_REQUEST;
    }
}
