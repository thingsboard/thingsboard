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
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.ser.std.FileSerializer;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
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
            case Int8:
            case Int16:
            case UInt8:
            case UInt16:
                int valueMetric = Integer.valueOf(String.valueOf(value));
                return metric.toBuilder().setIntValue(valueMetric).build();
            case Int32:
            case UInt32:
                if (value instanceof Long) {
                    return metric.toBuilder().setLongValue((long) value).build();
                } else {
                    return metric.toBuilder().setIntValue((int) value).build();
                }
            case Int64:
            case UInt64:
            case DateTime:
                return metric.toBuilder().setLongValue((long) value).build();
            case Float:
                return metric.toBuilder().setFloatValue((float) value).build();
            case Double:
                return metric.toBuilder().setDoubleValue((double) value).build();
            case Boolean:
                return metric.toBuilder().setBooleanValue((boolean) value).build();
            case String:
            case Text:
            case UUID:
                return metric.toBuilder().setStringValue((String) value).build();
            case Bytes:
            case Int8Array:
                ByteString byteString = ByteString.copyFrom((byte[]) value);
                return metric.toBuilder().setBytesValue(byteString).build();
            case Int16Array:
            case UInt8Array:
                byte[] int16Array = shortArrayToByteArray((short[]) value);
                ByteString byteInt16Array = ByteString.copyFrom((int16Array));
                return metric.toBuilder().setBytesValue(byteInt16Array).build();
            case Int32Array:
            case UInt16Array:
            case Int64Array:
            case UInt32Array:
            case UInt64Array:
            case DateTimeArray:
                if (value instanceof int[]) {
                    byte[] int32Array = integerArrayToByteArray((int[]) value);
                    ByteString byteInt32Array = ByteString.copyFrom((int32Array));
                    return metric.toBuilder().setBytesValue(byteInt32Array).build();
                } else {
                    byte[] int64Array = longArrayToByteArray((long[]) value);
                    ByteString byteInt64Array = ByteString.copyFrom((int64Array));
                    return metric.toBuilder().setBytesValue(byteInt64Array).build();
                }
            case DoubleArray:
                byte[] doubleArray = doublArrayToByteArray((double[]) value);
                ByteString byteDoubleArray = ByteString.copyFrom(doubleArray);
                return metric.toBuilder().setBytesValue(byteDoubleArray).build();
            case FloatArray:
                byte[] floatArray = floatArrayToByteArray((float[]) value);
                ByteString byteFloatArray = ByteString.copyFrom(floatArray);
                return metric.toBuilder().setBytesValue(byteFloatArray).build();
            case BooleanArray:
                byte[] booleanArray = booleanArrayToByteArray((boolean[]) value);
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

    public static Optional<Object> validatedValueByTypeMetric(TransportProtos.KeyValueProto kv, MetricDataType metricDataType) {
        if (kv.getTypeValue() <= 3) {
            return validatedValuePrimitiveByTypeMetric(kv, metricDataType);
        } else if (kv.getTypeValue() == 4) {
            return validatedValueJsonByTypeMetric(kv, metricDataType);
        } else {
            return Optional.empty();
        }
    }

    public static Optional<Object> validatedValuePrimitiveByTypeMetric(TransportProtos.KeyValueProto kv, MetricDataType metricDataType) {
        Optional<String> valueOpt = getValueKvProtoPrimitive(kv);
        if (valueOpt.isPresent()) {
            try {
                switch (metricDataType) {
                    // int
                    case Int8:
                    case Int16:
                    case UInt8:
                    case UInt16:
                        return  Optional.of(Integer.valueOf(valueOpt.get()));
                    // int/long
                    case Int32:
                    case UInt32:
                        try {
                            return Optional.of(Integer.valueOf(valueOpt.get()));
                        } catch (NumberFormatException e) {
                            return Optional.of(Long.valueOf(valueOpt.get()));
                        }
                        // long
                    case Int64:
                    case UInt64:
                    case DateTime:
                        return Optional.of(Long.valueOf(valueOpt.get()));
                        // float
                    case Float:
                        var f = new BigDecimal(valueOpt.get());
                        return Optional.of(f.floatValue());
                        // double
                    case Double:
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
                        if (kv.getTypeValue() == 4) {
                            return  Optional.of(valueOpt.get());
                        }
                        break;
                }
            } catch (Exception e) {
                log.error("Invalid type value [{}] for MetricDataType [{}] [{}]", kv, metricDataType.name(), e.getMessage());
                return Optional.empty();
            }
        }
        return Optional.empty();
    }


    public static Optional<Object> validatedValueJsonByTypeMetric(TransportProtos.KeyValueProto kv, MetricDataType metricDataType) {
//        try {
//            Optional<Object> valueOpt;
//            switch (metricDataType) {
//                // int
//                case Int8:
//                case Int16:
//                case UInt8:
//                case UInt16:
//                    valueOpt = getValueKvProtoPrimitive(kv);
//                    return valueOpt.isPresent() ? Optional.of(Integer.valueOf(String.valueOf(valueOpt.get()))) : valueOpt;
//                // int/long
//                case Int32:
//                case UInt32:
//                    valueOpt = getValueKvProtoPrimitive(kv);
//                    try {
//                        return Optional.of(Integer.valueOf(String.valueOf(valueOpt.get())));
//                    } catch (NumberFormatException e) {
//                        return Optional.of(Long.valueOf(String.valueOf(valueOpt.get())));
//                    }
//                    // long
//                case Int64:
//                case UInt64:
//                case DateTime:
//                    valueOpt = getValueKvProtoPrimitive(kv);
//                    return Optional.of(Long.valueOf(String.valueOf(valueOpt.get())));
//                // float
//                case Float:
//                    valueOpt = getValueKvProtoPrimitive(kv);
//                    var f = new BigDecimal(String.valueOf(kv.getDoubleV()));
//                    return Optional.of(f.floatValue());
//            }
//            break;
//            // double
//            case Double:
//                if (kv.getTypeValue() == 1) {
//                    return Optional.of(kv.getLongV());
//                }
//                break;
//            case Boolean:
//                if (kv.getTypeValue() == 0) { // ok 0
//                    return Optional.of(kv.getBoolV());
//                }
//                break;
//            case String:
//            case Text:
//            case UUID:
//                if (kv.getTypeValue() == 4) {
//                    return Optional.of(kv.getStringV());
//                }
//                break;
//            // byte[]
//            case Bytes:
//            case Int8Array:
//                if (kv.getTypeValue() == 5) {
////                        ByteString byteString = ByteString.copyFrom((byte[]) value);
////                        return metric.toBuilder().setBytesValue(byteString).build();
//                    return Optional.of(kv.getJsonV());
//                }
//                break;
//            // short[]
//            case Int16Array:
//            case UInt8Array:
//                if (kv.getTypeValue() == 5) {
////                        byte[] int16Array = shortArrayToByteArray((short[]) value);
////                        ByteString byteInt16Array = ByteString.copyFrom((int16Array));
//                    return Optional.of(kv.getJsonV());
//                }
//                break;
//            // int []
//            case UInt16Array:
//            case Int32Array:
//
//                // int[] / long[]
//            case UInt32Array:
//
//                // long[]
//            case Int64Array:
//            case UInt64Array:
//            case DateTimeArray:
//                if (kv.getTypeValue() == 5) {
////                        if (value instanceof int[]) {
////                            byte[] int32Array = integerArrayToByteArray((int[]) value);
////                            ByteString byteInt32Array = ByteString.copyFrom((int32Array));
////                            return metric.toBuilder().setBytesValue(byteInt32Array).build();
////                        } else {
////                            byte[] int64Array = longArrayToByteArray((long[]) value);
////                            ByteString byteInt64Array = ByteString.copyFrom((int64Array));
////                            return metric.toBuilder().setBytesValue(byteInt64Array).build();
////                        }
//                    return Optional.of(kv.getJsonV());
//                }
//                break;
//            // double []
//            case DoubleArray:
//                if (kv.getTypeValue() == 5) {
////                        byte[] doubleArray = doublArrayToByteArray((double[]) value);
////                        ByteString byteDoubleArray = ByteString.copyFrom(doubleArray);
////                        return metric.toBuilder().setBytesValue(byteDoubleArray).build();
//                    return Optional.of(kv.getJsonV());
//                }
//                break;
//            // float[]
//            case FloatArray:
//                if (kv.getTypeValue() == 5) {
////                        byte[] floatArray = floatArrayToByteArray((float[]) value);
////                        ByteString byteFloatArray = ByteString.copyFrom(floatArray);
////                        return metric.toBuilder().setBytesValue(byteFloatArray).build();
//                    return Optional.of(kv.getJsonV());
//                }
//                break;
//            // boolean[]
//            case BooleanArray:
//                if (kv.getTypeValue() == 5) {
////                        byte[] booleanArray = booleanArrayToByteArray((boolean[]) value);
////                        ByteString byteBooleanArray = ByteString.copyFrom(booleanArray);
////                        return metric.toBuilder().setBytesValue(byteBooleanArray).build();
//                    return Optional.of(kv.getJsonV());
//                }
//                break;
//            // String []
//            case StringArray:
//                if (kv.getTypeValue() == 5) {
////                        byte[] stringArray = stringArrayToByteArray((String[]) value);
////                        ByteString byteStringArray = ByteString.copyFrom(stringArray);
////                        return metric.toBuilder().setBytesValue(byteStringArray).build();
//                    return Optional.of(kv.getJsonV());
//                }
//                break;
//            case DataSet:
//            case File:
//            case Template:
//                log.error("Invalid type value [{}] for MetricDataType [{}]", kv, metricDataType.name());
//                return Optional.empty();
//            case Unknown:
//                log.error("Invalid MetricDataType [{}] type,  value [{}]", kv, metricDataType.name());
//                return Optional.empty();
//        } catch (Exception e) {
//            log.error("Invalid type value [{}] for MetricDataType [{}] [{}]", kv, metricDataType.name(), e.getMessage());
//            return Optional.empty();
//        }
        return Optional.empty();
    }


    private static byte[] shortArrayToByteArray(short[] inputs) {
        ByteBuffer bb = ByteBuffer.allocate(inputs.length * 2);
        for (short d : inputs) {
            bb.putShort(d);
        }
        return bb.array();
    }

    private static byte[] integerArrayToByteArray(int[] inputs) {
        ByteBuffer bb = ByteBuffer.allocate(inputs.length * 4);
        for (int d : inputs) {
            bb.putInt(d);
        }
        return bb.array();
    }

    private static byte[] longArrayToByteArray(long[] inputs) {
        ByteBuffer bb = ByteBuffer.allocate(inputs.length * 8);
        for (long d : inputs) {
            bb.putLong(d);
        }
        return bb.array();
    }

    private static byte[] doublArrayToByteArray(double[] inputs) {
        ByteBuffer bb = ByteBuffer.allocate(inputs.length * 8);
        for (double d : inputs) {
            bb.putDouble(d);
        }
        return bb.array();
    }

    private static byte[] floatArrayToByteArray(float[] inputs) throws ThingsboardException {
        ByteArrayOutputStream bas = new ByteArrayOutputStream();
        DataOutputStream ds = new DataOutputStream(bas);
        for (float f : inputs) {
            try {
                ds.writeFloat(f);
            } catch (IOException e) {
                throw new ThingsboardException("Invalid value float ", ThingsboardErrorCode.INVALID_ARGUMENTS);
            }
        }
        return bas.toByteArray();
    }

    private static byte[] booleanArrayToByteArray(boolean[] inputs) {
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
        } else if (kv.getTypeValue() == 2) {   // kvString
            return Optional.of(kv.getStringV());
        } else if (kv.getTypeValue() == 3) {   // kvDouble
            return Optional.of(String.valueOf(kv.getDoubleV()));
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
