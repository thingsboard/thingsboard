// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.transport.snmp.config.impl;

import org.thingsboard.server.common.data.transport.snmp.SnmpCommunicationSpec;
import org.thingsboard.server.common.data.transport.snmp.config.RepeatingQueryingSnmpCommunicationConfig;

public class ClientAttributesQueryingSnmpCommunicationConfig extends RepeatingQueryingSnmpCommunicationConfig {

    private static final long serialVersionUID = 536740834893462914L;

    @Override
    public SnmpCommunicationSpec getSpec() {
        return SnmpCommunicationSpec.CLIENT_ATTRIBUTES_QUERYING;
    }

}
