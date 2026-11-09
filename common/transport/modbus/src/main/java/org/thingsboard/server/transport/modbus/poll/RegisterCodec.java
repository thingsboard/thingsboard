// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.transport.modbus.poll;

import org.thingsboard.server.common.data.transport.modbus.ModbusByteOrder;
import org.thingsboard.server.common.data.transport.modbus.ModbusDataType;
import org.thingsboard.server.common.data.transport.modbus.ModbusMapping;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/**
 * Decodes values of a {@link ModbusMapping} out of the raw bytes of the {@link ReadBlock} that contains it.
 * <p>
 * Registers arrive as on the wire (2 bytes each, big-endian). {@code byteOrder} swaps the bytes inside each register,
 * {@code wordOrder} reverses the registers of a multi-register value. Coils and discrete inputs arrive LSB first.
 * Results are {@link Boolean}, {@link Long}, {@link Double} or {@link String}.
 */
public final class RegisterCodec {

    private RegisterCodec() {
    }

    public static Object decode(byte[] data, ReadBlock block, ModbusMapping mapping, ModbusByteOrder byteOrder, ModbusByteOrder wordOrder) {
        int offset = mapping.getAddress() - block.address();
        if (mapping.getFunctionCode().isBitAccess()) {
            return bitAt(data, offset);
        }
        int registers = mapping.getEffectiveObjectsCount();
        int byteOffset = offset * 2;
        if (byteOffset < 0 || byteOffset + registers * 2 > data.length) {
            throw new IllegalArgumentException("Response too short for " + mapping.getTag() + ": got " + data.length + " bytes");
        }
        ModbusDataType type = mapping.getType();
        return switch (type) {
            case BITS -> ((registerValue(data, byteOffset) >> mapping.getBit()) & 1) == 1;
            case STRING -> decodeString(ordered(data, byteOffset, registers, byteOrder, ModbusByteOrder.BIG));
            case BYTES -> HexFormat.of().formatHex(ordered(data, byteOffset, registers, byteOrder, ModbusByteOrder.BIG));
            default -> scale(decodeNumber(type, ordered(data, byteOffset, registers, byteOrder, wordOrder)), mapping.getMultiplier());
        };
    }

    private static Object decodeNumber(ModbusDataType type, byte[] bytes) {
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        return switch (type) {
            case INT16 -> (long) buf.getShort();
            case UINT16 -> (long) (buf.getShort() & 0xFFFF);
            case FLOAT16 -> shortestDouble(Float.float16ToFloat(buf.getShort()));
            case INT32 -> (long) buf.getInt();
            case UINT32 -> buf.getInt() & 0xFFFFFFFFL;
            case FLOAT32 -> shortestDouble(buf.getFloat());
            case INT64 -> buf.getLong();
            case UINT64 -> {
                BigInteger value = new BigInteger(1, bytes);
                yield value.bitLength() < Long.SIZE ? (Object) value.longValue() : value.toString();
            }
            case FLOAT64 -> buf.getDouble();
            default -> throw new IllegalArgumentException("Not a numeric type: " + type);
        };
    }

    private static Object scale(Object raw, Double multiplier) {
        if (multiplier == null) {
            return raw;
        }
        if (raw instanceof String big) {
            return new BigDecimal(big).multiply(BigDecimal.valueOf(multiplier)).doubleValue();
        }
        return BigDecimal.valueOf(((Number) raw).doubleValue()).multiply(BigDecimal.valueOf(multiplier)).doubleValue();
    }

    /** Float widened through its decimal form, so 231.4f becomes 231.4 instead of 231.39999389648438. */
    private static double shortestDouble(float value) {
        return Float.isFinite(value) ? Double.parseDouble(Float.toString(value)) : value;
    }

    private static Boolean bitAt(byte[] data, int index) {
        if (index < 0 || index / 8 >= data.length) {
            throw new IllegalArgumentException("Response too short for bit " + index + ": got " + data.length + " bytes");
        }
        return ((data[index / 8] >> (index % 8)) & 1) == 1;
    }

    private static int registerValue(byte[] data, int byteOffset) {
        return ((data[byteOffset] & 0xFF) << 8) | (data[byteOffset + 1] & 0xFF);
    }

    static byte[] ordered(byte[] data, int byteOffset, int registers, ModbusByteOrder byteOrder, ModbusByteOrder wordOrder) {
        byte[] out = new byte[registers * 2];
        for (int i = 0; i < registers; i++) {
            int source = byteOffset + 2 * (wordOrder == ModbusByteOrder.LITTLE ? registers - 1 - i : i);
            byte high = data[source];
            byte low = data[source + 1];
            if (byteOrder == ModbusByteOrder.LITTLE) {
                out[2 * i] = low;
                out[2 * i + 1] = high;
            } else {
                out[2 * i] = high;
                out[2 * i + 1] = low;
            }
        }
        return out;
    }

    private static String decodeString(byte[] bytes) {
        int end = bytes.length;
        while (end > 0 && (bytes[end - 1] == 0 || bytes[end - 1] == ' ')) {
            end--;
        }
        return new String(bytes, 0, end, StandardCharsets.US_ASCII);
    }

}
