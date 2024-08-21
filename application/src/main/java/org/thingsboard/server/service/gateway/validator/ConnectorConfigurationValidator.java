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
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.thingsboard.server.common.data.gateway.ConnectorType;
import org.thingsboard.server.common.data.gateway.connector.validators.GatewayConnectorValidationRecord;
import org.thingsboard.server.common.data.gateway.connector.validators.GatewayConnectorValidationRecordType;
import org.thingsboard.server.common.data.gateway.connector.validators.GatewayConnectorValidationResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class ConnectorConfigurationValidator {

    private final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    private final Validator validator = factory.getValidator();

    protected <T> GatewayConnectorValidationResult validateConfiguration(String connectorConfiguration, Class<T> configClass) {

        ValidationDeserializationProblemHandler errorHandler = new ValidationDeserializationProblemHandler();
        ObjectMapper mapper = JsonMapper.builder()
                .enable(MapperFeature.BLOCK_UNSAFE_POLYMORPHIC_BASE_TYPES)
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
                .addHandler(errorHandler)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false)
                .configure(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS, false)
                .configure(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE, false)
                .configure(DeserializationFeature.WRAP_EXCEPTIONS, false)
                .configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)
                .configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true)
                .build();

        T configuration = null;
        List<GatewayConnectorValidationRecord> errors = new ArrayList<>();
        FieldLocationTrackingParser trackingParser = null;
        try {
            trackingParser = new FieldLocationTrackingParser(mapper.createParser(connectorConfiguration));
            configuration = mapper.readValue(trackingParser, configClass);
        } catch (IllegalArgumentException e) {
            Throwable cause = e.getCause();
            if (cause instanceof InvalidFormatException) {
                handleInvalidFormatException((InvalidFormatException) cause, errors);
            } else if (cause instanceof JsonMappingException) {
                handleJsonMappingException((JsonMappingException) cause, errors);
            } else {
                errors.add(new GatewayConnectorValidationRecord("Cannot validate! - Invalid configuration format: " + e.getMessage(), 0, 1, GatewayConnectorValidationRecordType.ERROR));
            }
        } catch (Exception e) {
            errors.add(new GatewayConnectorValidationRecord("Cannot validate! - Invalid configuration format: " + e.getMessage(), 0, 1, GatewayConnectorValidationRecordType.ERROR));
        }

        errors.addAll(errorHandler.getErrors());

        if (configuration != null) {
            Set<ConstraintViolation<T>> violations = validator.validate(configuration);

            if (!violations.isEmpty()) {
                Map<String, JsonLocation> finalFieldLocations = trackingParser.getFieldLocations();
                errors.addAll(violations.stream()
                        .map(violation -> {
                            String path = "." + violation.getPropertyPath().toString()
                                    .replace("[", "/")
                                    .replace("]", "")
                                    .replace("/", ".");
                            String message = "\"" + path.substring(path.lastIndexOf(".") + 1) + "\" " + violation.getMessage();
                            int lineNumber = 0;
                            int columnNumber = 1;
                            if (finalFieldLocations != null) {
                                JsonLocation location = getJsonLocationFromPath(finalFieldLocations, path);
                                if (location != null) {
                                    lineNumber = location.getLineNr() - 1;
                                    columnNumber = location.getColumnNr();
                                }
                            }
                            return new GatewayConnectorValidationRecord(message,
                                    lineNumber, columnNumber, GatewayConnectorValidationRecordType.ERROR);
                        })
                        .toList());
            }
        }

        List<GatewayConnectorValidationRecord> annotations = new ArrayList<>();

        annotations.addAll(errors);
        annotations.addAll(errorHandler.getWarnings());


        return new GatewayConnectorValidationResult(errors.isEmpty(), annotations);
    }

    private static JsonLocation getJsonLocationFromPath(Map<String, JsonLocation> fieldLocations, String path) {
        JsonLocation location = fieldLocations.get(path);
        if (location == null) {
            location = fieldLocations.get(path.substring(0, path.lastIndexOf('.')));
        }
        return location;
    }

    private void handleInvalidFormatException(InvalidFormatException e, List<GatewayConnectorValidationRecord> errors) {
        String path = e.getPathReference();
        JsonLocation location = e.getLocation();
        String errorMessage = "Invalid value '" + e.getValue() + "' for field '" + path + "'. Expected type: " + e.getTargetType().getSimpleName();
        errors.add(new GatewayConnectorValidationRecord(errorMessage, location.getLineNr() - 1, location.getColumnNr(), GatewayConnectorValidationRecordType.ERROR));
    }

    private void handleJsonMappingException(JsonMappingException e, List<GatewayConnectorValidationRecord> errors) {
        String path = getPathFromException(e);
        JsonLocation location = e.getLocation();
        String errorMessage = "Error in field '" + path + "': " + e.getOriginalMessage();
        errors.add(new GatewayConnectorValidationRecord(errorMessage, location.getLineNr() - 1, location.getColumnNr(), GatewayConnectorValidationRecordType.ERROR));
    }

    private String getPathFromException(JsonMappingException e) {
        return "/" + e.getPath().stream()
                .map(JsonMappingException.Reference::getFieldName)
                .reduce("", (partialPath, element) -> partialPath + "/" + element);
    }

    public abstract ConnectorType getType();

    public abstract GatewayConnectorValidationResult validate(String connectorConfiguration, String version);
}
