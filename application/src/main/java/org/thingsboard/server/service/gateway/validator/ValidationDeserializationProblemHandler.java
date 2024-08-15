/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.service.gateway.validator;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import lombok.Getter;
import org.thingsboard.server.common.data.gateway.connector.validators.GatewayConnectorValidationRecord;
import org.thingsboard.server.common.data.gateway.connector.validators.GatewayConnectorValidationRecordType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
public class ValidationDeserializationProblemHandler extends DeserializationProblemHandler {

    private final List<GatewayConnectorValidationRecord> errors = new ArrayList<>();
    private final List<GatewayConnectorValidationRecord> warnings = new ArrayList<>();

    @Override
    public boolean handleUnknownProperty(DeserializationContext ctxt, JsonParser p, JsonDeserializer<?> deserializer, Object beanOrClass, String propertyName) {
        JsonLocation currentLocation = ctxt.getParser().currentLocation();
        warnings.add(new GatewayConnectorValidationRecord("\"" + propertyName + "\" is unknown",
                currentLocation.getLineNr() - 1, currentLocation.getColumnNr(), GatewayConnectorValidationRecordType.WARNING));
        return false;
    }

    @Override
    public Object handleMissingInstantiator(DeserializationContext ctxt, Class<?> instClass, ValueInstantiator valueInsta, JsonParser p, String msg) {
        return null;
    }

    @Override
    public Object handleUnexpectedToken(DeserializationContext ctxt, JavaType targetType, JsonToken t, JsonParser p, String failureMsg) throws IOException {
        JsonLocation currentLocation = ctxt.getParser().currentLocation();
        String errorMessage = getErrorMessageForFieldsWithKnownVariants(targetType.getRawClass(), t, p);
        errors.add(new GatewayConnectorValidationRecord(errorMessage, currentLocation.getLineNr() - 1,
                currentLocation.getColumnNr(), GatewayConnectorValidationRecordType.ERROR));
        return null;
    }

    @Override
    public Object handleWeirdKey(DeserializationContext ctxt, Class<?> rawKeyType, String keyValue, String failureMsg) {
        JsonLocation currentLocation = ctxt.getParser().currentLocation();
        String errorMessage = "Invalid key value \"" + keyValue + "\" for field \""
                + ctxt.getParser().getParsingContext().getCurrentName() + "\". Expected " + rawKeyType.getSimpleName();
        errors.add(new GatewayConnectorValidationRecord(errorMessage, currentLocation.getLineNr() - 1,
                currentLocation.getColumnNr(), GatewayConnectorValidationRecordType.ERROR));
        return null;
    }

    @Override
    public Object handleWeirdNumberValue(DeserializationContext ctxt, Class<?> targetType, Number valueToConvert, String failureMsg) throws IOException {
        JsonLocation currentLocation = ctxt.getParser().currentLocation();
        String errorMessage = getErrorMessageForFieldsWithKnownVariants(targetType, JsonToken.VALUE_NUMBER_INT, ctxt.getParser());
        errors.add(new GatewayConnectorValidationRecord(errorMessage, currentLocation.getLineNr() - 1,
                currentLocation.getColumnNr(), GatewayConnectorValidationRecordType.ERROR));
        return null;
    }

    @Override
    public Object handleWeirdNativeValue(DeserializationContext ctxt, JavaType targetType, Object valueToConvert, JsonParser p) throws IOException {
        JsonLocation currentLocation = ctxt.getParser().currentLocation();
        String errorMessage = getErrorMessageForFieldsWithKnownVariants(targetType.getRawClass(), p.currentToken(), p);
        errors.add(new GatewayConnectorValidationRecord(errorMessage, currentLocation.getLineNr() - 1,
                currentLocation.getColumnNr(), GatewayConnectorValidationRecordType.ERROR));
        return null;
    }

    @Override
    public Object handleWeirdStringValue(DeserializationContext ctxt, Class<?> targetType, String valueToConvert, String failureMsg) {
        JsonLocation currentLocation = ctxt.getParser().currentLocation();
        if (failureMsg.contains("not a valid") || "only \"true\" or \"false\" recognized".equals(failureMsg)) {
            String errorMessage = "Invalid value \"" + valueToConvert + "\" for field \""
                    + ctxt.getParser().getParsingContext().getCurrentName()
                    + "\". Expected type: " + targetType.getSimpleName();
            errors.add(new GatewayConnectorValidationRecord(errorMessage, currentLocation.getLineNr() - 1,
                    currentLocation.getColumnNr(), GatewayConnectorValidationRecordType.ERROR));
        } else {
            String errorMessage = "Unexpected value \"" + valueToConvert + "\" for field \""
                    + ctxt.getParser().getParsingContext().getCurrentName() + "\". Expected one of: " + extractPossibleValues(failureMsg);
            errors.add(new GatewayConnectorValidationRecord(errorMessage, currentLocation.getLineNr() - 1,
                    currentLocation.getColumnNr(), GatewayConnectorValidationRecordType.ERROR));
        }
        return null;
    }

    @Override
    public Object handleInstantiationProblem(DeserializationContext ctxt, Class<?> instClass, Object argument, Throwable t) {
        JsonLocation currentLocation = ctxt.getParser().currentLocation();
        String errorMessage = "Field \"" + ctxt.getParser().getParsingContext().getCurrentName()
                + "\" contains unknown value. Expected one of: " + extractPossibleValues(t.getMessage());
        errors.add(new GatewayConnectorValidationRecord(errorMessage, currentLocation.getLineNr() - 1,
                currentLocation.getColumnNr(), GatewayConnectorValidationRecordType.ERROR));
        return null;
    }

    @Override
    public JavaType handleMissingTypeId(DeserializationContext ctxt, JavaType baseType, TypeIdResolver idResolver, String failureMsg) {
        JsonLocation currentLocation = ctxt.getParser().currentLocation();
        String errorMessage = "Field \"" + extractMissingField(failureMsg) + "\" is missing";
        errors.add(new GatewayConnectorValidationRecord(errorMessage, currentLocation.getLineNr() - 1,
                currentLocation.getColumnNr(), GatewayConnectorValidationRecordType.ERROR));
        return baseType;
    }


    @Override
    public JavaType handleUnknownTypeId(DeserializationContext ctxt, JavaType baseType, String subTypeId, TypeIdResolver idResolver, String failureMsg) {
        JsonLocation currentLocation = ctxt.getParser().currentLocation();
        String errorMessage = "Field \"" + ctxt.getParser().getParsingContext().getCurrentName()
                + "\" contains unknown value, possible values: "
                + extractPossibleValues(failureMsg, baseType.getRawClass().getName());
        errors.add(new GatewayConnectorValidationRecord(errorMessage, currentLocation.getLineNr() - 1,
                currentLocation.getColumnNr(), GatewayConnectorValidationRecordType.ERROR));
        return baseType;
    }

    private String extractPossibleValues(String failureMsg) {
        return extractPossibleValues(failureMsg, null);
    }

    private String extractPossibleValues(String failureMsg, String baseType) {
        try {
            int startIndex = failureMsg.indexOf("type ids = [") + 11;
            int endIndex = failureMsg.indexOf("]", startIndex) + 1;
            String allowedTypes = failureMsg.substring(startIndex, endIndex);
            if (baseType != null && !baseType.isEmpty()) {
                String[] baseTypeClass = baseType.split("\\.");
                if (baseTypeClass.length > 0) {
                    return allowedTypes.replace(baseTypeClass[baseTypeClass.length - 1] + ", ", "");
                }
            }
            return allowedTypes;
        } catch (StringIndexOutOfBoundsException e) {
            return "";
        }
    }

    private String extractMissingField(String failureMsg) {
        int startIndex = failureMsg.indexOf("property '") + 10;
        int endIndex = failureMsg.indexOf("'", startIndex);
        return failureMsg.substring(startIndex, endIndex);
    }


    private static String getErrorMessageForFieldsWithKnownVariants(Class<?> targetTypeClass, JsonToken t, JsonParser p) throws IOException {
        String errorMessage = "Invalid value for field '" + p.currentName()
                + "\". Expected one of: ";
        if (targetTypeClass.isEnum()) {
            errorMessage += Arrays.toString(targetTypeClass.getEnumConstants()).toLowerCase();
        } else {
            errorMessage += targetTypeClass.getSimpleName();
        }
        errorMessage += " but got " + t;
        return errorMessage;
    }
}
