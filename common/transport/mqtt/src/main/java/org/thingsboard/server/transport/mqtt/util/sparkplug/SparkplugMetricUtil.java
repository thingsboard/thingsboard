/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.transport.mqtt.util.sparkplug;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.ser.std.FileSerializer;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.mqtt.SparkplugBProto;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.thingsboard.common.util.JacksonUtil.newArrayNode;

/**
 * Provides utility methods for SparkplugB MQTT Payload Metric.
 */
@Slf4j
public class SparkplugMetricUtil {

    public static Optional<TransportProtos.KeyValueProto> fromSparkplugBMetricToKeyValueProto(String key, SparkplugBProto.Payload.Metric protoMetric) throws ThingsboardException {
        // Check if the null flag has been set indicating that the value is null
        if (protoMetric.getIsNull()) {
            return Optional.empty();
        }
        // Otherwise convert the value based on the type
        int metricType = protoMetric.getDatatype();
        TransportProtos.KeyValueProto.Builder builderProto = TransportProtos.KeyValueProto.newBuilder();
        ArrayNode nodeArray = newArrayNode();
        MetricDataType metricDataType = MetricDataType.fromInteger(metricType);
        try {
            switch (metricDataType) {
                case Boolean:
                    return Optional.of(builderProto.setKey(key).setType(TransportProtos.KeyValueType.BOOLEAN_V)
                            .setBoolV(protoMetric.getBooleanValue()).build());
                case DateTime:
                case Int64:
                    return Optional.of(builderProto.setKey(key).setType(TransportProtos.KeyValueType.LONG_V)
                            .setLongV(protoMetric.getLongValue()).build());
                case Float:
                    var f = new BigDecimal(String.valueOf(protoMetric.getFloatValue()));
                    return Optional.of(builderProto.setKey(key).setType(TransportProtos.KeyValueType.DOUBLE_V)
                            .setDoubleV(f.doubleValue()).build());
                case Double:
                    return Optional.of(builderProto.setKey(key).setType(TransportProtos.KeyValueType.LONG_V)
                            .setLongV(Double.valueOf(protoMetric.getDoubleValue()).longValue()).build());
                case Int8:
                case UInt8:
                case Int16:
                case Int32:
                case UInt16:
                    return Optional.of(builderProto.setKey(key).setType(TransportProtos.KeyValueType.LONG_V)
                            .setLongV(protoMetric.getIntValue()).build());
                case UInt32:
                case UInt64:
                    if (protoMetric.hasIntValue()) {
                        return Optional.of(builderProto.setKey(key).setType(TransportProtos.KeyValueType.LONG_V)
                                .setLongV(protoMetric.getIntValue()).build());
                    } else if (protoMetric.hasLongValue()) {
                        return Optional.of(builderProto.setKey(key).setType(TransportProtos.KeyValueType.LONG_V)
                                .setLongV(protoMetric.getLongValue()).build());
                    } else {
                        log.error("Invalid value for UInt32 datatype");
                        throw new ThingsboardException("Invalid value for " + MetricDataType.fromInteger(metricType).name() + " datatype " + metricType, ThingsboardErrorCode.INVALID_ARGUMENTS);
                    }
                case String:
                case Text:
                case UUID:
                    return Optional.of(builderProto.setKey(key).setType(TransportProtos.KeyValueType.STRING_V)
                            .setStringV(protoMetric.getStringValue()).build());
                // byte[]
                case BooleanArray:
                    ByteBuffer booleanByteBuffer = ByteBuffer.wrap(protoMetric.getBytesValue().toByteArray());
                    while (booleanByteBuffer.hasRemaining()) {
                        nodeArray.add(booleanByteBuffer.get() == (byte) 0 ? false : true);
                    }
                    return Optional.of(builderProto.setKey(key).setType(TransportProtos.KeyValueType.JSON_V)
                            .setJsonV(nodeArray.toString()).build());
                // byte[]
                case Bytes:
                case Int8Array:
                    ByteBuffer byteBuffer = ByteBuffer.wrap(protoMetric.getBytesValue().toByteArray());
                    while (byteBuffer.hasRemaining()) {
                        nodeArray.add(byteBuffer.get());
                    }
                    return Optional.of(builderProto.setKey(key).setType(TransportProtos.KeyValueType.JSON_V)
                            .setJsonV(nodeArray.toString()).build());
                // short[]
                case Int16Array:
                case UInt8Array:
                    ShortBuffer shortByteBuffer = ByteBuffer.wrap(protoMetric.getBytesValue().toByteArray())
                            .asShortBuffer();
                    while (shortByteBuffer.hasRemaining()) {
                        nodeArray.add(shortByteBuffer.get());
                    }
                    return Optional.of(builderProto.setKey(key).setType(TransportProtos.KeyValueType.JSON_V)
                            .setJsonV(nodeArray.toString()).build());
                // int[]
                case Int32Array:
                case UInt16Array:
                    IntBuffer intByteBuffer = ByteBuffer.wrap(protoMetric.getBytesValue().toByteArray())
                            .asIntBuffer();
                    while (intByteBuffer.hasRemaining()) {
                        nodeArray.add(intByteBuffer.get());
                    }
                    return Optional.of(builderProto.setKey(key).setType(TransportProtos.KeyValueType.JSON_V)
                            .setJsonV(nodeArray.toString()).build());
                // float[]
                case FloatArray:
                    FloatBuffer floatByteBuffer = ByteBuffer.wrap(protoMetric.getBytesValue().toByteArray())
                            .asFloatBuffer();
                    while (floatByteBuffer.hasRemaining()) {
                        nodeArray.add(floatByteBuffer.get());
                    }
                    return Optional.of(builderProto.setKey(key).setType(TransportProtos.KeyValueType.JSON_V)
                            .setJsonV(nodeArray.toString()).build());
                // double[]
                case DoubleArray:
                    DoubleBuffer doubleByteBuffer = ByteBuffer.wrap(protoMetric.getBytesValue().toByteArray())
                            .asDoubleBuffer();
                    while (doubleByteBuffer.hasRemaining()) {
                        nodeArray.add(doubleByteBuffer.get());
                    }
                    return Optional.of(builderProto.setKey(key).setType(TransportProtos.KeyValueType.JSON_V)
                            .setJsonV(nodeArray.toString()).build());
                // long[]
                case DateTimeArray:
                case Int64Array:
                case UInt64Array:
                case UInt32Array:
                    LongBuffer longByteBuffer = ByteBuffer.wrap(protoMetric.getBytesValue().toByteArray())
                            .asLongBuffer();
                    while (longByteBuffer.hasRemaining()) {
                        nodeArray.add(longByteBuffer.get());
                    }
                    return Optional.of(builderProto.setKey(key).setType(TransportProtos.KeyValueType.JSON_V)
                            .setJsonV(nodeArray.toString()).build());
                case StringArray:
                    ByteBuffer stringByteBuffer = ByteBuffer.wrap(protoMetric.getBytesValue().toByteArray());
                    final ByteArrayInputStream byteArrayInputStream =
                            new ByteArrayInputStream(stringByteBuffer.array());
                    final ObjectInputStream objectInputStream =
                            new ObjectInputStream(byteArrayInputStream);
                    final String[] stringArray = (String[]) objectInputStream.readObject();
                    objectInputStream.close();
                    for (String s : stringArray) {
                        nodeArray.add(s);
                    }
                    return Optional.of(builderProto.setKey(key).setType(TransportProtos.KeyValueType.JSON_V)
                            .setJsonV(nodeArray.toString()).build());
                case DataSet:
                case Template:
                case File:
                    //TODO
                    // Build the and create the DataSet
                    /**
                     SparkplugBProto.Payload.DataSet protoDataSet = protoMetric.getDatasetValue();
                     return new SparkplugBProto.Payload.DataSet.Builder(protoDataSet.getNumOfColumns()).addColumnNames(protoDataSet.getColumnsList())
                     .addTypes(convertDataSetDataTypes(protoDataSet.getTypesList()))
                     .addRows(convertDataSetRows(protoDataSet.getRowsList(), protoDataSet.getTypesList()))
                     .createDataSet();
                     return Optional.of(builderProto.setKey(key).setType(TransportProtos.KeyValueType.STRING_V)
                     .setStringV(protoDataSet.toString()).build());
                     **/
                    //TODO
                    // Build the and create the Template
                    /**
                     SparkplugBProto.Payload.Template protoTemplate = protoMetric.getTemplateValue();
                     return Optional.of(builderProto.setKey(key).setType(TransportProtos.KeyValueType.STRING_V)
                     .setStringV( protoTemplate.toString()).build());
                     **/
                    //TODO
                    // Build the and create the File
                    /**
                     String filename = protoMetric.getMetadata().getFileName();
                     return Optional.of(builderPrbyteValueoto.setKey(key + "_" + filename).setType(TransportProtos.KeyValueType.STRING_V)
                     .setStringV(Hex.encodeHexString((protoMetric.getBytesValue().toByteArray()))).build());
                     **/
                    return Optional.empty();
                case Unknown:
                default:
                    throw new ThingsboardException("Failed to decode: Unknown MetricDataType " + metricType, ThingsboardErrorCode.INVALID_ARGUMENTS);
            }
        } catch (Exception e) {
            log.error("", e);
            return Optional.empty();
        }
    }

    public static SparkplugBProto.Payload.Metric createMetric(Object value, long ts, String key, MetricDataType metricDataType) throws ThingsboardException {
        SparkplugBProto.Payload.Metric metric = SparkplugBProto.Payload.Metric.newBuilder()
                .setTimestamp(ts)
                .setName(key)
                .setDatatype(metricDataType.toIntValue())
                .build();
        switch (metricDataType) {
            case Int8:      //  (byte)
                return metric.toBuilder().setIntValue(((Byte) value).intValue()).build();
            case Int16:     // (short)
            case UInt8:
                return metric.toBuilder().setIntValue(((Short) value).intValue()).build();
            case UInt16:     //  (int)
            case Int32:
                return metric.toBuilder().setIntValue(((Integer) value).intValue()).build();
            case UInt32:     // (long)
            case Int64:
            case UInt64:
            case DateTime:
                return metric.toBuilder().setLongValue(((Long) value).longValue()).build();
            case Float:     // (float)
                return metric.toBuilder().setFloatValue(((Float) value).floatValue()).build();
            case Double:     // (double)
                return metric.toBuilder().setDoubleValue(((Double) value).doubleValue()).build();
            case Boolean:      // (boolean)
                return metric.toBuilder().setBooleanValue(((Boolean) value).booleanValue()).build();
            case String:        // String)
            case Text:
            case UUID:
                return metric.toBuilder().setStringValue((String) value).build();
            case Bytes:
                ByteString byteString = ByteString.copyFrom((byte[]) value);
                return metric.toBuilder().setBytesValue(byteString).build();
            case Int8Array:
                byte [] int8Array = byteArrayToByteArray((Byte[]) value);
                ByteString byteInt8Array = ByteString.copyFrom((int8Array));
                return metric.toBuilder().setBytesValue(byteInt8Array).build();
            case Int16Array:
            case UInt8Array:
                byte[] int16Array = shortArrayToByteArray((Short[]) value);
                ByteString byteInt16Array = ByteString.copyFrom((int16Array));
                return metric.toBuilder().setBytesValue(byteInt16Array).build();
            case Int32Array:
            case UInt16Array:
            case Int64Array:
            case UInt32Array:
            case UInt64Array:
            case DateTimeArray:
                if (value instanceof Integer[]) {
                    byte[] int32Array = integerArrayToByteArray((Integer[]) value);
                    ByteString byteInt32Array = ByteString.copyFrom((int32Array));
                    return metric.toBuilder().setBytesValue(byteInt32Array).build();
                } else {
                    byte[] int64Array = longArrayToByteArray((Long[]) value);
                    ByteString byteInt64Array = ByteString.copyFrom((int64Array));
                    return metric.toBuilder().setBytesValue(byteInt64Array).build();
                }
            case DoubleArray:
                byte[] doubleArray = doublArrayToByteArray((Double[]) value);
                ByteString byteDoubleArray = ByteString.copyFrom(doubleArray);
                return metric.toBuilder().setBytesValue(byteDoubleArray).build();
            case FloatArray:
                byte[] floatArray = floatArrayToByteArray((Float[]) value);
                ByteString byteFloatArray = ByteString.copyFrom(floatArray);
                return metric.toBuilder().setBytesValue(byteFloatArray).build();
            case BooleanArray:
                byte[] booleanArray = booleanArrayToByteArray((Boolean[]) value);
                ByteString byteBooleanArray = ByteString.copyFrom(booleanArray);
                return metric.toBuilder().setBytesValue(byteBooleanArray).build();
            case StringArray:
                byte[] stringArray = stringArrayToByteArray((String[]) value);
                ByteString byteStringArray = ByteString.copyFrom(stringArray);
                return metric.toBuilder().setBytesValue(byteStringArray).build();
            case DataSet:
                return metric.toBuilder().setDatasetValue((SparkplugBProto.Payload.DataSet) value).build();
            case File:
                SparkplugMetricUtil.File file = (SparkplugMetricUtil.File) value;
                ByteString byteFileString = ByteString.copyFrom(file.getBytes());
                return metric.toBuilder().setBytesValue(byteFileString).build();
            case Template:
                return metric.toBuilder().setTemplateValue((SparkplugBProto.Payload.Template) value).build();
            case Unknown:
                throw new ThingsboardException("Invalid value for MetricDataType " + metricDataType.name(), ThingsboardErrorCode.INVALID_ARGUMENTS);
        }
        return metric;
    }

    public static Optional<Object> validatedValueByTypeMetric(TransportProtos.KeyValueProto kv, MetricDataType metricDataType) throws ThingsboardException {
        if (kv.getTypeValue() <= 3) {
            return validatedValuePrimitiveByTypeMetric(kv, metricDataType);
        } else if (kv.getTypeValue() == 4) {
            JsonNode arrayNode = JacksonUtil.fromString(kv.getJsonV(), JsonNode.class);
            if (arrayNode.isArray()) {
                return validatedValueJsonByTypeMetric(kv.getJsonV(), metricDataType);
            }
        } else {
            throw new ThingsboardException("Invalid type KeyValueProto " + kv.toString() + " for MetricDataType " + metricDataType.name(), ThingsboardErrorCode.INVALID_ARGUMENTS);
        }
        return Optional.empty();
    }

    public static Optional<Object> validatedValuePrimitiveByTypeMetric(TransportProtos.KeyValueProto kv, MetricDataType metricDataType) throws ThingsboardException {
        Optional<String> valueOpt = getValueKvProtoPrimitive(kv);
        if (valueOpt.isPresent()) {
            try {
                switch (metricDataType) {
                    // int
                    case Int8:
                    case Int16:
                    case UInt8:
                    case UInt16:
                    case Int32:
                        Optional <Integer> boolInt8 = booleanStringToInt (valueOpt.get());
                        if(boolInt8.isPresent()) {
                            return Optional.of(boolInt8.get());
                        }
                        try {
                            return Optional.of(Integer.valueOf(valueOpt.get()));
                        } catch (NumberFormatException eInt) {
                            var i = new BigDecimal(valueOpt.get());
                            if (i.longValue() <= Integer.MAX_VALUE) {
                                return Optional.of(i.intValue());
                            }
                            throw new ThingsboardException("Invalid type value " + kv.toString() + " for MetricDataType "
                                    + metricDataType.name(), eInt, ThingsboardErrorCode.INVALID_ARGUMENTS);
                        }
                        // long
                    case UInt32:
                    case Int64:
                    case UInt64:
                    case DateTime:
                        Optional <Integer> boolInt64 = booleanStringToInt (valueOpt.get());
                        if(boolInt64.isPresent()) {
                            return Optional.of(Long.valueOf(boolInt64.get()));
                        }
                        var l = new BigDecimal(valueOpt.get());
                        return Optional.of(l.longValue());
                        // float
                    case Float:
                        Optional <Integer> boolFloat = booleanStringToInt (valueOpt.get());
                        if(boolFloat.isPresent()) {
                            var fb = new BigDecimal(boolFloat.get());
                            return Optional.of(fb.floatValue());
                        }
                        var f = new BigDecimal(valueOpt.get());
                        return Optional.of(f.floatValue());
                        // double
                    case Double:
                        Optional <Integer> boolDouble = booleanStringToInt (valueOpt.get());
                        if(boolDouble.isPresent()) {
                            return Optional.of(Double.valueOf(boolDouble.get()));
                        }
                        var dd = new BigDecimal(valueOpt.get());
                        return Optional.of(dd.doubleValue());
                    case Boolean:
                        if ("true".equals(valueOpt.get())) {
                            return Optional.of(true);
                        } else if ("false".equals(valueOpt.get())) {
                            return Optional.of(false);
                        } else {
                            Number number = NumberFormat.getInstance().parse(valueOpt.get());
                            if (StringUtils.isBlank(number.toString()) || "0".equals(number.toString())) { // ok 0
                                return Optional.of(false);
                            } else {
                                return Optional.of(true);
                            }
                        }
                    case String:
                    case Text:
                    case UUID:
                        return  Optional.of(valueOpt.get());
                }
            } catch (Exception e) {
                log.trace("Invalid type value [{}] for MetricDataType [{}] [{}]", kv, metricDataType.name(), e.getMessage());
                throw new ThingsboardException("Invalid type value " + kv.toString() + " for MetricDataType " + metricDataType.name(), e, ThingsboardErrorCode.INVALID_ARGUMENTS);
            }
        }
        return Optional.empty();
    }

    public static Optional<Object> validatedValueJsonByTypeMetric(String arrayNodeStr,  MetricDataType metricDataType) {
        try {
            Optional<Object> valueOpt;
            switch (metricDataType) {
                // byte[]
                case Bytes:
                    List<Byte> listBytes = JacksonUtil.fromString(arrayNodeStr, new TypeReference<>() {});
                    byte[] bytes = new byte[listBytes.size()];
                    for(int i = 0; i < listBytes.size(); i++) {
                        bytes[i] = listBytes.get(i).byteValue();
                    }
                    return Optional.of(bytes);
                    // Byte []
                case Int8Array:
                    List<Byte> listInt8Array = JacksonUtil.fromString(arrayNodeStr, new TypeReference<>() {});
                    Byte[] int8Arrays = listInt8Array.toArray(Byte[]::new) ;
                    return Optional.of(int8Arrays);
                // Short[]
                case Int16Array:
                case UInt8Array:
                    List<Short> listShorts = JacksonUtil.fromString(arrayNodeStr, new TypeReference<>() {});
                    return Optional.of(listShorts.toArray(Short[]::new));
                // Integer []
                case UInt16Array:
                case Int32Array:
                    List<Integer> listIntegers = JacksonUtil.fromString(arrayNodeStr, new TypeReference<>() {});
                    return Optional.of(listIntegers.toArray(Integer[]::new));
                // Long[]
                case UInt32Array:
                case Int64Array:
                case UInt64Array:
                case DateTimeArray:
                    List<Long> listLongs = JacksonUtil.fromString(arrayNodeStr, new TypeReference<>() {});
                    return Optional.of(listLongs.toArray(Long[]::new));
                // Double []
                case DoubleArray:
                    List<Double> listDoubles = JacksonUtil.fromString(arrayNodeStr, new TypeReference<>() {});
                    return Optional.of(listDoubles.toArray(Double[]::new));
                // Float[]
                case FloatArray:
                    List<Float> listFloats = JacksonUtil.fromString(arrayNodeStr, new TypeReference<>() {});
                    return Optional.of(listFloats.toArray(Float[]::new));
                // Boolean[]
                case BooleanArray:
                    List<Boolean> listBooleans = JacksonUtil.fromString(arrayNodeStr, new TypeReference<>() {});
                    return Optional.of(listBooleans.toArray(Boolean[]::new));
                case StringArray:
                    List<String> listStrings = JacksonUtil.fromString(arrayNodeStr, new TypeReference<>() {});
                    return Optional.of(listStrings.toArray(String[]::new));
                case DataSet:
                case File:
                case Template:
                    log.error("Invalid type value [{}] for MetricDataType [{}]", arrayNodeStr, metricDataType.name());
                    return Optional.empty();
                case Unknown:
                default:
                    log.error("Invalid MetricDataType [{}] type,  value [{}]", arrayNodeStr, metricDataType.name());
                    return Optional.empty();
            }
        } catch (Exception e) {
            log.error("Invalid type value [{}] for MetricDataType [{}] [{}]", arrayNodeStr, metricDataType.name(), e.getMessage());
            return Optional.empty();
        }
    }

    private static byte[] byteArrayToByteArray(Byte[] inputs) {
        ByteBuffer bb = ByteBuffer.allocate(inputs.length);
        for (Byte d : inputs) {
            bb.put(d);
        }
        return bb.array();
    }

    private static byte[] shortArrayToByteArray(Short[] inputs) {
        ByteBuffer bb = ByteBuffer.allocate(inputs.length * 2);
        for (Short d : inputs) {
            bb.putShort(d);
        }
        return bb.array();
    }

    private static byte[] integerArrayToByteArray(Integer[] inputs) {
        ByteBuffer bb = ByteBuffer.allocate(inputs.length * 4);
        for (Integer d : inputs) {
            bb.putInt(d);
        }
        return bb.array();
    }

    private static byte[] longArrayToByteArray(Long[] inputs) {
        ByteBuffer bb = ByteBuffer.allocate(inputs.length * 8);
        for (Long d : inputs) {
            bb.putLong(d);
        }
        return bb.array();
    }

    private static byte[] doublArrayToByteArray(Double[] inputs) {
        ByteBuffer bb = ByteBuffer.allocate(inputs.length * 8);
        for (Double d : inputs) {
            bb.putDouble(d);
        }
        return bb.array();
    }

    private static byte[] floatArrayToByteArray(Float[] inputs) throws ThingsboardException {
        ByteArrayOutputStream bas = new ByteArrayOutputStream();
        DataOutputStream ds = new DataOutputStream(bas);
        for (Float f : inputs) {
            try {
                ds.writeFloat(f);
            } catch (IOException e) {
                throw new ThingsboardException("Invalid value float ", ThingsboardErrorCode.INVALID_ARGUMENTS);
            }
        }
        return bas.toByteArray();
    }

    private static byte[] booleanArrayToByteArray(Boolean[] inputs) {
        byte[] toReturn = new byte[inputs.length];
        for (int entry = 0; entry < toReturn.length; entry++) {
            toReturn[entry] = (byte) (inputs[entry] ? 1 : 0);
        }
        return toReturn;
    }

    private static byte[] stringArrayToByteArray(String[] inputs) throws ThingsboardException {
        final ByteArrayOutputStream bas = new ByteArrayOutputStream();
        try {
            final ObjectOutputStream os = new ObjectOutputStream(bas);
            os.writeObject(inputs);
            os.flush();
            os.close();
        } catch (Exception e) {
            throw new ThingsboardException("Invalid value float ", ThingsboardErrorCode.INVALID_ARGUMENTS);
        }
        return bas.toByteArray();
    }

    private static Optional<String> getValueKvProtoPrimitive(TransportProtos.KeyValueProto kv) {
        if (kv.getTypeValue() == 0) {         // boolean
            return Optional.of(String.valueOf(kv.getBoolV()));
        } else if (kv.getTypeValue() == 1) {   // kvLong
            return Optional.of(String.valueOf(kv.getLongV()));
        } else if (kv.getTypeValue() == 2) {   // kvDouble/float
            return Optional.of(String.valueOf(kv.getDoubleV()));
        } else if (kv.getTypeValue() == 3) {   // kvString
            return Optional.of(kv.getStringV());
        } else {
            return Optional.empty();
        }
    }

    private static Optional<Integer> booleanStringToInt (String booleanStr) {
        if ("true".equals(booleanStr)) {
            return Optional.of(1);
        } else if ("false".equals(booleanStr)) {
            return Optional.of(0);
        } else {
            return Optional.empty();
        }
    }

    @JsonIgnoreProperties(
            value = {"fileName"})
    @JsonSerialize(
            using = FileSerializer.class)
    public class File {

        private String fileName;
        private byte[] bytes;

        /**
         * Default Constructor
         */
        public File() {
            super();
        }

        /**
         * Constructor
         *
         * @param fileName the full file name path
         * @param bytes    the array of bytes that represent the contents of the file
         */
        public File(String fileName, byte[] bytes) {
            super();
            this.fileName = fileName == null
                    ? null
                    : fileName.replace("/", System.getProperty("file.separator")).replace("\\",
                    System.getProperty("file.separator"));
            this.bytes = Arrays.copyOf(bytes, bytes.length);
        }

        /**
         * Gets the full filename path
         *
         * @return the full filename path
         */
        public String getFileName() {
            return fileName;
        }

        /**
         * Sets the full filename path
         *
         * @param fileName the full filename path
         */
        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        /**
         * Gets the bytes that represent the contents of the file
         *
         * @return the bytes that represent the contents of the file
         */
        public byte[] getBytes() {
            return bytes;
        }

        /**
         * Sets the bytes that represent the contents of the file
         *
         * @param bytes the bytes that represent the contents of the file
         */
        public void setBytes(byte[] bytes) {
            this.bytes = bytes;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("File [fileName=");
            builder.append(fileName);
            builder.append(", bytes=");
            builder.append(Arrays.toString(bytes));
            builder.append("]");
            return builder.toString();
        }
    }

}
