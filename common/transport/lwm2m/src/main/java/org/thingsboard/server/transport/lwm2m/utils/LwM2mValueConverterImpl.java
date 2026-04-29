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
package org.thingsboard.server.transport.lwm2m.utils;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.ObjectLink;
import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.core.node.codec.LwM2mValueConverter;
import org.eclipse.leshan.core.util.Hex;
import org.thingsboard.server.common.data.StringUtils;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;

import static org.eclipse.leshan.core.model.ResourceModel.Type.NONE;
import static org.eclipse.leshan.core.model.ResourceModel.Type.OPAQUE;
import static org.eclipse.leshan.core.model.ResourceModel.Type.TIME;

@Slf4j
public class LwM2mValueConverterImpl implements LwM2mValueConverter {

    private static final LwM2mValueConverterImpl INSTANCE = new LwM2mValueConverterImpl();

    public static LwM2mValueConverterImpl getInstance() {
        return INSTANCE;
    }

    @Override
    public Object convertValue(Object value, Type currentType, Type expectedType, LwM2mPath resourcePath)
            throws CodecException {
        if (value == null) {
           return null;
        }
        if (expectedType == null) {
            /** unknown resource, trusted value */
            return value;
        }

        if (currentType == null) {
            currentType = OPAQUE;
        }

        if (currentType == expectedType || currentType == NONE || currentType == TIME) {
            /** expected type */
            return value;
        }

        switch (expectedType) {
            case INTEGER:
                switch (currentType) {
                    case FLOAT:
                        log.debug("Trying to convert float value [{}] to Integer", value);
                        Long longValue = ((Double) value).longValue();
                        if ((double) value == longValue.doubleValue()) {
                            return longValue;
                        }
                    case STRING:
                        log.debug("Trying to convert String value [{}] to Integer", value);
                        return Long.parseLong((String) value);
                    default:
                        break;
                }
                break;
            case FLOAT:
                switch (currentType) {
                    case INTEGER:
                        log.debug("Trying to convert integer value [{}] to float", value);
                        Double floatValue = ((Long) value).doubleValue();
                        if ((long) value == floatValue.longValue()) {
                            return floatValue;
                        }
                    case STRING:
                        log.debug("Trying to convert String value [{}] to Float", value);
                        return Float.valueOf((String) value);
                    default:
                        break;
                }
                break;
            case BOOLEAN:
                switch (currentType) {
                    case STRING:
                        log.debug("Trying to convert string value {} to boolean", value);
                        if (StringUtils.equalsIgnoreCase((String) value, "true")) {
                            return true;
                        } else if (StringUtils.equalsIgnoreCase((String) value, "false")) {
                            return false;
                        }
                        break;
                    case INTEGER:
                        log.debug("Trying to convert int value {} to boolean", value);
                        Long val = (Long) value;
                        if (val == 1) {
                            return true;
                        } else if (val == 0) {
                            return false;
                        }
                        break;
                    default:
                        break;
                }
                break;
            case TIME:
                switch (currentType) {
                    case INTEGER:
                        log.debug("Trying to convert long value {} to date", value);
                        /* let's assume we received the millisecond since 1970/1/1 */
                        return new Date(((Number) value).longValue() * 1000L);
                    case STRING:
                        log.debug("Trying to convert string value {} to date", value);
                        /** let's assume we received an ISO 8601 format date */
                        try {
                            return new Date(Long.decode(value.toString()));
                            /**
                            DatatypeFactory datatypeFactory = DatatypeFactory.newInstance();
                            XMLGregorianCalendar cal = datatypeFactory.newXMLGregorianCalendar((String) value);
                            return cal.toGregorianCalendar().getTime();
                             **/
                        } catch (IllegalArgumentException e) {
                            log.debug("Unable to convert string to date", e);
                            throw new CodecException("Unable to convert string (%s) to %s for resource %s", value, TIME.name(),
                                    resourcePath);
                        }
                    default:
                        break;
                }
                break;
            case STRING:
                switch (currentType) {
                    case BOOLEAN:
                    case INTEGER:
                    case FLOAT:
                        return String.valueOf(value);
                    case TIME:
                        String DATE_FORMAT = "yyyy-MM-dd[[ ]['T']HH:mm[:ss[.SSS]][ ][XXX][Z][z][VV][O]]";
                        Long timeValue;
                        try {
                            timeValue = ((Date) value).getTime();
                        }
                        catch (Exception e){
                           timeValue = new BigInteger((byte [])value).longValue();
                        }
                        DateFormat formatter = new SimpleDateFormat(DATE_FORMAT);
                        return formatter.format(new Date(timeValue));
                    case OPAQUE:
                        return Hex.encodeHexString((byte[])value);
                    case OBJLNK:
                        return ObjectLink.decodeFromString((String) value);
                    default:
                        break;
                }
                break;
            case OPAQUE:
                if (currentType == Type.INTEGER) {
                    if (value instanceof Integer) {
                        return ByteBuffer.allocate(4).putInt((Integer) value).array();
                    } else {
                        return ByteBuffer.allocate(8).putLong((Long) value).array();
                    }
                } else if (currentType == Type.FLOAT) {
                    if (value instanceof Float) {
                        return ByteBuffer.allocate(4).putFloat((Float) value).array();
                    } else {
                        return ByteBuffer.allocate(8).putDouble((Double) value).array();
                    }
                } else if (currentType == Type.STRING) {
                    /** let's assume we received an hexadecimal string */
                    log.debug("Trying to convert hexadecimal/base64 string [{}] to byte array", value);
                    try {
                        return Hex.decodeHex(((String)value).toCharArray());
                    } catch (IllegalArgumentException e) {
                        try {
                            return Base64.getDecoder().decode(((String) value).getBytes());
                        } catch (IllegalArgumentException ea) {
                            throw new CodecException("Unable to convert hexastring or base64 [%s] to byte array for resource %s",
                                    value, resourcePath);
                        }
                    }
                } else if (currentType == Type.BOOLEAN) {
                    return  new byte[] {(byte)((boolean)value ? 1 : 0)};
                }
                break;
            case OBJLNK:
                if (currentType == Type.STRING) {
                    return ObjectLink.fromPath(value.toString());
                }
            default:
        }
        throw new CodecException("Invalid value type for resource %s, expected %s, got %s", resourcePath, expectedType,
                currentType);
    }
 }
