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
import com.fasterxml.jackson.databind.ser.std.FileSerializer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.mqtt.SparkplugBProto;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Provides utility methods for SparkplugB MQTT Payload Metric.
 */
@Slf4j
public class SparkplugMetricUtil {

    public static Optional<TransportProtos.KeyValueProto> getFromSparkplugBMetricToKeyValueProto(String key, SparkplugBProto.Payload.Metric protoMetric) throws ThingsboardException {
        // Check if the null flag has been set indicating that the value is null
        if (protoMetric.getIsNull()) {
            return null;
        }
        // Otherwise convert the value based on the type
        int metricType = protoMetric.getDatatype();
        switch (MetricDataType.fromInteger(metricType)) {
            case Boolean:
                return Optional.of(TransportProtos.KeyValueProto.newBuilder().setKey(key).setType(TransportProtos.KeyValueType.BOOLEAN_V)
                        .setBoolV(protoMetric.getBooleanValue()).build());
            case DateTime:
            case Int64:
                return Optional.of(TransportProtos.KeyValueProto.newBuilder().setKey(key).setType(TransportProtos.KeyValueType.LONG_V)
                        .setLongV(protoMetric.getLongValue()).build());
            case File:
                String filename = protoMetric.getMetadata().getFileName();
                return Optional.of(TransportProtos.KeyValueProto.newBuilder().setKey(key + "_" + filename).setType(TransportProtos.KeyValueType.STRING_V)
                        .setStringV(Hex.encodeHexString((protoMetric.getBytesValue().toByteArray()))).build());
            case Float:
                return Optional.of(TransportProtos.KeyValueProto.newBuilder().setKey(key).setType(TransportProtos.KeyValueType.LONG_V)
                        .setLongV((long) protoMetric.getFloatValue()).build());
            case Double:
                return Optional.of(TransportProtos.KeyValueProto.newBuilder().setKey(key).setType(TransportProtos.KeyValueType.DOUBLE_V)
                        .setDoubleV(protoMetric.getDoubleValue()).build());
            case Int8:
            case UInt8:
            case Int16:
            case Int32:
            case UInt16:
                return Optional.of(TransportProtos.KeyValueProto.newBuilder().setKey(key).setType(TransportProtos.KeyValueType.LONG_V)
                        .setLongV(protoMetric.getIntValue()).build());
            case UInt32:
                if (protoMetric.hasIntValue()) {
                    return Optional.of(TransportProtos.KeyValueProto.newBuilder().setKey(key).setType(TransportProtos.KeyValueType.LONG_V)
                            .setLongV(protoMetric.getIntValue()).build());
                } else if (protoMetric.hasLongValue()) {
                    return Optional.of(TransportProtos.KeyValueProto.newBuilder().setKey(key).setType(TransportProtos.KeyValueType.LONG_V)
                            .setLongV(protoMetric.getLongValue()).build());
                } else {
                    log.error("Invalid value for UInt32 datatype");
                    throw new ThingsboardException("Invalid value for UInt32 datatype " + metricType, ThingsboardErrorCode.INVALID_ARGUMENTS);
                }
            case UInt64:
                return Optional.of(TransportProtos.KeyValueProto.newBuilder().setKey(key).setType(TransportProtos.KeyValueType.DOUBLE_V)
                        .setDoubleV((new BigInteger(Long.toUnsignedString(protoMetric.getLongValue()))).longValue()).build());
            case String:
            case Text:
            case UUID:
                return Optional.of(TransportProtos.KeyValueProto.newBuilder().setKey(key).setType(TransportProtos.KeyValueType.STRING_V)
                        .setStringV(protoMetric.getStringValue()).build());
            case Bytes:
            case Int8Array:
            case Int16Array:
            case Int32Array:
            case Int64Array:
            case UInt8Array:
            case UInt16Array:
            case UInt32Array:
            case UInt64Array:
            case FloatArray:
            case DoubleArray:
            case BooleanArray:
                return Optional.of(TransportProtos.KeyValueProto.newBuilder().setKey(key).setType(TransportProtos.KeyValueType.STRING_V)
                        .setStringV(Hex.encodeHexString(protoMetric.getBytesValue().toByteArray())).build());
            case DataSet:
                SparkplugBProto.Payload.DataSet protoDataSet = protoMetric.getDatasetValue();
                //TODO
                // Build the and create the DataSet
                /**
                 return new SparkplugBProto.Payload.DataSet.Builder(protoDataSet.getNumOfColumns()).addColumnNames(protoDataSet.getColumnsList())
                 .addTypes(convertDataSetDataTypes(protoDataSet.getTypesList()))
                 .addRows(convertDataSetRows(protoDataSet.getRowsList(), protoDataSet.getTypesList()))
                 .createDataSet();
                 **/
                return Optional.of(TransportProtos.KeyValueProto.newBuilder().setKey(key).setType(TransportProtos.KeyValueType.STRING_V)
                        .setStringV(protoDataSet.toString()).build());
            case Template:
                //TODO
                // Build the and create the Template
                SparkplugBProto.Payload.Template protoTemplate = protoMetric.getTemplateValue();
                return Optional.of(TransportProtos.KeyValueProto.newBuilder().setKey(key).setType(TransportProtos.KeyValueType.STRING_V)
                        .setStringV( protoTemplate.toString()).build());
            case StringArray:
                ByteBuffer stringByteBuffer = ByteBuffer.wrap(protoMetric.getBytesValue().toByteArray());
                List<String> stringList = new ArrayList<>();
                stringByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
                StringBuilder sb = new StringBuilder();
                while (stringByteBuffer.hasRemaining()) {
                    byte b = stringByteBuffer.get();
                    if (b == (byte) 0) {
                        stringList.add(sb.toString());
                        sb = new StringBuilder();
                    } else {

                        sb.append((char) b);
                    }
                }
                String st =  StringUtils.join(stringList, "|");
                return Optional.of(TransportProtos.KeyValueProto.newBuilder().setKey(key).setType(TransportProtos.KeyValueType.STRING_V)
                        .setStringV(st).build());
            case DateTimeArray:
                ByteBuffer dateTimeByteBuffer = ByteBuffer.wrap(protoMetric.getBytesValue().toByteArray());
                List<Long> dateTimeList = new ArrayList<Long>();
                dateTimeByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
                while (dateTimeByteBuffer.hasRemaining()) {
                    long longValue = dateTimeByteBuffer.getLong();
                    dateTimeList.add(longValue);
                }
                Gson gson = new GsonBuilder().create();
                JsonArray dateTimeArray = gson.toJsonTree(dateTimeList).getAsJsonArray();
                return Optional.of(TransportProtos.KeyValueProto.newBuilder().setKey(key).setType(TransportProtos.KeyValueType.JSON_V)
                        .setStringV(dateTimeArray.toString()).build());
            case Unknown:
            default:
                throw new ThingsboardException("Failed to decode: Unknown MetricDataType " + metricType, ThingsboardErrorCode.INVALID_ARGUMENTS);
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
