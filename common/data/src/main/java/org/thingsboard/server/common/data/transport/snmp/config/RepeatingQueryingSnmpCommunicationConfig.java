// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.transport.snmp.config;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.transport.snmp.SnmpMethod;

@EqualsAndHashCode(callSuper = true)
@Data
public abstract class RepeatingQueryingSnmpCommunicationConfig extends MultipleMappingsSnmpCommunicationConfig {
    private Long queryingFrequencyMs;

    @Override
    public SnmpMethod getMethod() {
        return SnmpMethod.GET;
    }

    @Override
    public boolean isValid() {
        return queryingFrequencyMs != null && queryingFrequencyMs > 0 && super.isValid();
    }
}
