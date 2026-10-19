// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.transport.snmp.config.impl;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.transport.snmp.SnmpCommunicationSpec;
import org.thingsboard.server.common.data.transport.snmp.config.RepeatingQueryingSnmpCommunicationConfig;

@EqualsAndHashCode(callSuper = true)
@Data
public class TelemetryQueryingSnmpCommunicationConfig extends RepeatingQueryingSnmpCommunicationConfig {

    private static final long serialVersionUID = -1367743866881596885L;

    @Override
    public SnmpCommunicationSpec getSpec() {
        return SnmpCommunicationSpec.TELEMETRY_QUERYING;
    }

}
