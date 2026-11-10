// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.transport.modbus.poll;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import org.thingsboard.server.common.data.transport.modbus.ModbusByteOrder;
import org.thingsboard.server.common.data.transport.modbus.ModbusDataType;
import org.thingsboard.server.common.data.transport.modbus.ModbusMapping;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/**
 * Decodes values of a {@link ModbusMapping} out of the raw bytes of the {@link ReadBlock} that contains it,
 * and encodes values to write back.
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

    /**
     * Encodes a value for function code 6 or 16: register bytes as they go on the wire (2 per register).
     * {@code multiplier} is reversed (raw = value / multiplier). Integer types are rounded half-up and range-checked and
     * float types reject values they cannot hold, so a value never wraps around or turns into infinity silently.
     * <p>
     * Examples (value &rarr; bytes on the wire, in hex):
     * <pre>
     * UINT16, multiplier 0.1:          23.5    &rarr; raw 235 &rarr; 00 EB
     * UINT16:                          70000   &rarr; rejected, out of range
     * FLOAT32, byte BIG, word BIG:     231.4   &rarr; 43 67 66 66
     * FLOAT32, byte BIG, word LITTLE:  231.4   &rarr; 66 66 43 67   (registers swapped)
     * FLOAT32, byte LITTLE, word BIG:  231.4   &rarr; 67 43 66 66   (bytes swapped inside each register)
     * STRING, 2 registers:             "AB"    &rarr; 41 42 00 00   (ASCII, padded with zeros)
     * BYTES, 1 register:               "01ab"  &rarr; 01 AB         (hex string, exact length required)
     * </pre>
     */
    public static byte[] encodeRegisters(JsonElement value, ModbusMapping mapping, ModbusByteOrder byteOrder, ModbusByteOrder wordOrder) {
        int registers = mapping.getEffectiveObjectsCount();
        ModbusDataType type = mapping.getType();
        byte[] logical = switch (type) {
            case STRING -> stringBytes(asString(value), registers);
            case BYTES -> hexBytes(asString(value), registers);
            default -> numberBytes(type, unscale(asNumber(value), mapping.getMultiplier()));
        };
        // ordered() is its own inverse: the same reordering turns logical bytes back into wire bytes
        return ordered(logical, 0, registers, byteOrder, type.isNumeric() ? wordOrder : ModbusByteOrder.BIG);
    }

    /**
     * Value of a coil (function codes 5 and 15) or of one register bit (function code 22):
     * {@code true/false}, a number equal to 1 or 0, or their string forms.
     * <p>
     * Examples:
     * <pre>
     * true, 1, 1.0, "true", "TRUE", "1", "1.0"   &rarr; true  (on)
     * false, 0, 0.0, "false", "0"                &rarr; false (off)
     * 2, 0.5, -1, "on", {"a":1}                  &rarr; rejected
     * </pre>
     */
    public static boolean encodeCoil(JsonElement value) {
        if (value instanceof JsonPrimitive primitive) {
            if (primitive.isBoolean()) {
                return primitive.getAsBoolean();
            }
            String text = primitive.getAsString().trim();
            if (text.equalsIgnoreCase("true")) {
                return true;
            }
            if (text.equalsIgnoreCase("false")) {
                return false;
            }
            try {
                // compared by value, so 1, 1.0 and "1.00" are all "on"
                BigDecimal number = new BigDecimal(text);
                if (number.compareTo(BigDecimal.ONE) == 0) {
                    return true;
                }
                if (number.signum() == 0) {
                    return false;
                }
            } catch (NumberFormatException e) {
                // not a number: rejected below
            }
        }
        throw new IllegalArgumentException("Expected true/false or 1/0 but got " + value);
    }

    /** Masks for function code 22 that set or clear one bit and leave the others: {@code [andMask, orMask]}. */
    public static int[] maskFor(boolean set, int bit) {
        int andMask = ~(1 << bit) & 0xFFFF;
        int orMask = set ? 1 << bit : 0;
        return new int[]{andMask, orMask};
    }

    private static byte[] numberBytes(ModbusDataType type, BigDecimal value) {
        ByteBuffer buf = ByteBuffer.allocate(type.getRegisters() * 2);
        switch (type) {
            case INT16 -> buf.putShort((short) integer(value, type, -32768, 32767).intValue());
            case UINT16 -> buf.putShort((short) integer(value, type, 0, 65535).intValue());
            case INT32 -> buf.putInt((int) integer(value, type, Integer.MIN_VALUE, Integer.MAX_VALUE).longValue());
            case UINT32 -> buf.putInt((int) integer(value, type, 0, 0xFFFFFFFFL).longValue());
            case INT64 -> buf.putLong(integer(value, type, BigInteger.valueOf(Long.MIN_VALUE), BigInteger.valueOf(Long.MAX_VALUE)).longValue());
            case UINT64 -> buf.putLong(integer(value, type, BigInteger.ZERO, BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE)).longValue());
            case FLOAT16 -> {
                short half = Float.floatToFloat16(value.floatValue());
                requireFinite(Float.float16ToFloat(half), value, type);
                buf.putShort(half);
            }
            case FLOAT32 -> {
                float single = value.floatValue();
                requireFinite(single, value, type);
                buf.putFloat(single);
            }
            case FLOAT64 -> {
                double wide = value.doubleValue();
                requireFinite(wide, value, type);
                buf.putDouble(wide);
            }
            default -> throw new IllegalArgumentException("Not a numeric type: " + type);
        }
        return buf.array();
    }

    /** A decimal too large for the float type converts to infinity, which must not be written as if it were a value. */
    private static void requireFinite(double converted, BigDecimal value, ModbusDataType type) {
        if (Double.isInfinite(converted)) {
            throw new IllegalArgumentException("Value " + value.toPlainString() + " is out of range for " + type);
        }
    }

    private static BigInteger integer(BigDecimal value, ModbusDataType type, long min, long max) {
        return integer(value, type, BigInteger.valueOf(min), BigInteger.valueOf(max));
    }

    private static BigInteger integer(BigDecimal value, ModbusDataType type, BigInteger min, BigInteger max) {
        BigInteger rounded = value.setScale(0, RoundingMode.HALF_UP).toBigInteger();
        if (rounded.compareTo(min) < 0 || rounded.compareTo(max) > 0) {
            throw new IllegalArgumentException("Value " + value.toPlainString() + " is out of range for " + type + " (" + min + ".." + max + ")");
        }
        return rounded;
    }

    private static BigDecimal unscale(BigDecimal value, Double multiplier) {
        return multiplier == null ? value : value.divide(BigDecimal.valueOf(multiplier), MathContext.DECIMAL128);
    }

    private static BigDecimal asNumber(JsonElement value) {
        if (value instanceof JsonPrimitive primitive) {
            if (primitive.isBoolean()) {
                return primitive.getAsBoolean() ? BigDecimal.ONE : BigDecimal.ZERO;
            }
            try {
                return new BigDecimal(primitive.getAsString().trim());
            } catch (NumberFormatException e) {
                // fall through
            }
        }
        throw new IllegalArgumentException("Expected a number but got " + value);
    }

    private static String asString(JsonElement value) {
        if (value instanceof JsonPrimitive primitive) {
            return primitive.getAsString();
        }
        throw new IllegalArgumentException("Expected a string but got " + value);
    }

    private static byte[] stringBytes(String text, int registers) {
        if (!text.chars().allMatch(c -> c < 128)) {
            throw new IllegalArgumentException("Only ASCII characters can be written");
        }
        byte[] ascii = text.getBytes(StandardCharsets.US_ASCII);
        if (ascii.length > registers * 2) {
            throw new IllegalArgumentException("Text of " + ascii.length + " characters does not fit into " + registers + " registers");
        }
        byte[] padded = new byte[registers * 2];
        System.arraycopy(ascii, 0, padded, 0, ascii.length);
        return padded;
    }

    private static byte[] hexBytes(String hex, int registers) {
        byte[] bytes;
        try {
            bytes = HexFormat.of().parseHex(hex.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Expected a hex string but got '" + hex + "'");
        }
        if (bytes.length != registers * 2) {
            throw new IllegalArgumentException("Expected " + registers * 2 + " bytes but got " + bytes.length);
        }
        return bytes;
    }

}
