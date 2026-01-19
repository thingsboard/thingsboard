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
package org.thingsboard.server.transport.mqtt.util.sparkplug;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.adaptor.AdaptorException;
import org.thingsboard.server.gen.transport.mqtt.SparkplugBProto;

import java.math.BigInteger;
import java.util.Date;

/**
 * Created by nickAS21 on 10.01.23
 */

@Slf4j
public enum MetricDataType {

    // Basic Types
    Int8(1, Byte.class),
    Int16(2, Short.class),
    Int32(3, Integer.class),
    Int64(4, Long.class),
    UInt8(5, Short.class),
    UInt16(6, Integer.class),
    UInt32(7, Long.class),
    UInt64(8, BigInteger.class),
    Float(9, Float.class),
    Double(10, Double.class),
    Boolean(11, Boolean.class),
    String(12, String.class),
    DateTime(13, Date.class),
    Text(14, String.class),

    // Custom Types for Metrics
    UUID(15, String.class),
    DataSet(16, SparkplugBProto.Payload.DataSet.class),
    Bytes(17, byte[].class),
    File(18, SparkplugMetricUtil.File.class),
    Template(19, SparkplugBProto.Payload.Template.class),

    // Additional PropertyValue Types (PropertyValue Types (20 and 21) are NOT metric datatypes)
    PropertySet(20, SparkplugBProto.Payload.PropertySet.class),
    PropertySetList(21, SparkplugBProto.Payload.PropertySetList.class),

    // Array Types
    Int8Array(22, Byte[].class),
    Int16Array(23, Short[].class),
    Int32Array(24, Integer[].class),
    Int64Array(25, Long[].class),
    UInt8Array(26, Short[].class),
    UInt16Array(27, Integer[].class),
    UInt32Array(28, Long[].class),
    UInt64Array(29, BigInteger[].class),
    FloatArray(30, Float[].class),
    DoubleArray(31, Double[].class),
    BooleanArray(32, Boolean[].class),
    StringArray(33, String[].class),
    DateTimeArray(34, Date[].class),

    // Unknown
    Unknown(0, Object.class);

    private Class<?> clazz = null;
    private int intValue = 0;

    /**
     * Constructor
     *
     * @param intValue the integer value of this {@link MetricDataType}
     * @param clazz    the {@link Class} type associated with this {@link MetricDataType}
     */
    private MetricDataType(int intValue, Class<?> clazz) {
        this.intValue = intValue;
        this.clazz = clazz;
    }

    /**
     * Checks the type of a specified value against the specified {@link MetricDataType}
     *
     * @param value the {@link Object} value to check against the {@link MetricDataType}
     * @throws AdaptorException if the value is not a valid type for the given {@link MetricDataType}
     */
    public void checkType(Object value) throws AdaptorException {
        if (value != null && !clazz.isAssignableFrom(value.getClass())) {
            String msgError = "Failed type check - " + clazz + " != " + ((value != null) ? value.getClass().toString() : "null");
            log.debug(msgError);
            throw new AdaptorException(msgError);
        }
    }

    /**
     * Returns an integer representation of the data type.
     *
     * @return an integer representation of the data type.
     */
    public int toIntValue() {
        return this.intValue;
    }

    /**
     * Converts the integer representation of the data type into a {@link MetricDataType} instance.
     *
     * @param i the integer representation of the data type.
     * @return a {@link MetricDataType} instance.
     */
    public static MetricDataType fromInteger(int i) {
        switch (i) {
            case 1:
                return Int8;
            case 2:
                return Int16;
            case 3:
                return Int32;
            case 4:
                return Int64;
            case 5:
                return UInt8;
            case 6:
                return UInt16;
            case 7:
                return UInt32;
            case 8:
                return UInt64;
            case 9:
                return Float;
            case 10:
                return Double;
            case 11:
                return Boolean;
            case 12:
                return String;
            case 13:
                return DateTime;
            case 14:
                return Text;
            case 15:
                return UUID;
            case 16:
                return DataSet;
            case 17:
                return Bytes;
            case 18:
                return File;
            case 19:
                return Template;
            case 20:
                return PropertySet;
            case 21:
                return PropertySetList;
            case 22:
                return Int8Array;
            case 23:
                return Int16Array;
            case 24:
                return Int32Array;
            case 25:
                return Int64Array;
            case 26:
                return UInt8Array;
            case 27:
                return UInt16Array;
            case 28:
                return UInt32Array;
            case 29:
                return UInt64Array;
            case 30:
                return FloatArray;
            case 31:
                return DoubleArray;
            case 32:
                return BooleanArray;
            case 33:
                return StringArray;
            case 34:
                return DateTimeArray;
            default:
                return Unknown;
        }
    }

    /**
     * @return the class type for this DataType
     */
    public Class<?> getClazz() {
        return clazz;
    }


}