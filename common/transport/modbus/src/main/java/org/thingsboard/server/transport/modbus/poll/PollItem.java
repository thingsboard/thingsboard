// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.transport.modbus.poll;

import org.thingsboard.server.common.data.transport.modbus.ModbusMapping;

/**
 * A polled mapping and where its value goes. The same register may be polled for telemetry and attributes.
 */
public record PollItem(ModbusMapping mapping, Target target) {

    public enum Target {TELEMETRY, ATTRIBUTE}

    public int address() {
        return mapping.getAddress();
    }

    public int count() {
        return mapping.getEffectiveObjectsCount();
    }

}
