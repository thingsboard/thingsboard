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
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.mqtt.SparkplugBProto;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.Optional;

import static org.thingsboard.common.util.JacksonUtil.newArrayNode;

/**
 * Provides utility methods for SparkplugB MQTT Payload Metric.
 */
@Slf4j
public class SparkplugMetricUtil {

    public static Optional<TransportProtos.KeyValueProto> getFromSparkplugBMetricToKeyValueProto(String key, SparkplugBProto.Payload.Metric protoMetric) throws ThingsboardException {
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
                    return Optional.of(builderProto.setKey(key).setType(TransportProtos.KeyValueType.DOUBLE_V)
                            .setDoubleV(protoMetric.getDoubleValue()).build());
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
                    while (booleanByteBuffer.hasRemaining()){
                        nodeArray.add(booleanByteBuffer.get() == (byte) 0 ? false : true);
                    }
                    return Optional.of(builderProto.setKey(key).setType(TransportProtos.KeyValueType.JSON_V)
                            .setJsonV(nodeArray.toString()).build());
                // byte[]
                case Bytes:
                case Int8Array:
                    ByteBuffer byteBuffer = ByteBuffer.wrap(protoMetric.getBytesValue().toByteArray());
                    while (byteBuffer.hasRemaining()){
                        nodeArray.add(byteBuffer.get());
                    }
                    return Optional.of(builderProto.setKey(key).setType(TransportProtos.KeyValueType.JSON_V)
                            .setJsonV(nodeArray.toString()).build());
                // short[]
                case Int16Array:
                case UInt8Array:
                    ShortBuffer shortByteBuffer = ByteBuffer.wrap(protoMetric.getBytesValue().toByteArray())
                            .asShortBuffer();
                    while (shortByteBuffer.hasRemaining()){
                        nodeArray.add(shortByteBuffer.get());
                    }
                    return Optional.of(builderProto.setKey(key).setType(TransportProtos.KeyValueType.JSON_V)
                            .setJsonV(nodeArray.toString()).build());
                // int[]
                case Int32Array:
                case UInt16Array:
                    IntBuffer intByteBuffer = ByteBuffer.wrap(protoMetric.getBytesValue().toByteArray())
                            .asIntBuffer();
                    while (intByteBuffer.hasRemaining()){
                        nodeArray.add(intByteBuffer.get());
                    }
                    return Optional.of(builderProto.setKey(key).setType(TransportProtos.KeyValueType.JSON_V)
                            .setJsonV(nodeArray.toString()).build());
                // float[]
                case FloatArray:
                    FloatBuffer floatByteBuffer = ByteBuffer.wrap(protoMetric.getBytesValue().toByteArray())
                            .asFloatBuffer();
                    while (floatByteBuffer.hasRemaining()){
                        nodeArray.add(floatByteBuffer.get());
                    }
                    return Optional.of(builderProto.setKey(key).setType(TransportProtos.KeyValueType.JSON_V)
                            .setJsonV(nodeArray.toString()).build());
                // double[]
                case DoubleArray:
                    DoubleBuffer doubleByteBuffer = ByteBuffer.wrap(protoMetric.getBytesValue().toByteArray())
                            .asDoubleBuffer();
                    while (doubleByteBuffer.hasRemaining()){
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
                    while (longByteBuffer.hasRemaining()){
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
                    for (String s: stringArray) {
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
        } catch (Exception e){
            log.error("", e);
            return  Optional.empty();
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
