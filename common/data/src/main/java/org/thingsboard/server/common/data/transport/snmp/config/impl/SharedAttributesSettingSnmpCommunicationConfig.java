// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.transport.snmp.config.impl;

import org.thingsboard.server.common.data.transport.snmp.SnmpCommunicationSpec;
import org.thingsboard.server.common.data.transport.snmp.SnmpMethod;
import org.thingsboard.server.common.data.transport.snmp.config.MultipleMappingsSnmpCommunicationConfig;

public class SharedAttributesSettingSnmpCommunicationConfig extends MultipleMappingsSnmpCommunicationConfig {

    private static final long serialVersionUID = 8981224974190924703L;

    @Override
    public SnmpCommunicationSpec getSpec() {
        return SnmpCommunicationSpec.SHARED_ATTRIBUTES_SETTING;
    }

    @Override
    public SnmpMethod getMethod() {
        return SnmpMethod.SET;
    }

}
