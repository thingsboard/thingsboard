// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.transport.modbus.poll;

import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.transport.modbus.ModbusDataType;
import org.thingsboard.server.common.data.transport.modbus.ModbusFunctionCode;
import org.thingsboard.server.common.data.transport.modbus.ModbusMapping;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.common.data.transport.modbus.ModbusFunctionCode.READ_COILS;
import static org.thingsboard.server.common.data.transport.modbus.ModbusFunctionCode.READ_HOLDING_REGISTERS;
import static org.thingsboard.server.common.data.transport.modbus.ModbusFunctionCode.READ_INPUT_REGISTERS;

class ReadPlannerTest {

    @Test
    void mergesNeighboursWithinGap() {
        var voltage = item("voltage", ModbusDataType.FLOAT32, READ_INPUT_REGISTERS, 0, null);
        var current = item("current", ModbusDataType.FLOAT32, READ_INPUT_REGISTERS, 6, null);

        List<ReadBlock> blocks = ReadPlanner.plan(List.of(current, voltage), 8);

        assertThat(blocks).containsExactly(new ReadBlock(READ_INPUT_REGISTERS, 0, 8, List.of(voltage, current)));
    }

    @Test
    void splitsWhenGapIsTooLarge() {
        var a = item("a", ModbusDataType.UINT16, READ_HOLDING_REGISTERS, 0, null);
        var b = item("b", ModbusDataType.UINT16, READ_HOLDING_REGISTERS, 10, null);

        List<ReadBlock> blocks = ReadPlanner.plan(List.of(a, b), 8);

        assertThat(blocks).containsExactly(
                new ReadBlock(READ_HOLDING_REGISTERS, 0, 1, List.of(a)),
                new ReadBlock(READ_HOLDING_REGISTERS, 10, 1, List.of(b)));
    }

    @Test
    void keepsFunctionCodesSeparate() {
        var holding = item("h", ModbusDataType.UINT16, READ_HOLDING_REGISTERS, 0, null);
        var input = item("i", ModbusDataType.UINT16, READ_INPUT_REGISTERS, 1, null);
        var coil = item("c", ModbusDataType.BITS, READ_COILS, 0, null);

        List<ReadBlock> blocks = ReadPlanner.plan(List.of(holding, input, coil), 8);

        assertThat(blocks).extracting(ReadBlock::functionCode)
                .containsExactlyInAnyOrder(READ_HOLDING_REGISTERS, READ_INPUT_REGISTERS, READ_COILS);
    }

    @Test
    void respectsFunctionCodeLimit() {
        var first = item("first", ModbusDataType.STRING, READ_HOLDING_REGISTERS, 0, 100);
        var second = item("second", ModbusDataType.STRING, READ_HOLDING_REGISTERS, 100, 30);

        List<ReadBlock> blocks = ReadPlanner.plan(List.of(first, second), 8);

        assertThat(blocks).hasSize(2); // 130 registers would exceed the 125 limit of function code 3
    }

    @Test
    void mergesOverlappingAndDuplicateReads() {
        var telemetry = item("ctRatio", ModbusDataType.UINT16, READ_HOLDING_REGISTERS, 20, null);
        var attribute = new PollItem(telemetry.mapping(), PollItem.Target.ATTRIBUTE);
        var wide = item("wide", ModbusDataType.INT32, READ_HOLDING_REGISTERS, 19, null);

        List<ReadBlock> blocks = ReadPlanner.plan(List.of(telemetry, attribute, wide), 0);

        assertThat(blocks).hasSize(1);
        assertThat(blocks.get(0).address()).isEqualTo(19);
        assertThat(blocks.get(0).count()).isEqualTo(2);
        assertThat(blocks.get(0).items()).hasSize(3);
    }

    @Test
    void splitPerItemCreatesOneBlockPerMapping() {
        var a = item("a", ModbusDataType.UINT16, READ_HOLDING_REGISTERS, 0, null);
        var b = item("b", ModbusDataType.INT32, READ_HOLDING_REGISTERS, 4, null);
        ReadBlock merged = ReadPlanner.plan(List.of(a, b), 8).get(0);

        assertThat(merged.splitPerItem()).containsExactly(
                new ReadBlock(READ_HOLDING_REGISTERS, 0, 1, List.of(a)),
                new ReadBlock(READ_HOLDING_REGISTERS, 4, 2, List.of(b)));
    }

    static PollItem item(String tag, ModbusDataType type, ModbusFunctionCode fc, int address, Integer count) {
        ModbusMapping mapping = new ModbusMapping();
        mapping.setTag(tag);
        mapping.setType(type);
        mapping.setFunctionCode(fc);
        mapping.setAddress(address);
        mapping.setObjectsCount(count);
        return new PollItem(mapping, PollItem.Target.TELEMETRY);
    }

}
