/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.core.node.codec.LwM2mValueConverter;
import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.core.util.StringUtils;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

@Slf4j
public class LwM2mValueConverterImpl implements LwM2mValueConverter {

    @Override
    public Object convertValue(Object value, Type currentType, Type expectedType, LwM2mPath resourcePath)
            throws CodecException {
        if (expectedType == null) {
            /** unknown resource, trusted value */
            return value;
        }

        if (currentType == expectedType) {
            /** expected type */
            return value;
        }

        switch (expectedType) {
            case INTEGER:
                switch (currentType) {
                    case FLOAT:
                        log.debug("Trying to convert float value [{}] to integer", value);
                        Long longValue = ((Double) value).longValue();
                        if ((double) value == longValue.doubleValue()) {
                            return longValue;
                        }
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
                        /** let's assume we received the millisecond since 1970/1/1 */
                        return new Date((Long) value);
                    case STRING:
                        log.debug("Trying to convert string value {} to date", value);
                        /** let's assume we received an ISO 8601 format date */
                        try {
                            DatatypeFactory datatypeFactory = DatatypeFactory.newInstance();
                            XMLGregorianCalendar cal = datatypeFactory.newXMLGregorianCalendar((String) value);
                            return cal.toGregorianCalendar().getTime();
                        } catch (DatatypeConfigurationException | IllegalArgumentException e) {
                            log.debug("Unable to convert string to date", e);
                            throw new CodecException("Unable to convert string (%s) to date for resource %s", value,
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
//                        return Long.toString(((Date) value).getTime());
                        String DATE_FORMAT = "MMM d, yyyy HH:mm a";
                        Long timeValue = ((Date) value).getTime();
                        DateFormat formatter = new SimpleDateFormat(DATE_FORMAT);
                        return formatter.format(new Date(timeValue));
                    default:
                        break;
                }
                break;
            case OPAQUE:
                if (currentType == Type.STRING) {
                    /** let's assume we received an hexadecimal string */
                    log.debug("Trying to convert hexadecimal string [{}] to byte array", value);
                    // TODO check if we shouldn't instead assume that the string contains Base64 encoded data
                    try {
                        return Hex.decodeHex(((String) value).toCharArray());
                    } catch (IllegalArgumentException e) {
                        throw new CodecException("Unable to convert hexastring [%s] to byte array for resource %s", value,
                                resourcePath);
                    }
                }
                break;
            default:
        }

        throw new CodecException("Invalid value type for resource %s, expected %s, got %s", resourcePath, expectedType,
                currentType);
    }
}
