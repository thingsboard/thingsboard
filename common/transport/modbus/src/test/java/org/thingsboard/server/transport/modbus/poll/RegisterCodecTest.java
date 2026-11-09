// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.transport.modbus.poll;

import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.transport.modbus.ModbusByteOrder;
import org.thingsboard.server.common.data.transport.modbus.ModbusDataType;
import org.thingsboard.server.common.data.transport.modbus.ModbusFunctionCode;
import org.thingsboard.server.common.data.transport.modbus.ModbusMapping;

import java.nio.ByteBuffer;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.thingsboard.server.common.data.transport.modbus.ModbusByteOrder.BIG;
import static org.thingsboard.server.common.data.transport.modbus.ModbusByteOrder.LITTLE;
import static org.thingsboard.server.common.data.transport.modbus.ModbusFunctionCode.READ_COILS;
import static org.thingsboard.server.common.data.transport.modbus.ModbusFunctionCode.READ_HOLDING_REGISTERS;

class RegisterCodecTest {

    @Test
    void decodesSignedAndUnsigned16() {
        byte[] data = {(byte) 0xFF, (byte) 0xFE};
        assertThat(decode(data, mapping(ModbusDataType.INT16, 0), BIG, BIG)).isEqualTo(-2L);
        assertThat(decode(data, mapping(ModbusDataType.UINT16, 0), BIG, BIG)).isEqualTo(65534L);
    }

    @Test
    void decodesFloat32InAllByteAndWordOrders() {
        byte[] abcd = ByteBuffer.allocate(4).putFloat(231.4f).array();          // A B C D
        byte[] cdab = {abcd[2], abcd[3], abcd[0], abcd[1]};                      // word swap
        byte[] badc = {abcd[1], abcd[0], abcd[3], abcd[2]};                      // byte swap
        byte[] dcba = {abcd[3], abcd[2], abcd[1], abcd[0]};                      // both

        ModbusMapping m = mapping(ModbusDataType.FLOAT32, 0);
        assertThat(decode(abcd, m, BIG, BIG)).isEqualTo(231.4);
        assertThat(decode(cdab, m, BIG, LITTLE)).isEqualTo(231.4);
        assertThat(decode(badc, m, LITTLE, BIG)).isEqualTo(231.4);
        assertThat(decode(dcba, m, LITTLE, LITTLE)).isEqualTo(231.4);
    }

    @Test
    void decodes32And64BitIntegers() {
        assertThat(decode(ByteBuffer.allocate(4).putInt(-5).array(), mapping(ModbusDataType.INT32, 0), BIG, BIG)).isEqualTo(-5L);
        assertThat(decode(ByteBuffer.allocate(4).putInt(-1).array(), mapping(ModbusDataType.UINT32, 0), BIG, BIG)).isEqualTo(4294967295L);
        assertThat(decode(ByteBuffer.allocate(8).putLong(123456789012L).array(), mapping(ModbusDataType.INT64, 0), BIG, BIG)).isEqualTo(123456789012L);
        assertThat(decode(ByteBuffer.allocate(8).putDouble(1.5).array(), mapping(ModbusDataType.FLOAT64, 0), BIG, BIG)).isEqualTo(1.5);
    }

    @Test
    void decodesUint64AsLongOrStringWhenTooLarge() {
        assertThat(decode(ByteBuffer.allocate(8).putLong(42).array(), mapping(ModbusDataType.UINT64, 0), BIG, BIG)).isEqualTo(42L);
        assertThat(decode(ByteBuffer.allocate(8).putLong(-1).array(), mapping(ModbusDataType.UINT64, 0), BIG, BIG))
                .isEqualTo("18446744073709551615");
    }

    @Test
    void decodesFloat16() {
        byte[] data = ByteBuffer.allocate(2).putShort(Float.floatToFloat16(1.5f)).array();
        assertThat(decode(data, mapping(ModbusDataType.FLOAT16, 0), BIG, BIG)).isEqualTo(1.5);
    }

    @Test
    void appliesMultiplier() {
        ModbusMapping m = mapping(ModbusDataType.UINT16, 0);
        m.setMultiplier(0.1);
        assertThat(decode(new byte[]{0x00, (byte) 0xEB}, m, BIG, BIG)).isEqualTo(23.5);
    }

    @Test
    void decodesValueAtOffsetInsideBlock() {
        byte[] data = {0x00, 0x01, 0x00, 0x02, 0x00, 0x03};
        ModbusMapping third = mapping(ModbusDataType.UINT16, 12);
        ReadBlock block = new ReadBlock(READ_HOLDING_REGISTERS, 10, 3, List.of());
        assertThat(RegisterCodec.decode(data, block, third, BIG, BIG)).isEqualTo(3L);
    }

    @Test
    void decodesBitOfRegister() {
        ModbusMapping running = mapping(ModbusDataType.BITS, 0);
        running.setObjectsCount(1);
        running.setBit(1);
        ModbusMapping fault = mapping(ModbusDataType.BITS, 0);
        fault.setBit(3);
        byte[] statusWord = {0x00, 0b0000_1010};
        assertThat(decode(statusWord, running, BIG, BIG)).isEqualTo(true);
        assertThat(decode(statusWord, fault, BIG, BIG)).isEqualTo(true);
        fault.setBit(2);
        assertThat(decode(statusWord, fault, BIG, BIG)).isEqualTo(false);
    }

    @Test
    void decodesCoilsLsbFirst() {
        byte[] coils = {0b0000_0101, 0b0000_0001}; // coils 0, 2 and 8 are on
        ReadBlock block = new ReadBlock(READ_COILS, 100, 9, List.of());
        assertThat(RegisterCodec.decode(coils, block, coil(100), BIG, BIG)).isEqualTo(true);
        assertThat(RegisterCodec.decode(coils, block, coil(101), BIG, BIG)).isEqualTo(false);
        assertThat(RegisterCodec.decode(coils, block, coil(102), BIG, BIG)).isEqualTo(true);
        assertThat(RegisterCodec.decode(coils, block, coil(108), BIG, BIG)).isEqualTo(true);
    }

    @Test
    void decodesStringTrimmingPaddingAndHonouringByteOrder() {
        ModbusMapping serial = mapping(ModbusDataType.STRING, 0);
        serial.setObjectsCount(3);
        assertThat(decode(new byte[]{'S', 'N', '1', '2', 0, 0}, serial, BIG, BIG)).isEqualTo("SN12");
        assertThat(decode(new byte[]{'N', 'S', '2', '1', ' ', 0}, serial, LITTLE, BIG)).isEqualTo("SN12");
    }

    @Test
    void decodesBytesAsHex() {
        ModbusMapping raw = mapping(ModbusDataType.BYTES, 0);
        raw.setObjectsCount(2);
        assertThat(decode(new byte[]{0x01, (byte) 0xAB, 0x00, 0x10}, raw, BIG, BIG)).isEqualTo("01ab0010");
    }

    @Test
    void rejectsTooShortResponse() {
        assertThatThrownBy(() -> decode(new byte[]{0x00, 0x01}, mapping(ModbusDataType.FLOAT32, 0), BIG, BIG))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static Object decode(byte[] data, ModbusMapping mapping, ModbusByteOrder byteOrder, ModbusByteOrder wordOrder) {
        ReadBlock block = new ReadBlock(mapping.getFunctionCode(), mapping.getAddress(), mapping.getEffectiveObjectsCount(), List.of());
        return RegisterCodec.decode(data, block, mapping, byteOrder, wordOrder);
    }

    private static ModbusMapping mapping(ModbusDataType type, int address) {
        ModbusMapping mapping = new ModbusMapping();
        mapping.setTag("v");
        mapping.setType(type);
        mapping.setFunctionCode(READ_HOLDING_REGISTERS);
        mapping.setAddress(address);
        return mapping;
    }

    private static ModbusMapping coil(int address) {
        ModbusMapping mapping = mapping(ModbusDataType.BITS, address);
        mapping.setFunctionCode(ModbusFunctionCode.READ_COILS);
        return mapping;
    }

}
