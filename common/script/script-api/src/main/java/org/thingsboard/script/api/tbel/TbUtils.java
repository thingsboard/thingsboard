/**
 * Copyright © 2016-2023 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.script.api.tbel;

import org.mvel2.ExecutionContext;
import org.mvel2.ParserConfiguration;
import org.mvel2.execution.ExecutionArrayList;
import org.mvel2.execution.ExecutionHashMap;
import org.mvel2.util.MethodStub;
import org.thingsboard.server.common.data.StringUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TbUtils {

    private static final byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes(StandardCharsets.US_ASCII);

    public static void register(ParserConfiguration parserConfig) throws Exception {
        parserConfig.addImport("btoa", new MethodStub(TbUtils.class.getMethod("btoa",
                String.class)));
        parserConfig.addImport("atob", new MethodStub(TbUtils.class.getMethod("atob",
                String.class)));
        parserConfig.addImport("bytesToString", new MethodStub(TbUtils.class.getMethod("bytesToString",
                List.class)));
        parserConfig.addImport("bytesToString", new MethodStub(TbUtils.class.getMethod("bytesToString",
                List.class, String.class)));
        parserConfig.addImport("decodeToString", new MethodStub(TbUtils.class.getMethod("bytesToString",
                List.class)));
        parserConfig.addImport("decodeToJson", new MethodStub(TbUtils.class.getMethod("decodeToJson",
                ExecutionContext.class, List.class)));
        parserConfig.addImport("stringToBytes", new MethodStub(TbUtils.class.getMethod("stringToBytes",
                ExecutionContext.class, String.class)));
        parserConfig.addImport("stringToBytes", new MethodStub(TbUtils.class.getMethod("stringToBytes",
                ExecutionContext.class, String.class, String.class)));
        parserConfig.addImport("parseInt", new MethodStub(TbUtils.class.getMethod("parseInt",
                String.class)));
        parserConfig.addImport("parseInt", new MethodStub(TbUtils.class.getMethod("parseInt",
                String.class, int.class)));
        parserConfig.addImport("parseFloat", new MethodStub(TbUtils.class.getMethod("parseFloat",
                String.class)));
        parserConfig.addImport("parseDouble", new MethodStub(TbUtils.class.getMethod("parseDouble",
                String.class)));
        parserConfig.addImport("parseLittleEndianHexToInt", new MethodStub(TbUtils.class.getMethod("parseLittleEndianHexToInt",
                String.class)));
        parserConfig.addImport("parseBigEndianHexToInt", new MethodStub(TbUtils.class.getMethod("parseBigEndianHexToInt",
                String.class)));
        parserConfig.addImport("parseHexToInt", new MethodStub(TbUtils.class.getMethod("parseHexToInt",
                String.class)));
        parserConfig.addImport("parseHexToInt", new MethodStub(TbUtils.class.getMethod("parseHexToInt",
                String.class, boolean.class)));
        parserConfig.addImport("parseBytesToInt", new MethodStub(TbUtils.class.getMethod("parseBytesToInt",
                List.class, int.class, int.class)));
        parserConfig.addImport("parseBytesToInt", new MethodStub(TbUtils.class.getMethod("parseBytesToInt",
                List.class, int.class, int.class, boolean.class)));
        parserConfig.addImport("parseBytesToInt", new MethodStub(TbUtils.class.getMethod("parseBytesToInt",
                byte[].class, int.class, int.class)));
        parserConfig.addImport("parseBytesToInt", new MethodStub(TbUtils.class.getMethod("parseBytesToInt",
                byte[].class, int.class, int.class, boolean.class)));
        parserConfig.addImport("toFixed", new MethodStub(TbUtils.class.getMethod("toFixed",
                double.class, int.class)));
        parserConfig.addImport("hexToBytes", new MethodStub(TbUtils.class.getMethod("hexToBytes",
                ExecutionContext.class, String.class)));
        parserConfig.addImport("base64ToHex", new MethodStub(TbUtils.class.getMethod("base64ToHex",
                String.class)));
        parserConfig.addImport("base64ToBytes", new MethodStub(TbUtils.class.getMethod("base64ToBytes",
                String.class)));
        parserConfig.addImport("bytesToBase64", new MethodStub(TbUtils.class.getMethod("bytesToBase64",
                byte[].class)));
        parserConfig.addImport("bytesToHex", new MethodStub(TbUtils.class.getMethod("bytesToHex",
                byte[].class)));
        parserConfig.addImport("bytesToHex", new MethodStub(TbUtils.class.getMethod("bytesToHex",
                ExecutionArrayList.class)));
        parserConfig.addImport("toFlatMap", new MethodStub(TbUtils.class.getMethod("toFlatMap",
                ExecutionContext.class, Map.class)));
        parserConfig.addImport("toFlatMap", new MethodStub(TbUtils.class.getMethod("toFlatMap",
                ExecutionContext.class, Map.class, boolean.class)));
        parserConfig.addImport("toFlatMap", new MethodStub(TbUtils.class.getMethod("toFlatMap",
                ExecutionContext.class, Map.class, List.class)));
        parserConfig.addImport("toFlatMap", new MethodStub(TbUtils.class.getMethod("toFlatMap",
                ExecutionContext.class, Map.class, List.class, boolean.class)));
    }

    public static String btoa(String input) {
        return new String(Base64.getEncoder().encode(input.getBytes()));
    }

    public static String atob(String encoded) {
        return new String(Base64.getDecoder().decode(encoded));
    }

    public static Object decodeToJson(ExecutionContext ctx, List<Byte> bytesList) throws IOException {
        return TbJson.parse(ctx, bytesToString(bytesList));
    }

    public static String bytesToString(List<Byte> bytesList) {
        byte[] bytes = bytesFromList(bytesList);
        return new String(bytes);
    }

    public static String bytesToString(List<Byte> bytesList, String charsetName) throws UnsupportedEncodingException {
        byte[] bytes = bytesFromList(bytesList);
        return new String(bytes, charsetName);
    }

    public static List<Byte> stringToBytes(ExecutionContext ctx, String str) {
        byte[] bytes = str.getBytes();
        return bytesToList(ctx, bytes);
    }

    public static List<Byte> stringToBytes(ExecutionContext ctx, String str, String charsetName) throws UnsupportedEncodingException {
        byte[] bytes = str.getBytes(charsetName);
        return bytesToList(ctx, bytes);
    }

    private static byte[] bytesFromList(List<Byte> bytesList) {
        byte[] bytes = new byte[bytesList.size()];
        for (int i = 0; i < bytesList.size(); i++) {
            bytes[i] = bytesList.get(i);
        }
        return bytes;
    }

    private static List<Byte> bytesToList(ExecutionContext ctx, byte[] bytes) {
        List<Byte> list = new ExecutionArrayList<>(ctx);
        for (byte aByte : bytes) {
            list.add(aByte);
        }
        return list;
    }

    public static Integer parseInt(String value) {
        if (value != null) {
            try {
                int radix = 10;
                if (isHexadecimal(value)) {
                    radix = 16;
                }
                return Integer.parseInt(prepareNumberString(value), radix);
            } catch (NumberFormatException e) {
                Float f = parseFloat(value);
                if (f != null) {
                    return f.intValue();
                }
            }
        }
        return null;
    }

    public static Integer parseInt(String value, int radix) {
        if (value != null) {
            try {
                return Integer.parseInt(prepareNumberString(value), radix);
            } catch (NumberFormatException e) {
                Float f = parseFloat(value);
                if (f != null) {
                    return f.intValue();
                }
            }
        }
        return null;
    }

    public static Float parseFloat(String value) {
        if (value != null) {
            try {
                return Float.parseFloat(prepareNumberString(value));
            } catch (NumberFormatException e) {
            }
        }
        return null;
    }

    public static Double parseDouble(String value) {
        if (value != null) {
            try {
                return Double.parseDouble(prepareNumberString(value));
            } catch (NumberFormatException e) {
            }
        }
        return null;
    }

    public static int parseLittleEndianHexToInt(String hex) {
        return parseHexToInt(hex, false);
    }

    public static int parseBigEndianHexToInt(String hex) {
        return parseHexToInt(hex, true);
    }

    public static int parseHexToInt(String hex) {
        return parseHexToInt(hex, true);
    }

    public static int parseHexToInt(String hex, boolean bigEndian) {
        int length = hex.length();
        if (length > 8) {
            throw new IllegalArgumentException("Hex string is too large. Maximum 8 symbols allowed.");
        }
        if (length % 2 > 0) {
            throw new IllegalArgumentException("Hex string must be even-length.");
        }
        byte[] data = new byte[length / 2];
        for (int i = 0; i < length; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4) + Character.digit(hex.charAt(i + 1), 16));
        }
        return parseBytesToInt(data, 0, data.length, bigEndian);
    }

    public static ExecutionArrayList<Byte> hexToBytes(ExecutionContext ctx, String hex) {
        int len = hex.length();
        if (len % 2 > 0) {
            throw new IllegalArgumentException("Hex string must be even-length.");
        }
        ExecutionArrayList<Byte> data = new ExecutionArrayList<>(ctx);
        for (int i = 0; i < len; i += 2) {
            data.add((byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16)));
        }
        return data;
    }

    public static String base64ToHex(String base64) {
        return bytesToHex(Base64.getDecoder().decode(base64));
    }

    public static String bytesToBase64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static byte[] base64ToBytes(String input) {
        return Base64.getDecoder().decode(input);
    }

    public static int parseBytesToInt(List<Byte> data, int offset, int length) {
        return parseBytesToInt(data, offset, length, true);
    }

    public static int parseBytesToInt(List<Byte> data, int offset, int length, boolean bigEndian) {
        final byte[] bytes = new byte[data.size()];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = data.get(i);
        }
        return parseBytesToInt(bytes, offset, length, bigEndian);
    }

    public static int parseBytesToInt(byte[] data, int offset, int length) {
        return parseBytesToInt(data, offset, length, true);
    }

    public static int parseBytesToInt(byte[] data, int offset, int length, boolean bigEndian) {
        if (offset > data.length) {
            throw new IllegalArgumentException("Offset: " + offset + " is out of bounds for array with length: " + data.length + "!");
        }
        if (length > 4) {
            throw new IllegalArgumentException("Length: " + length + " is too large. Maximum 4 bytes is allowed!");
        }
        if (offset + length > data.length) {
            throw new IllegalArgumentException("Offset: " + offset + " and Length: " + length + " is out of bounds for array with length: " + data.length + "!");
        }
        var bb = ByteBuffer.allocate(4);
        if (!bigEndian) {
            bb.order(ByteOrder.LITTLE_ENDIAN);
        }
        bb.position(bigEndian ? 4 - length : 0);
        bb.put(data, offset, length);
        bb.position(0);
        return bb.getInt();
    }

    public static String bytesToHex(ExecutionArrayList<?> bytesList) {
        byte[] bytes = new byte[bytesList.size()];
        for (int i = 0; i < bytesList.size(); i++) {
            bytes[i] = Byte.parseByte(bytesList.get(i).toString());
        }
        return bytesToHex(bytes);
    }

    public static String bytesToHex(byte[] bytes) {
        byte[] hexChars = new byte[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars, StandardCharsets.UTF_8);
    }

    public static double toFixed(double value, int precision) {
        return BigDecimal.valueOf(value).setScale(precision, RoundingMode.HALF_UP).doubleValue();
    }

    private static boolean isHexadecimal(String value) {
        return value != null && (value.contains("0x") || value.contains("0X"));
    }

    private static String prepareNumberString(String value) {
        if (value != null) {
            value = value.trim();
            if (isHexadecimal(value)) {
                value = value.replace("0x", "");
                value = value.replace("0X", "");
            }
            value = value.replace(",", ".");
        }
        return value;
    }

    public static ExecutionHashMap<String, Object> toFlatMap(ExecutionContext ctx, Map<String, Object> json) {
        return toFlatMap(ctx, json, new ArrayList<>(), true);
    }

    public static ExecutionHashMap<String, Object> toFlatMap(ExecutionContext ctx, Map<String, Object> json, boolean pathInKey) {
        return toFlatMap(ctx, json, new ArrayList<>(), pathInKey);
    }

    public static ExecutionHashMap<String, Object> toFlatMap(ExecutionContext ctx, Map<String, Object> json, List<String> excludeList) {
        return toFlatMap(ctx, json, excludeList, true);
    }

    public static ExecutionHashMap<String, Object> toFlatMap(ExecutionContext ctx, Map<String, Object> json, List<String> excludeList, boolean pathInKey) {
        ExecutionHashMap<String, Object> map = new ExecutionHashMap<>(16, ctx);
        parseRecursive(json, map, excludeList, "", pathInKey);
        return map;
    }

    private static void parseRecursive(Object json, Map<String, Object> map, List<String> excludeList, String path, boolean pathInKey) {
        if (json instanceof Map.Entry) {
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) json;
            if (StringUtils.isNotBlank(path)) {
                path += ".";
            }
            if (excludeList.contains(entry.getKey())) {
                return;
            }
            path += entry.getKey();
            json = entry.getValue();
        }
        if (json instanceof Set || json instanceof List) {
            String arrayPath = path + ".";
            Object[] collection = ((Collection<?>) json).toArray();
            for (int index = 0; index < collection.length; index++) {
                parseRecursive(collection[index], map, excludeList, arrayPath + index, pathInKey);
            }
        } else if (json instanceof Map) {
            Map<?, ?> node = (Map<?, ?>) json;
            for (Map.Entry<?, ?> entry : node.entrySet()) {
                parseRecursive(entry, map, excludeList, path, pathInKey);
            }
        } else {
            if (pathInKey) {
                map.put(path, json);
            } else {
                String key = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
                map.put(key, json);
            }
        }
    }
}
