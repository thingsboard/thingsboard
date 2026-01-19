/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.script.api.tbel;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.primitives.Bytes;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.mvel2.ExecutionContext;
import org.mvel2.ParserConfiguration;
import org.mvel2.execution.ExecutionArrayList;
import org.mvel2.execution.ExecutionHashMap;
import org.mvel2.execution.ExecutionLinkedHashSet;
import org.mvel2.util.MethodStub;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.NumberUtils;
import org.thingsboard.common.util.geo.Coordinates;
import org.thingsboard.common.util.geo.GeoUtil;
import org.thingsboard.common.util.geo.RangeUnit;
import org.thingsboard.server.common.data.StringUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

import static java.lang.Character.MAX_RADIX;
import static java.lang.Character.MIN_RADIX;

@Slf4j
public class TbUtils {

    private static final byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes(StandardCharsets.US_ASCII);

    private static final int ZERO_RADIX = 0;
    private static final int OCTAL_RADIX = 8;
    private static final int DEC_RADIX = 10;
    private static final int HEX_RADIX = 16;
    private static final int HEX_LEN_MIN = -1;
    private static final int HEX_LEN_INT_MAX = 8;
    private static final int HEX_LEN_LONG_MAX = 16;
    private static final int BYTES_LEN_INT_MAX = 4;
    private static final int BYTES_LEN_LONG_MAX = 8;
    private static final int BIN_LEN_MAX = 8;

    private static final LinkedHashMap<String, String> mdnEncodingReplacements = new LinkedHashMap<>();

    static {
        mdnEncodingReplacements.put("\\+", "%20");
        mdnEncodingReplacements.put("%21", "!");
        mdnEncodingReplacements.put("%27", "'");
        mdnEncodingReplacements.put("%28", "\\(");
        mdnEncodingReplacements.put("%29", "\\)");
        mdnEncodingReplacements.put("%7E", "~");
        mdnEncodingReplacements.put("%3B", ";");
        mdnEncodingReplacements.put("%2C", ",");
        mdnEncodingReplacements.put("%2F", "/");
        mdnEncodingReplacements.put("%3F", "\\?");
        mdnEncodingReplacements.put("%3A", ":");
        mdnEncodingReplacements.put("%40", "@");
        mdnEncodingReplacements.put("%26", "&");
        mdnEncodingReplacements.put("%3D", "=");
        mdnEncodingReplacements.put("%2B", "\\+");
        mdnEncodingReplacements.put("%24", Matcher.quoteReplacement("$"));
        mdnEncodingReplacements.put("%23", "#");
    }

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
        parserConfig.addImport("decodeToJson", new MethodStub(TbUtils.class.getMethod("decodeToJson",
                ExecutionContext.class, String.class)));
        parserConfig.addImport("stringToBytes", new MethodStub(TbUtils.class.getMethod("stringToBytes",
                ExecutionContext.class, Object.class)));
        parserConfig.addImport("stringToBytes", new MethodStub(TbUtils.class.getMethod("stringToBytes",
                ExecutionContext.class, Object.class, String.class)));
        parserConfig.registerNonConvertableMethods(TbUtils.class, Collections.singleton("stringToBytes"));
        parserConfig.addImport("parseInt", new MethodStub(TbUtils.class.getMethod("parseInt",
                String.class)));
        parserConfig.addImport("parseInt", new MethodStub(TbUtils.class.getMethod("parseInt",
                String.class, int.class)));
        parserConfig.addImport("parseLong", new MethodStub(TbUtils.class.getMethod("parseLong",
                String.class)));
        parserConfig.addImport("parseLong", new MethodStub(TbUtils.class.getMethod("parseLong",
                String.class, int.class)));
        parserConfig.addImport("parseFloat", new MethodStub(TbUtils.class.getMethod("parseFloat",
                String.class)));
        parserConfig.addImport("parseFloat", new MethodStub(TbUtils.class.getMethod("parseFloat",
                String.class, int.class)));
        parserConfig.addImport("parseHexIntLongToFloat", new MethodStub(TbUtils.class.getMethod("parseHexIntLongToFloat",
                String.class, boolean.class)));
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
                List.class)));
        parserConfig.addImport("parseBytesToInt", new MethodStub(TbUtils.class.getMethod("parseBytesToInt",
                List.class, int.class)));
        parserConfig.addImport("parseBytesToInt", new MethodStub(TbUtils.class.getMethod("parseBytesToInt",
                List.class, int.class, int.class)));
        parserConfig.addImport("parseBytesToInt", new MethodStub(TbUtils.class.getMethod("parseBytesToInt",
                List.class, int.class, int.class, boolean.class)));
        parserConfig.addImport("parseBytesToInt", new MethodStub(TbUtils.class.getMethod("parseBytesToInt",
                byte[].class)));
        parserConfig.addImport("parseBytesToInt", new MethodStub(TbUtils.class.getMethod("parseBytesToInt",
                byte[].class, int.class)));
        parserConfig.addImport("parseBytesToInt", new MethodStub(TbUtils.class.getMethod("parseBytesToInt",
                byte[].class, int.class, int.class)));
        parserConfig.addImport("parseBytesToInt", new MethodStub(TbUtils.class.getMethod("parseBytesToInt",
                byte[].class, int.class, int.class, boolean.class)));
        parserConfig.addImport("parseLittleEndianHexToLong", new MethodStub(TbUtils.class.getMethod("parseLittleEndianHexToLong",
                String.class)));
        parserConfig.addImport("parseBigEndianHexToLong", new MethodStub(TbUtils.class.getMethod("parseBigEndianHexToLong",
                String.class)));
        parserConfig.addImport("parseHexToLong", new MethodStub(TbUtils.class.getMethod("parseHexToLong",
                String.class)));
        parserConfig.addImport("parseHexToLong", new MethodStub(TbUtils.class.getMethod("parseHexToLong",
                String.class, boolean.class)));
        parserConfig.addImport("parseBytesToLong", new MethodStub(TbUtils.class.getMethod("parseBytesToLong",
                List.class)));
        parserConfig.addImport("parseBytesToLong", new MethodStub(TbUtils.class.getMethod("parseBytesToLong",
                List.class, int.class)));
        parserConfig.addImport("parseBytesToLong", new MethodStub(TbUtils.class.getMethod("parseBytesToLong",
                List.class, int.class, int.class)));
        parserConfig.addImport("parseBytesToLong", new MethodStub(TbUtils.class.getMethod("parseBytesToLong",
                List.class, int.class, int.class, boolean.class)));
        parserConfig.addImport("parseBytesToLong", new MethodStub(TbUtils.class.getMethod("parseBytesToLong",
                byte[].class)));
        parserConfig.addImport("parseBytesToLong", new MethodStub(TbUtils.class.getMethod("parseBytesToLong",
                byte[].class, int.class)));
        parserConfig.addImport("parseBytesToLong", new MethodStub(TbUtils.class.getMethod("parseBytesToLong",
                byte[].class, int.class, int.class)));
        parserConfig.addImport("parseBytesToLong", new MethodStub(TbUtils.class.getMethod("parseBytesToLong",
                byte[].class, int.class, int.class, boolean.class)));
        parserConfig.addImport("parseLittleEndianHexToFloat", new MethodStub(TbUtils.class.getMethod("parseLittleEndianHexToFloat",
                String.class)));
        parserConfig.addImport("parseBigEndianHexToFloat", new MethodStub(TbUtils.class.getMethod("parseBigEndianHexToFloat",
                String.class)));
        parserConfig.addImport("parseHexToFloat", new MethodStub(TbUtils.class.getMethod("parseHexToFloat",
                String.class)));
        parserConfig.addImport("parseHexToFloat", new MethodStub(TbUtils.class.getMethod("parseHexToFloat",
                String.class, boolean.class)));
        parserConfig.addImport("parseBytesToFloat", new MethodStub(TbUtils.class.getMethod("parseBytesToFloat",
                List.class)));
        parserConfig.addImport("parseBytesToFloat", new MethodStub(TbUtils.class.getMethod("parseBytesToFloat",
                List.class, int.class)));
        parserConfig.addImport("parseBytesToFloat", new MethodStub(TbUtils.class.getMethod("parseBytesToFloat",
                List.class, int.class, int.class)));
        parserConfig.addImport("parseBytesToFloat", new MethodStub(TbUtils.class.getMethod("parseBytesToFloat",
                List.class, int.class, int.class, boolean.class)));
        parserConfig.addImport("parseBytesToFloat", new MethodStub(TbUtils.class.getMethod("parseBytesToFloat",
                byte[].class)));
        parserConfig.addImport("parseBytesToFloat", new MethodStub(TbUtils.class.getMethod("parseBytesToFloat",
                byte[].class, int.class)));
        parserConfig.addImport("parseBytesToFloat", new MethodStub(TbUtils.class.getMethod("parseBytesToFloat",
                byte[].class, int.class, int.class)));
        parserConfig.addImport("parseBytesToFloat", new MethodStub(TbUtils.class.getMethod("parseBytesToFloat",
                byte[].class, int.class, int.class, boolean.class)));
        parserConfig.addImport("parseBytesIntToFloat", new MethodStub(TbUtils.class.getMethod("parseBytesIntToFloat",
                List.class)));
        parserConfig.addImport("parseBytesIntToFloat", new MethodStub(TbUtils.class.getMethod("parseBytesIntToFloat",
                List.class, int.class)));
        parserConfig.addImport("parseBytesIntToFloat", new MethodStub(TbUtils.class.getMethod("parseBytesIntToFloat",
                List.class, int.class, int.class)));
        parserConfig.addImport("parseBytesIntToFloat", new MethodStub(TbUtils.class.getMethod("parseBytesIntToFloat",
                List.class, int.class, int.class, boolean.class)));
        parserConfig.addImport("parseBytesIntToFloat", new MethodStub(TbUtils.class.getMethod("parseBytesIntToFloat",
                byte[].class)));
        parserConfig.addImport("parseBytesIntToFloat", new MethodStub(TbUtils.class.getMethod("parseBytesIntToFloat",
                byte[].class, int.class)));
        parserConfig.addImport("parseBytesIntToFloat", new MethodStub(TbUtils.class.getMethod("parseBytesIntToFloat",
                byte[].class, int.class, int.class)));
        parserConfig.addImport("parseBytesIntToFloat", new MethodStub(TbUtils.class.getMethod("parseBytesIntToFloat",
                byte[].class, int.class, int.class, boolean.class)));
        parserConfig.addImport("parseLittleEndianHexToDouble", new MethodStub(TbUtils.class.getMethod("parseLittleEndianHexToDouble",
                String.class)));
        parserConfig.addImport("parseBigEndianHexToDouble", new MethodStub(TbUtils.class.getMethod("parseBigEndianHexToDouble",
                String.class)));
        parserConfig.addImport("parseHexToDouble", new MethodStub(TbUtils.class.getMethod("parseHexToDouble",
                String.class)));
        parserConfig.addImport("parseHexToDouble", new MethodStub(TbUtils.class.getMethod("parseHexToDouble",
                String.class, boolean.class)));
        parserConfig.addImport("parseBytesToDouble", new MethodStub(TbUtils.class.getMethod("parseBytesToDouble",
                List.class)));
        parserConfig.addImport("parseBytesToDouble", new MethodStub(TbUtils.class.getMethod("parseBytesToDouble",
                List.class, int.class)));
        parserConfig.addImport("parseBytesToDouble", new MethodStub(TbUtils.class.getMethod("parseBytesToDouble",
                List.class, int.class, int.class)));
        parserConfig.addImport("parseBytesToDouble", new MethodStub(TbUtils.class.getMethod("parseBytesToDouble",
                List.class, int.class, int.class, boolean.class)));
        parserConfig.addImport("parseBytesToDouble", new MethodStub(TbUtils.class.getMethod("parseBytesToDouble",
                byte[].class)));
        parserConfig.addImport("parseBytesToDouble", new MethodStub(TbUtils.class.getMethod("parseBytesToDouble",
                byte[].class, int.class)));
        parserConfig.addImport("parseBytesToDouble", new MethodStub(TbUtils.class.getMethod("parseBytesToDouble",
                byte[].class, int.class, int.class)));
        parserConfig.addImport("parseBytesToDouble", new MethodStub(TbUtils.class.getMethod("parseBytesToDouble",
                byte[].class, int.class, int.class, boolean.class)));
        parserConfig.addImport("parseBytesLongToDouble", new MethodStub(TbUtils.class.getMethod("parseBytesLongToDouble",
                List.class)));
        parserConfig.addImport("parseBytesLongToDouble", new MethodStub(TbUtils.class.getMethod("parseBytesLongToDouble",
                List.class, int.class)));
        parserConfig.addImport("parseBytesLongToDouble", new MethodStub(TbUtils.class.getMethod("parseBytesLongToDouble",
                List.class, int.class, int.class)));
        parserConfig.addImport("parseBytesLongToDouble", new MethodStub(TbUtils.class.getMethod("parseBytesLongToDouble",
                List.class, int.class, int.class, boolean.class)));
        parserConfig.addImport("parseBytesLongToDouble", new MethodStub(TbUtils.class.getMethod("parseBytesLongToDouble",
                byte[].class)));
        parserConfig.addImport("parseBytesLongToDouble", new MethodStub(TbUtils.class.getMethod("parseBytesLongToDouble",
                byte[].class, int.class)));
        parserConfig.addImport("parseBytesLongToDouble", new MethodStub(TbUtils.class.getMethod("parseBytesLongToDouble",
                byte[].class, int.class, int.class)));
        parserConfig.addImport("parseBytesLongToDouble", new MethodStub(TbUtils.class.getMethod("parseBytesLongToDouble",
                byte[].class, int.class, int.class, boolean.class)));
        parserConfig.addImport("toFixed", new MethodStub(NumberUtils.class.getMethod("toFixed",
                double.class, int.class)));
        parserConfig.addImport("toFixed", new MethodStub(NumberUtils.class.getMethod("toFixed",
                float.class, int.class)));
        parserConfig.addImport("toInt", new MethodStub(NumberUtils.class.getMethod("toInt",
                double.class)));
        parserConfig.addImport("isNaN", new MethodStub(NumberUtils.class.getMethod("isNaN",
                double.class)));
        parserConfig.addImport("hexToBytes", new MethodStub(TbUtils.class.getMethod("hexToBytes",
                ExecutionContext.class, String.class)));
        parserConfig.addImport("hexToBytesArray", new MethodStub(TbUtils.class.getMethod("hexToBytesArray",
                String.class)));
        parserConfig.addImport("intToHex", new MethodStub(TbUtils.class.getMethod("intToHex",
                Integer.class)));
        parserConfig.addImport("intToHex", new MethodStub(TbUtils.class.getMethod("intToHex",
                Integer.class, boolean.class)));
        parserConfig.addImport("intToHex", new MethodStub(TbUtils.class.getMethod("intToHex",
                Integer.class, boolean.class, boolean.class)));
        parserConfig.addImport("intToHex", new MethodStub(TbUtils.class.getMethod("intToHex",
                Integer.class, boolean.class, boolean.class, int.class)));
        parserConfig.addImport("longToHex", new MethodStub(TbUtils.class.getMethod("longToHex",
                Long.class)));
        parserConfig.addImport("longToHex", new MethodStub(TbUtils.class.getMethod("longToHex",
                Long.class, boolean.class)));
        parserConfig.addImport("longToHex", new MethodStub(TbUtils.class.getMethod("longToHex",
                Long.class, boolean.class, boolean.class)));
        parserConfig.addImport("longToHex", new MethodStub(TbUtils.class.getMethod("longToHex",
                Long.class, boolean.class, boolean.class, int.class)));
        parserConfig.addImport("intLongToRadixString", new MethodStub(TbUtils.class.getMethod("intLongToRadixString",
                Long.class)));
        parserConfig.addImport("intLongToRadixString", new MethodStub(TbUtils.class.getMethod("intLongToRadixString",
                Long.class, int.class)));
        parserConfig.addImport("intLongToRadixString", new MethodStub(TbUtils.class.getMethod("intLongToRadixString",
                Long.class, int.class, boolean.class)));
        parserConfig.addImport("intLongToRadixString", new MethodStub(TbUtils.class.getMethod("intLongToRadixString",
                Long.class, int.class, boolean.class, boolean.class)));
        parserConfig.addImport("floatToHex", new MethodStub(TbUtils.class.getMethod("floatToHex",
                Float.class)));
        parserConfig.addImport("floatToHex", new MethodStub(TbUtils.class.getMethod("floatToHex",
                Float.class, boolean.class)));
        parserConfig.addImport("doubleToHex", new MethodStub(TbUtils.class.getMethod("doubleToHex",
                Double.class)));
        parserConfig.addImport("doubleToHex", new MethodStub(TbUtils.class.getMethod("doubleToHex",
                Double.class, boolean.class)));
        parserConfig.addImport("printUnsignedBytes", new MethodStub(TbUtils.class.getMethod("printUnsignedBytes",
                ExecutionContext.class, List.class)));
        parserConfig.addImport("base64ToHex", new MethodStub(TbUtils.class.getMethod("base64ToHex",
                String.class)));
        parserConfig.addImport("hexToBase64", new MethodStub(TbUtils.class.getMethod("hexToBase64",
                String.class)));
        parserConfig.addImport("base64ToBytes", new MethodStub(TbUtils.class.getMethod("base64ToBytes",
                String.class)));
        parserConfig.addImport("base64ToBytesList", new MethodStub(TbUtils.class.getMethod("base64ToBytesList",
                ExecutionContext.class, String.class)));
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
        parserConfig.addImport("encodeURI", new MethodStub(TbUtils.class.getMethod("encodeURI",
                String.class)));
        parserConfig.addImport("decodeURI", new MethodStub(TbUtils.class.getMethod("decodeURI",
                String.class)));
        parserConfig.addImport("raiseError", new MethodStub(TbUtils.class.getMethod("raiseError",
                String.class)));
        parserConfig.addImport("isBinary", new MethodStub(TbUtils.class.getMethod("isBinary",
                String.class)));
        parserConfig.addImport("isOctal", new MethodStub(TbUtils.class.getMethod("isOctal",
                String.class)));
        parserConfig.addImport("isDecimal", new MethodStub(TbUtils.class.getMethod("isDecimal",
                String.class)));
        parserConfig.addImport("isHexadecimal", new MethodStub(TbUtils.class.getMethod("isHexadecimal",
                String.class)));
        parserConfig.addImport("bytesToExecutionArrayList", new MethodStub(TbUtils.class.getMethod("bytesToExecutionArrayList",
                ExecutionContext.class, byte[].class)));
        parserConfig.addImport("padStart", new MethodStub(TbUtils.class.getMethod("padStart",
                String.class, int.class, char.class)));
        parserConfig.addImport("padEnd", new MethodStub(TbUtils.class.getMethod("padEnd",
                String.class, int.class, char.class)));
        parserConfig.addImport("parseByteToBinaryArray", new MethodStub(TbUtils.class.getMethod("parseByteToBinaryArray",
                byte.class)));
        parserConfig.addImport("parseByteToBinaryArray", new MethodStub(TbUtils.class.getMethod("parseByteToBinaryArray",
                byte.class, int.class)));
        parserConfig.addImport("parseByteToBinaryArray", new MethodStub(TbUtils.class.getMethod("parseByteToBinaryArray",
                byte.class, int.class, boolean.class)));
        parserConfig.addImport("parseBytesToBinaryArray", new MethodStub(TbUtils.class.getMethod("parseBytesToBinaryArray",
                List.class)));
        parserConfig.addImport("parseBytesToBinaryArray", new MethodStub(TbUtils.class.getMethod("parseBytesToBinaryArray",
                List.class, int.class)));
        parserConfig.addImport("parseBytesToBinaryArray", new MethodStub(TbUtils.class.getMethod("parseBytesToBinaryArray",
                byte[].class)));
        parserConfig.addImport("parseBytesToBinaryArray", new MethodStub(TbUtils.class.getMethod("parseBytesToBinaryArray",
                byte[].class)));
        parserConfig.addImport("parseBytesToBinaryArray", new MethodStub(TbUtils.class.getMethod("parseBytesToBinaryArray",
                byte[].class, int.class)));
        parserConfig.addImport("parseLongToBinaryArray", new MethodStub(TbUtils.class.getMethod("parseLongToBinaryArray",
                long.class)));
        parserConfig.addImport("parseLongToBinaryArray", new MethodStub(TbUtils.class.getMethod("parseLongToBinaryArray",
                long.class, int.class)));
        parserConfig.addImport("parseBinaryArrayToInt", new MethodStub(TbUtils.class.getMethod("parseBinaryArrayToInt",
                List.class)));
        parserConfig.addImport("parseBinaryArrayToInt", new MethodStub(TbUtils.class.getMethod("parseBinaryArrayToInt",
                List.class, int.class)));
        parserConfig.addImport("parseBinaryArrayToInt", new MethodStub(TbUtils.class.getMethod("parseBinaryArrayToInt",
                List.class, int.class, int.class)));
        parserConfig.addImport("parseBinaryArrayToInt", new MethodStub(TbUtils.class.getMethod("parseBinaryArrayToInt",
                byte[].class)));
        parserConfig.addImport("parseBinaryArrayToInt", new MethodStub(TbUtils.class.getMethod("parseBinaryArrayToInt",
                byte[].class, int.class)));
        parserConfig.addImport("parseBinaryArrayToInt", new MethodStub(TbUtils.class.getMethod("parseBinaryArrayToInt",
                byte[].class, int.class, int.class)));
        parserConfig.addImport("isInsidePolygon", new MethodStub(TbUtils.class.getMethod("isInsidePolygon",
                double.class, double.class, String.class)));
        parserConfig.addImport("isInsideCircle", new MethodStub(TbUtils.class.getMethod("isInsideCircle",
                double.class, double.class, String.class)));
        parserConfig.addImport("isMap", new MethodStub(TbUtils.class.getMethod("isMap",
                Object.class)));
        parserConfig.addImport("isList", new MethodStub(TbUtils.class.getMethod("isList",
                Object.class)));
        parserConfig.addImport("isArray", new MethodStub(TbUtils.class.getMethod("isArray",
                Object.class)));
        parserConfig.addImport("newSet", new MethodStub(TbUtils.class.getMethod("newSet",
                ExecutionContext.class)));
        parserConfig.addImport("toSet", new MethodStub(TbUtils.class.getMethod("toSet",
                ExecutionContext.class, List.class)));
        parserConfig.addImport("isSet", new MethodStub(TbUtils.class.getMethod("isSet",
                Object.class)));
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

    public static Object decodeToJson(ExecutionContext ctx, String jsonStr) throws IOException {
        return TbJson.parse(ctx, jsonStr);
    }

    public static String bytesToString(List<?> bytesList) {
        byte[] bytes = bytesFromList(bytesList);
        return new String(bytes);
    }

    public static String bytesToString(List<?> bytesList, String charsetName) throws UnsupportedEncodingException {
        byte[] bytes = bytesFromList(bytesList);
        return new String(bytes, charsetName);
    }

    public static List<Byte> stringToBytes(ExecutionContext ctx, Object str) throws IllegalAccessException {
        if (str instanceof String) {
            byte[] bytes = str.toString().getBytes();
            return bytesToList(ctx, bytes);
        } else {
            throw new IllegalAccessException("Invalid type parameter [" + str.getClass().getSimpleName() + "]. Expected 'String'");
        }
    }

    public static List<Byte> stringToBytes(ExecutionContext ctx, Object str, String charsetName) throws UnsupportedEncodingException, IllegalAccessException {
        if (str instanceof String) {
            byte[] bytes = str.toString().getBytes(charsetName);
            return bytesToList(ctx, bytes);
        } else {
            throw new IllegalAccessException("Invalid type parameter [" + str.getClass().getSimpleName() + "]. Expected 'String'");
        }
    }

    private static byte[] bytesFromList(List<?> bytesList) {
        byte[] bytes = new byte[bytesList.size()];
        for (int i = 0; i < bytesList.size(); i++) {
            Object objectVal = bytesList.get(i);
            if (objectVal instanceof Integer) {
                bytes[i] = isValidIntegerToByte((Integer) objectVal);
            } else if (objectVal instanceof String) {
                bytes[i] = isValidIntegerToByte(parseInt((String) objectVal));
            } else if (objectVal instanceof Byte) {
                bytes[i] = (byte) objectVal;
            } else {
                throw new NumberFormatException("The value '" + objectVal + "' could not be correctly converted to a byte. " +
                        "Must be a HexDecimal/String/Integer/Byte format !");
            }
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
        return parseInt(value, ZERO_RADIX);
    }

    public static Integer parseInt(String value, int radix) {
        return parseInt(value, radix, true);
    }

    private static Integer parseInt(String value, int radix, boolean bigEndian) {
        String valueP = prepareNumberString(value, bigEndian);
        if (valueP != null) {
            int radixValue = isValidStringAndRadix(valueP, radix, value);
            if (radixValue >= 25 && radixValue <= MAX_RADIX) {
                return (Integer) compareIntLongValueMinMax(valueP, radixValue, Integer.MAX_VALUE, Integer.MIN_VALUE);
            }
            return switch (radixValue) {
                case MIN_RADIX -> parseBinaryStringAsSignedInteger(valueP);
                case OCTAL_RADIX, DEC_RADIX, HEX_RADIX -> Integer.parseInt(valueP, radixValue);
                default -> throw new IllegalArgumentException("Invalid radix: [" + radix + "]");
            };
        }
        return null;
    }

    public static Long parseLong(String value) {
        return parseLong(value, ZERO_RADIX);
    }

    public static Long parseLong(String value, int radix) {
        return parseLong(value, radix, true);
    }

    private static Long parseLong(String value, int radix, boolean bigEndian) {
        String valueP = prepareNumberString(value, bigEndian);
        if (valueP != null) {
            int radixValue = isValidStringAndRadix(valueP, radix, value);
            if (radixValue >= 25 && radixValue <= MAX_RADIX) {
                return (Long) compareIntLongValueMinMax(valueP, radixValue, Long.MAX_VALUE, Long.MIN_VALUE);
            }
            return switch (radixValue) {
                case MIN_RADIX -> parseBinaryStringAsSignedLong(valueP);
                case OCTAL_RADIX, DEC_RADIX, HEX_RADIX -> Long.parseLong(valueP, radixValue);
                default -> throw new IllegalArgumentException("Invalid radix: [" + radix + "]");
            };
        }
        return null;
    }

    private static int parseBinaryStringAsSignedInteger(String binaryString) {
        if (binaryString.length() != 32) {
            // Pad the binary string to 64 bits if it is not already
            binaryString = String.format("%32s", binaryString).replace(' ', '0');
        }

        // If the MSB is 1, the number is negative in two's complement
        if (binaryString.charAt(0) == '1') {
            // Calculate the two's complement
            String invertedBinaryString = invertBinaryString(binaryString);
            int positiveValue = Integer.parseInt(invertedBinaryString, MIN_RADIX) + 1;
            return -positiveValue;
        } else {
            return Integer.parseInt(binaryString, MIN_RADIX);
        }
    }

    private static long parseBinaryStringAsSignedLong(String binaryString) {
        if (binaryString.length() != 64) {
            // Pad the binary string to 64 bits if it is not already
            binaryString = String.format("%64s", binaryString).replace(' ', '0');
        }

        // If the MSB is 1, the number is negative in two's complement
        if (binaryString.charAt(0) == '1') {
            // Calculate the two's complement
            String invertedBinaryString = invertBinaryString(binaryString);
            long positiveValue = Long.parseLong(invertedBinaryString, MIN_RADIX) + 1;
            return -positiveValue;
        } else {
            return Long.parseLong(binaryString, MIN_RADIX);
        }
    }

    private static String invertBinaryString(String binaryString) {
        StringBuilder invertedString = new StringBuilder();
        for (char bit : binaryString.toCharArray()) {
            invertedString.append(bit == '0' ? '1' : '0');
        }
        return invertedString.toString();
    }

    private static int getRadix10_16(String value) {
        int radix = isDecimal(value) > 0 ? DEC_RADIX : isHexadecimal(value);
        if (radix > 0) {
            return radix;
        } else {
            throw new NumberFormatException("Value: \"" + value + "\" is not numeric or hexDecimal format!");
        }
    }

    public static Float parseFloat(String value) {
        return parseFloat(value, ZERO_RADIX);
    }

    public static Float parseFloat(String value, int radix) {
        String valueP = prepareNumberString(value, true);
        if (valueP != null) {
            return parseFloatFromString(value, valueP, radix);
        }
        return null;
    }

    private static Float parseFloatFromString(String value, String valueP, int radix) {
        int radixValue = isValidStringAndRadix(valueP, radix, value);
        if (radixValue == HEX_RADIX) {
            int bits = (int) Long.parseLong(valueP, HEX_RADIX);
            // Hex representation is a standard IEEE 754 float value (eg "0x41200000" for 10.0f).
            return Float.intBitsToFloat(bits);
        } else {
            return Float.parseFloat(value);
        }
    }

    public static Float parseHexIntLongToFloat(String value, boolean bigEndian) {
        String valueP = prepareNumberString(value, bigEndian);
        if (valueP != null) {
            int radixValue = isValidStringAndRadix(valueP, HEX_RADIX, value);
            if (radixValue == HEX_RADIX) {
                int bits = (int) Long.parseLong(valueP, HEX_RADIX);
                // If the length is not equal to 8 characters, we process it as an integer (eg "0x0A" for 10.0f).
                float floatValue = (float) bits;
                return Float.valueOf(floatValue);
            }
        }
        return null;
    }


    public static Double parseDouble(String value) {
        int radix = getRadix10_16(value);
        return parseDouble(value, radix);
    }

    public static Double parseDouble(String value, int radix) {
        return parseDouble(value, radix, true);
    }

    private static Double parseDouble(String value, int radix, boolean bigEndian) {
        String valueP = prepareNumberString(value, bigEndian);
        if (valueP != null) {
            int radixValue = isValidStringAndRadix(valueP, radix, value);
            if (radixValue == DEC_RADIX) {
                return Double.parseDouble(valueP);
            } else {
                long bits = Long.parseUnsignedLong(valueP, HEX_RADIX);
                return Double.longBitsToDouble(bits);
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

    public static Integer parseHexToInt(String value, boolean bigEndian) {
        return parseInt(value, HEX_RADIX, bigEndian);
    }

    public static long parseLittleEndianHexToLong(String hex) {
        return parseHexToLong(hex, false);
    }

    public static long parseBigEndianHexToLong(String hex) {
        return parseHexToLong(hex, true);
    }

    public static long parseHexToLong(String hex) {
        return parseHexToLong(hex, true);
    }

    public static Long parseHexToLong(String value, boolean bigEndian) {
        return parseLong(value, HEX_RADIX, bigEndian);
    }

    public static float parseLittleEndianHexToFloat(String hex) {
        return parseHexToFloat(hex, false);
    }

    public static float parseBigEndianHexToFloat(String hex) {
        return parseHexToFloat(hex, true);
    }

    public static float parseHexToFloat(String hex) {
        return parseHexToFloat(hex, true);
    }

    public static Float parseHexToFloat(String value, boolean bigEndian) {
        String valueP = prepareNumberString(value, bigEndian);
        if (valueP != null) {
            return parseFloatFromString(value, valueP, HEX_RADIX);
        }
        return null;
    }

    public static double parseLittleEndianHexToDouble(String hex) {
        return parseHexToDouble(hex, false);
    }

    public static double parseBigEndianHexToDouble(String hex) {
        return parseHexToDouble(hex, true);
    }

    public static double parseHexToDouble(String hex) {
        return parseHexToDouble(hex, true);
    }

    public static double parseHexToDouble(String value, boolean bigEndian) {
        return parseDouble(value, HEX_RADIX, bigEndian);
    }

    public static ExecutionArrayList<Byte> hexToBytes(ExecutionContext ctx, String value) {
        String hex = validateAndPrepareHex(value);
        byte[] data = hexToBytes(hex);
        return bytesToExecutionArrayList(ctx, data);
    }

    public static byte[] hexToBytesArray(String value) {
        String hex = validateAndPrepareHex(value);
        return hexToBytes(hex);
    }

    public static List<Integer> printUnsignedBytes(ExecutionContext ctx, List<Byte> byteArray) {
        ExecutionArrayList<Integer> data = new ExecutionArrayList<>(ctx);
        for (Byte b : byteArray) {
            // Convert signed byte to unsigned integer
            int unsignedByte = Byte.toUnsignedInt(b);
            data.add(unsignedByte);
        }
        return data;
    }

    public static String intToHex(Integer i) {
        return prepareNumberHexString(i.longValue(), true, false, HEX_LEN_MIN, HEX_LEN_INT_MAX);
    }

    public static String intToHex(Integer i, boolean bigEndian) {
        return prepareNumberHexString(i.longValue(), bigEndian, false, HEX_LEN_MIN, HEX_LEN_INT_MAX);
    }

    public static String intToHex(Integer i, boolean bigEndian, boolean pref) {
        return prepareNumberHexString(i.longValue(), bigEndian, pref, HEX_LEN_MIN, HEX_LEN_INT_MAX);
    }

    public static String intToHex(Integer i, boolean bigEndian, boolean pref, int len) {
        return prepareNumberHexString(i.longValue(), bigEndian, pref, len, HEX_LEN_INT_MAX);
    }

    public static String longToHex(Long l) {
        return prepareNumberHexString(l, true, false, HEX_LEN_MIN, HEX_LEN_LONG_MAX);
    }

    public static String longToHex(Long l, boolean bigEndian) {
        return prepareNumberHexString(l, bigEndian, false, HEX_LEN_MIN, HEX_LEN_LONG_MAX);
    }

    public static String longToHex(Long l, boolean bigEndian, boolean pref) {
        return prepareNumberHexString(l, bigEndian, pref, HEX_LEN_MIN, HEX_LEN_LONG_MAX);
    }

    public static String longToHex(Long l, boolean bigEndian, boolean pref, int len) {
        return prepareNumberHexString(l, bigEndian, pref, len, HEX_LEN_LONG_MAX);
    }

    public static String intLongToRadixString(Long number) {
        return intLongToRadixString(number, DEC_RADIX);
    }

    public static String intLongToRadixString(Long number, int radix) {
        return intLongToRadixString(number, radix, true);
    }

    public static String intLongToRadixString(Long number, int radix, boolean bigEndian) {
        return intLongToRadixString(number, radix, bigEndian, false);
    }

    public static String intLongToRadixString(Long number, int radix, boolean bigEndian, boolean pref) {
        if (radix >= 25 && radix <= MAX_RADIX) {
            return Long.toString(number, radix);
        }
        return switch (radix) {
            case MIN_RADIX -> formatBinary(Long.toBinaryString(number));
            case OCTAL_RADIX -> Long.toOctalString(number);
            case DEC_RADIX -> Long.toString(number);
            case HEX_RADIX -> prepareNumberHexString(number, bigEndian, pref, -1, -1);
            default -> throw new IllegalArgumentException("Invalid radix: [" + radix + "]");
        };
    }

    private static Number compareIntLongValueMinMax(String valueP, int radix, Number maxValue, Number minValue) {
        boolean isInteger = maxValue.getClass().getSimpleName().equals("Integer");
        try {
            if (isInteger) {
                return Integer.parseInt(valueP, radix);
            } else {
                return Long.parseLong(valueP, radix);
            }
        } catch (NumberFormatException e) {
            BigInteger bi = new BigInteger(valueP, radix);
            long maxValueL = isInteger ? maxValue.longValue() : (long) maxValue;
            if (bi.compareTo(BigInteger.valueOf(maxValueL)) > 0) {
                throw new NumberFormatException("Value \"" + valueP + "\"is greater than the maximum " + maxValue.getClass().getSimpleName() + " value " + maxValue + " !");
            }
            long minValueL = isInteger ? minValue.longValue() : (long) minValue;
            if (bi.compareTo(BigInteger.valueOf(minValueL)) < 0) {
                throw new NumberFormatException("Value \"" + valueP + "\" is  less than the minimum " + minValue.getClass().getSimpleName() + " value " + minValue + " !");
            }
            throw new NumberFormatException(e.getMessage());
        }
    }

    private static String prepareNumberHexString(Long number, boolean bigEndian, boolean pref, int len, int hexLenMax) {
        String hex = Long.toHexString(number).toUpperCase();
        hexLenMax = hexLenMax < 0 ? hex.length() : hexLenMax;
        String hexWithoutZeroFF = removeLeadingZero_FF(hex, number, hexLenMax);
        hexWithoutZeroFF = bigEndian ? hexWithoutZeroFF : reverseHexStringByOrder(hexWithoutZeroFF);
        len = len == HEX_LEN_MIN ? hexWithoutZeroFF.length() : len;
        String result = hexWithoutZeroFF.substring(hexWithoutZeroFF.length() - len);
        return pref ? "0x" + result : result;
    }

    private static String removeLeadingZero_FF(String hex, Long number, int hexLenMax) {
        String hexWithoutZero = hex.replaceFirst("^0+(?!$)", ""); // Remove leading zeros except for the last one
        hexWithoutZero = hexWithoutZero.length() % 2 > 0 ? "0" + hexWithoutZero : hexWithoutZero;
        if (number >= 0) {
            return hexWithoutZero;
        } else {
            String hexWithoutZeroFF = hexWithoutZero.replaceFirst("^F+(?!$)", "");
            hexWithoutZeroFF = hexWithoutZeroFF.length() % 2 > 0 ? "F" + hexWithoutZeroFF : hexWithoutZeroFF;
            if (hexWithoutZeroFF.length() > hexLenMax) {
                return hexWithoutZeroFF.substring(hexWithoutZeroFF.length() - hexLenMax);
            } else if (hexWithoutZeroFF.length() == hexLenMax) {
                return hexWithoutZeroFF;
            } else {
                return "FF" + hexWithoutZeroFF;
            }
        }
    }

    public static String floatToHex(Float f) {
        return floatToHex(f, true);
    }

    public static String floatToHex(Float f, boolean bigEndian) {
        // Convert the float to its raw integer bits representation
        int bits = Float.floatToIntBits(f);

        // Format the integer bits as a hexadecimal string
        String result = String.format("0x%08X", bits);
        return bigEndian ? result : reverseHexStringByOrder(result);
    }

    public static String doubleToHex(Double d) {
        return doubleToHex(d, true);
    }

    public static String doubleToHex(Double d, boolean bigEndian) {
        long bits = Double.doubleToRawLongBits(d);

        // Format the integer bits as a hexadecimal string
        String result = String.format("0x%016X", bits);
        return bigEndian ? result : reverseHexStringByOrder(result);
    }

    public static String base64ToHex(String base64) {
        return bytesToHex(Base64.getDecoder().decode(base64));
    }

    public static String hexToBase64(String hex) {
        return bytesToBase64(hexToBytes(hex));
    }

    public static String bytesToBase64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static byte[] base64ToBytes(String input) {
        return Base64.getDecoder().decode(input);
    }

    public static ExecutionArrayList<Byte> base64ToBytesList(ExecutionContext ctx, String input) {
        byte[] bytes = Base64.getDecoder().decode(input);
        return bytesToExecutionArrayList(ctx, bytes);
    }

    public static int parseBytesToInt(List<Byte> data) {
        return parseBytesToInt(data, 0);
    }

    public static int parseBytesToInt(List<Byte> data, int offset) {
        return parseBytesToInt(data, offset, validateLength(data.size(), offset, BYTES_LEN_INT_MAX));
    }

    public static int parseBytesToInt(List<Byte> data, int offset, int length) {
        return parseBytesToInt(data, offset, length, true);
    }

    public static int parseBytesToInt(List<Byte> data, int offset, int length, boolean bigEndian) {
        return parseBytesToInt(Bytes.toArray(data), offset, length, bigEndian);
    }

    public static int parseBytesToInt(byte[] data) {
        return parseBytesToInt(data, 0);
    }

    public static int parseBytesToInt(byte[] data, int offset) {
        return parseBytesToInt(data, offset, validateLength(data.length, offset, BYTES_LEN_INT_MAX));
    }

    public static int parseBytesToInt(byte[] data, int offset, int length) {
        return parseBytesToInt(data, offset, length, true);
    }

    public static int parseBytesToInt(byte[] data, int offset, int length, boolean bigEndian) {
        validationNumberByLength(data, offset, length, BYTES_LEN_INT_MAX);
        var bb = ByteBuffer.allocate(4);
        if (!bigEndian) {
            bb.order(ByteOrder.LITTLE_ENDIAN);
        }
        bb.position(bigEndian ? 4 - length : 0);
        bb.put(data, offset, length);
        bb.position(0);
        return bb.getInt();
    }

    public static long parseBytesToUnsignedInt(byte[] data) {
        return parseBytesToUnsignedInt(data, 0);
    }

    public static long parseBytesToUnsignedInt(byte[] data, int offset) {
        return parseBytesToUnsignedInt(data, offset, validateLength(data.length, offset, BYTES_LEN_INT_MAX));
    }

    public static long parseBytesToUnsignedInt(byte[] data, int offset, int length) {
        return parseBytesToUnsignedInt(data, offset, length, true);
    }

    public static long parseBytesToUnsignedInt(byte[] data, int offset, int length, boolean bigEndian) {
        validationNumberByLength(data, offset, length, BYTES_LEN_INT_MAX);

        ByteBuffer bb = ByteBuffer.allocate(8);
        if (!bigEndian) {
            bb.order(ByteOrder.LITTLE_ENDIAN);
        }
        bb.position(bigEndian ? 8 - length : 0);
        bb.put(data, offset, length);
        bb.position(0);

        return bb.getLong();
    }

    public static long parseBytesToUnsignedInt(List<Byte> data) {
        return parseBytesToUnsignedInt(data, 0);
    }

    public static long parseBytesToUnsignedInt(List<Byte> data, int offset) {
        return parseBytesToUnsignedInt(data, offset, validateLength(data.size(), offset, BYTES_LEN_INT_MAX));
    }

    public static long parseBytesToUnsignedInt(List<Byte> data, int offset, int length) {
        return parseBytesToUnsignedInt(data, offset, length, true);
    }

    public static long parseBytesToUnsignedInt(List<Byte> data, int offset, int length, boolean bigEndian) {
        return parseBytesToUnsignedInt(Bytes.toArray(data), offset, length, bigEndian);
    }

    public static long parseBytesToLong(List<Byte> data) {
        return parseBytesToLong(data, 0);
    }

    public static long parseBytesToLong(List<Byte> data, int offset) {
        return parseBytesToLong(data, offset, validateLength(data.size(), offset, BYTES_LEN_LONG_MAX));
    }

    public static long parseBytesToLong(List<Byte> data, int offset, int length) {
        return parseBytesToLong(data, offset, length, true);
    }

    public static long parseBytesToLong(List<Byte> data, int offset, int length, boolean bigEndian) {
        return parseBytesToLong(Bytes.toArray(data), offset, length, bigEndian);
    }

    public static long parseBytesToLong(byte[] data) {
        return parseBytesToLong(data, 0);
    }

    public static long parseBytesToLong(byte[] data, int offset) {
        return parseBytesToLong(data, offset, validateLength(data.length, offset, BYTES_LEN_LONG_MAX));
    }

    public static long parseBytesToLong(byte[] data, int offset, int length) {
        return parseBytesToLong(data, offset, length, true);
    }

    public static long parseBytesToLong(byte[] data, int offset, int length, boolean bigEndian) {
        validationNumberByLength(data, offset, length, BYTES_LEN_LONG_MAX);
        var bb = ByteBuffer.allocate(BYTES_LEN_LONG_MAX);
        if (!bigEndian) {
            bb.order(ByteOrder.LITTLE_ENDIAN);
        }
        bb.position(bigEndian ? BYTES_LEN_LONG_MAX - length : 0);
        bb.put(data, offset, length);
        bb.position(0);
        return bb.getLong();
    }

    public static float parseBytesToFloat(List data) {
        return parseBytesToFloat(data, 0);
    }

    public static float parseBytesToFloat(List data, int offset) {
        return parseBytesToFloat(data, offset, validateLength(data.size(), offset, BYTES_LEN_INT_MAX));
    }

    public static float parseBytesToFloat(List data, int offset, int length) {
        return parseBytesToFloat(data, offset, length, true);
    }

    public static float parseBytesToFloat(List data, int offset, int length, boolean bigEndian) {
        return parseBytesToFloat(Bytes.toArray(data), offset, length, bigEndian);
    }

    public static float parseBytesToFloat(byte[] data) {
        return parseBytesToFloat(data, 0);
    }

    public static float parseBytesToFloat(byte[] data, int offset) {
        return parseBytesToFloat(data, offset, validateLength(data.length, offset, BYTES_LEN_INT_MAX));
    }

    public static float parseBytesToFloat(byte[] data, int offset, int length) {
        return parseBytesToFloat(data, offset, length, true);
    }

    public static float parseBytesToFloat(byte[] data, int offset, int length, boolean bigEndian) {
        var bb = ByteBuffer.allocate(BYTES_LEN_INT_MAX);
        if (!bigEndian) {
            bb.order(ByteOrder.LITTLE_ENDIAN);
        }
        bb.position(bigEndian ? BYTES_LEN_INT_MAX - length : 0);
        bb.put(data, offset, length);
        bb.position(0);
        float floatValue = bb.getFloat();
        if (Float.isNaN(floatValue)) {
            throw new NumberFormatException("byte[] 0x" + bytesToHex(data) + " is a Not-a-Number (NaN) value");
        }
        return floatValue;
    }

    public static float parseBytesIntToFloat(List data) {
        return parseBytesIntToFloat(data, 0);
    }

    public static float parseBytesIntToFloat(List data, int offset) {
        return parseBytesIntToFloat(data, offset, validateLength(data.size(), offset, BYTES_LEN_INT_MAX));
    }

    public static float parseBytesIntToFloat(List data, int offset, int length) {
        return parseBytesIntToFloat(data, offset, length, true);
    }

    public static float parseBytesIntToFloat(List data, int offset, int length, boolean bigEndian) {
        return parseBytesIntToFloat(Bytes.toArray(data), offset, length, bigEndian);
    }

    public static float parseBytesIntToFloat(byte[] data) {
        return parseBytesIntToFloat(data, 0);
    }

    public static float parseBytesIntToFloat(byte[] data, int offset) {
        return parseBytesIntToFloat(data, offset, validateLength(data.length, offset, BYTES_LEN_INT_MAX));
    }

    public static float parseBytesIntToFloat(byte[] data, int offset, int length) {
        return parseBytesIntToFloat(data, offset, length, true);
    }

    public static float parseBytesIntToFloat(byte[] data, int offset, int length, boolean bigEndian) {
        byte[] bytesToNumber = prepareBytesToNumber(data, offset, length, bigEndian, BYTES_LEN_INT_MAX);
        long longValue = parseBytesToLong(bytesToNumber, 0, length);
        BigDecimal bigDecimalValue = new BigDecimal(longValue);
        return bigDecimalValue.floatValue();
    }

    public static double parseBytesToDouble(List data) {
        return parseBytesToDouble(data, 0);
    }

    public static double parseBytesToDouble(List data, int offset) {
        return parseBytesToDouble(data, offset, validateLength(data.size(), offset, BYTES_LEN_LONG_MAX));
    }

    public static double parseBytesToDouble(List data, int offset, int length) {
        return parseBytesToDouble(data, offset, length, true);
    }

    public static double parseBytesToDouble(List data, int offset, int length, boolean bigEndian) {
        return parseBytesToDouble(Bytes.toArray(data), offset, length, bigEndian);
    }

    public static double parseBytesToDouble(byte[] data) {
        return parseBytesToDouble(data, 0);
    }

    public static double parseBytesToDouble(byte[] data, int offset) {
        return parseBytesToDouble(data, offset, validateLength(data.length, offset, BYTES_LEN_LONG_MAX));
    }

    public static double parseBytesToDouble(byte[] data, int offset, int length) {
        return parseBytesToDouble(data, offset, length, true);
    }

    public static double parseBytesToDouble(byte[] data, int offset, int length, boolean bigEndian) {
        var bb = ByteBuffer.allocate(BYTES_LEN_LONG_MAX);
        if (!bigEndian) {
            bb.order(ByteOrder.LITTLE_ENDIAN);
        }
        bb.position(bigEndian ? BYTES_LEN_LONG_MAX - length : 0);
        bb.put(data, offset, length);
        bb.position(0);
        double doubleValue = bb.getDouble();
        if (Double.isNaN(doubleValue)) {
            throw new NumberFormatException("byte[] 0x" + bytesToHex(data) + " is a Not-a-Number (NaN) value");
        }
        return doubleValue;
    }

    public static double parseBytesLongToDouble(List data) {
        return parseBytesLongToDouble(data, 0);
    }

    public static double parseBytesLongToDouble(List data, int offset) {
        return parseBytesLongToDouble(data, offset, validateLength(data.size(), offset, BYTES_LEN_LONG_MAX));
    }

    public static double parseBytesLongToDouble(List data, int offset, int length) {
        return parseBytesLongToDouble(data, offset, length, true);
    }

    public static double parseBytesLongToDouble(List data, int offset, int length, boolean bigEndian) {
        return parseBytesLongToDouble(Bytes.toArray(data), offset, length, bigEndian);
    }

    public static double parseBytesLongToDouble(byte[] data) {
        return parseBytesLongToDouble(data, 0);
    }

    public static double parseBytesLongToDouble(byte[] data, int offset) {
        return parseBytesLongToDouble(data, offset, validateLength(data.length, offset, BYTES_LEN_LONG_MAX));
    }

    public static double parseBytesLongToDouble(byte[] data, int offset, int length) {
        return parseBytesLongToDouble(data, offset, length, true);
    }

    public static double parseBytesLongToDouble(byte[] data, int offset, int length, boolean bigEndian) {
        byte[] bytesToNumber = prepareBytesToNumber(data, offset, length, bigEndian, BYTES_LEN_LONG_MAX);
        BigInteger bigInt = new BigInteger(1, bytesToNumber);
        BigDecimal bigDecimalValue = new BigDecimal(bigInt);
        return bigDecimalValue.doubleValue();
    }

    private static byte[] prepareBytesToNumber(byte[] data, int offset, int length, boolean bigEndian,
                                               int bytesLenMax) {
        validationNumberByLength(data, offset, length, bytesLenMax);
        byte[] dataBytesArray = Arrays.copyOfRange(data, offset, (offset + length));
        if (!bigEndian) {
            ArrayUtils.reverse(dataBytesArray);
        }
        return dataBytesArray;
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

    public static String encodeURI(String uri) {
        String encoded = URLEncoder.encode(uri, StandardCharsets.UTF_8);
        for (var entry : mdnEncodingReplacements.entrySet()) {
            encoded = encoded.replaceAll(entry.getKey(), entry.getValue());
        }
        return encoded;
    }

    public static String decodeURI(String uri) {
        ArrayList<String> allKeys = new ArrayList<>(mdnEncodingReplacements.keySet());
        Collections.reverse(allKeys);
        for (String strKey : allKeys) {
            uri = uri.replaceAll(mdnEncodingReplacements.get(strKey), strKey);
        }
        return URLDecoder.decode(uri, StandardCharsets.UTF_8);
    }

    public static void raiseError(String message) {
        throw new RuntimeException(message);
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
                String key = path.substring(path.lastIndexOf('.') + 1);
                if (StringUtils.isNumeric(key)) {
                    int pos = path.length();
                    for (int i = 0; i < 2; i++) {
                        pos = path.lastIndexOf('.', pos - 1);
                    }
                    key = path.substring(pos + 1);
                }
                map.put(key, json);
            }
        }
    }

    private static String prepareNumberString(String value, boolean bigEndian) {
        if (StringUtils.isNotBlank(value)) {
            value = value.trim();
            value = value.replace("0x", "");
            value = value.replace("0X", "");
            value = value.replace(",", ".");
            return bigEndian ? value : reverseHexStringByOrder(value);
        }
        return null;
    }

    private static int isValidStringAndRadix(String valueP, int radix, String value) {
        int radixValue;
        if (radix == 0) {
            radixValue = getRadix10_16(valueP);
        } else if (radix >= 25 && radix <= MAX_RADIX) {
            return radix;
        } else {
            radixValue = switch (radix) {
                case MIN_RADIX -> isBinary(valueP);
                case OCTAL_RADIX -> isOctal(valueP);
                case DEC_RADIX -> isDecimal(valueP);
                case HEX_RADIX -> isHexadecimal(valueP);
                default -> throw new IllegalArgumentException("Invalid radix: [" + radix + "]");

            };
        }

        if (radixValue > 0) {
            if (value.startsWith("0x")) radixValue = HEX_RADIX;
            if (radixValue == HEX_RADIX) {
                valueP = value.startsWith("-") ? value.substring(1) : value;
                if (valueP.length() % 2 > 0) {
                    throw new NumberFormatException("The hexadecimal value: \"" + value + "\" must be of even length, or if the decimal value must be a number!");
                }
            }
            return radixValue;
        } else {
            if (radix > 0) {
                throw new NumberFormatException("Failed radix [" + radix + "] for value: \"" + value + "\", must be [" + radixValue + "] !");
            } else {
                throw new NumberFormatException("Invalid \"" + value + "\". It is not numeric or hexadecimal format!");
            }
        }
    }

    public static int isBinary(String str) {
        if (str == null || str.isEmpty()) {
            return -1;
        }
        return str.matches("[01]+") ? MIN_RADIX : -1;
    }

    public static int isOctal(String str) {
        if (str == null || str.isEmpty()) {
            return -1;
        }
        return str.matches("[0-7]+") ? OCTAL_RADIX : -1;
    }

    public static int isDecimal(String str) {
        if (str == null || str.isEmpty()) {
            return -1;
        }
        return str.matches("[+-]?\\d+(\\.\\d+)?([eE][+-]?\\d+)?") ? DEC_RADIX : -1;
    }

    public static int isHexadecimal(String str) {
        if (str == null || str.isEmpty()) {
            return -1;
        }
        return str.matches("^-?(0[xX])?[0-9a-fA-F]+$") ? HEX_RADIX : -1;
    }

    public static ExecutionArrayList<Byte> bytesToExecutionArrayList(ExecutionContext ctx, byte[] byteArray) {
        List<Byte> byteList = new ArrayList<>();
        for (byte b : byteArray) {
            byteList.add(b);
        }
        ExecutionArrayList<Byte> list = new ExecutionArrayList(byteList, ctx);
        return list;
    }

    public static String padStart(String str, int targetLength, char padString) {
        while (str.length() < targetLength) {
            str = padString + str;
        }
        return str;
    }

    public static String padEnd(String str, int targetLength, char padString) {
        while (str.length() < targetLength) {
            str = str + padString;
        }
        return str;
    }

    public static byte[] parseByteToBinaryArray(byte byteValue) {
        return parseByteToBinaryArray(byteValue, BIN_LEN_MAX);
    }

    public static byte[] parseByteToBinaryArray(byte byteValue, int binLength) {
        return parseByteToBinaryArray(byteValue, binLength, true);
    }

    /**
     * bigEndian = true
     * Writes the bit value to the appropriate location in the bins array, starting at the end of the array,
     * to ensure proper alignment (highest bit to low end).
     */
    public static byte[] parseByteToBinaryArray(byte byteValue, int binLength, boolean bigEndian) {
        byte[] bins = new byte[binLength];
        for (int i = 0; i < binLength; i++) {
            if (bigEndian) {
                bins[binLength - 1 - i] = (byte) ((byteValue >> i) & 1);
            } else {
                bins[i] = (byte) ((byteValue >> i) & 1);
            }
        }
        return bins;
    }

    public static byte[] parseBytesToBinaryArray(List listValue) {
        return parseBytesToBinaryArray(listValue, listValue.size() * BIN_LEN_MAX);
    }

    public static byte[] parseBytesToBinaryArray(List listValue, int binLength) {
        return parseBytesToBinaryArray(Bytes.toArray(listValue), binLength);
    }

    public static byte[] parseBytesToBinaryArray(byte[] bytesValue) {
        return parseLongToBinaryArray(parseBytesToLong(bytesValue), bytesValue.length * BIN_LEN_MAX);
    }

    public static byte[] parseBytesToBinaryArray(byte[] bytesValue, int binLength) {
        return parseLongToBinaryArray(parseBytesToLong(bytesValue), binLength);
    }

    public static byte[] parseLongToBinaryArray(long longValue) {
        return parseLongToBinaryArray(longValue, BYTES_LEN_LONG_MAX * BIN_LEN_MAX);
    }

    /**
     * Writes the bit value to the appropriate location in the bins array, starting at the end of the array,
     * to ensure proper alignment (highest bit to low end).
     */
    public static byte[] parseLongToBinaryArray(long longValue, int binLength) {
        int len = Math.min(binLength, BYTES_LEN_LONG_MAX * BIN_LEN_MAX);
        byte[] bins = new byte[len];
        for (int i = 0; i < len; i++) {
            bins[len - 1 - i] = (byte) ((longValue >> i) & 1);
        }
        return bins;
    }

    public static int parseBinaryArrayToInt(List listValue) {
        return parseBinaryArrayToInt(listValue, 0);
    }

    public static int parseBinaryArrayToInt(List listValue, int offset) {
        return parseBinaryArrayToInt(listValue, offset, listValue.size());
    }

    public static int parseBinaryArrayToInt(List listValue, int offset, int length) {
        return parseBinaryArrayToInt(Bytes.toArray(listValue), offset, length);
    }

    public static int parseBinaryArrayToInt(byte[] bytesValue) {
        return parseBinaryArrayToInt(bytesValue, 0);
    }

    public static int parseBinaryArrayToInt(byte[] bytesValue, int offset) {
        return parseBinaryArrayToInt(bytesValue, offset, bytesValue.length);
    }

    public static int parseBinaryArrayToInt(byte[] bytesValue, int offset, int length) {
        int result = 0;
        int len = Math.min(length + offset, bytesValue.length);
        for (int i = offset; i < len; i++) {
            result = (result << 1) | (bytesValue[i] & 1);
        }

        // For the one byte (8 bit) only If the most significant bit (sign) is set, we convert the result into a negative number
        if ((bytesValue.length == BIN_LEN_MAX)
                && offset == 0 && bytesValue[0] == 1) {
            result -= (1 << (len - offset));
        }
        return result;
    }

    public static boolean isInsidePolygon(double latitude, double longitude, String perimeter) {
        return GeoUtil.contains(perimeter, new Coordinates(latitude, longitude));
    }

    public static boolean isInsideCircle(double latitude, double longitude, String perimeter) {
        JsonNode perimeterJson = JacksonUtil.toJsonNode(perimeter);
        double centerLatitude = Double.parseDouble(perimeterJson.get("latitude").asText());
        double centerLongitude = Double.parseDouble(perimeterJson.get("longitude").asText());
        double range = Double.parseDouble(perimeterJson.get("radius").asText());
        RangeUnit rangeUnit = perimeterJson.has("radiusUnit") ? RangeUnit.valueOf(perimeterJson.get("radiusUnit").asText()) : RangeUnit.METER;

        Coordinates entityCoordinates = new Coordinates(latitude, longitude);
        Coordinates perimeterCoordinates = new Coordinates(centerLatitude, centerLongitude);
        return range > GeoUtil.distance(entityCoordinates, perimeterCoordinates, rangeUnit);
    }

    public static boolean isMap(Object obj) {
        return obj instanceof Map;

    }

    public static boolean isList(Object obj) {
        return obj instanceof List;
    }

    public static boolean isArray(Object obj) {
        return obj != null && obj.getClass().isArray();
    }

    public static <E> Set<E> newSet(ExecutionContext ctx) {
        return new ExecutionLinkedHashSet<>(ctx);
    }

    public static <E> Set<E> toSet(ExecutionContext ctx, List<E> list) {
        Set<E> newSet = new LinkedHashSet<>(list);
        return new ExecutionLinkedHashSet<>(newSet, ctx);
    }

    public static boolean isSet(Object obj) {
        return obj instanceof Set;
    }

    private static byte isValidIntegerToByte(Integer val) {
        if (val > 255 || val < -128) {
            throw new NumberFormatException("The value '" + val + "' could not be correctly converted to a byte. " +
                    "Integer to byte conversion requires the use of only 8 bits (with a range of min/max = -128/255)!");
        } else {
            return val.byteValue();
        }
    }

    private static String reverseHexStringByOrder(String value) {
        if (value.startsWith("-")) {
            throw new IllegalArgumentException("The hexadecimal string must be without a negative sign.");
        }
        boolean isHexPref = value.startsWith("0x");
        String hex = isHexPref ? value.substring(2) : value;
        if (hex.length() % 2 > 0) {
            throw new IllegalArgumentException("The hexadecimal string must be even-length.");
        }
        // Split the hex string into bytes (2 characters each)
        StringBuilder reversedHex = new StringBuilder(BYTES_LEN_LONG_MAX);
        for (int i = hex.length() - 2; i >= 0; i -= 2) {
            reversedHex.append(hex, i, i + 2);
        }
        String result = reversedHex.toString();
        return isHexPref ? "0x" + result : result;
    }

    private static void validationNumberByLength(byte[] data, int offset, int length, int bytesLenMax) {
        if (offset > data.length) {
            throw new IllegalArgumentException("Offset: " + offset + " is out of bounds for array with length: " + data.length + "!");
        }

        if (offset + length > data.length) {
            throw new IllegalArgumentException("Offset: " + offset + " and Length: " + length + " is out of bounds for array with length: " + data.length + "!");
        }

        if (length > bytesLenMax) {
            throw new IllegalArgumentException("Length: " + length + " is too large. Maximum " + bytesLenMax + " bytes is allowed!");
        }
    }

    private static int validateLength(int dataLength, int offset, int bytesLenMax) {
        return (dataLength < offset) ? dataLength : Math.min((dataLength - offset), bytesLenMax);
    }

    private static String formatBinary(String binaryString) {
        int format = binaryString.length() < 8 ? 8 :
                binaryString.length() < 16 ? 16 :
                        binaryString.length() < 32 ? 32 :
                                binaryString.length() < 64 ? 64 : 0;
        return format == 0 ? binaryString : String.format("%" + format + "s", binaryString).replace(' ', '0');
    }

    private static byte[] hexToBytes(String hex) {
        byte[] data = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length(); i += 2) {
            // Extract two characters from the hex string
            String byteString = hex.substring(i, i + 2);
            // Parse the hex string to a byte
            byte byteValue = (byte) Integer.parseInt(byteString, HEX_RADIX);
            // Add the byte to the ArrayList
            data[i / 2] = byteValue;
        }
        return data;
    }

    private static String validateAndPrepareHex(String value) {
        String hex = prepareNumberString(value, true);
        if (hex == null) {
            throw new IllegalArgumentException("Hex string must be not empty!");
        }
        int len = hex.length();
        if (len % 2 > 0) {
            throw new IllegalArgumentException("Hex string must be even-length.");
        }
        int radix = isHexadecimal(value);
        if (radix != HEX_RADIX) {
            throw new NumberFormatException("Value: \"" + value + "\" is not numeric or hexDecimal format!");
        }
        return hex;
    }

}

