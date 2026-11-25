// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.transport.modbus.poll;

import org.thingsboard.server.common.data.transport.modbus.ModbusFunctionCode;

import java.util.List;

/**
 * One read request covering {@code count} consecutive objects starting at {@code address}, and the items decoded from it.
 */
public record ReadBlock(ModbusFunctionCode functionCode, int address, int count, List<PollItem> items) {

    /** One block per item, for devices that reject reads spanning unmapped addresses (exception 02). */
    public List<ReadBlock> splitPerItem() {
        return items.stream()
                .map(item -> new ReadBlock(functionCode, item.address(), item.count(), List.of(item)))
                .toList();
    }

}
