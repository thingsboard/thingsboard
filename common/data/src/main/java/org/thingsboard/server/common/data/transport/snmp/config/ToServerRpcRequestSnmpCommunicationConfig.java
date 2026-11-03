// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.transport.snmp.config;

import org.thingsboard.server.common.data.transport.snmp.SnmpCommunicationSpec;

public class ToServerRpcRequestSnmpCommunicationConfig extends MultipleMappingsSnmpCommunicationConfig {

    private static final long serialVersionUID = 4851028734093214L;

    @Override
    public SnmpCommunicationSpec getSpec() {
        return SnmpCommunicationSpec.TO_SERVER_RPC_REQUEST;
    }

}
