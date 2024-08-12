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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.thingsboard.server.common.data.gateway.ConnectorType;
import org.thingsboard.server.common.data.gateway.connector.validators.GatewayConnectorValidationErrorRecord;
import org.thingsboard.server.common.data.gateway.connector.validators.GatewayConnectorValidationResult;
import org.thingsboard.server.common.data.gateway.connector.validators.GatewayConnectorValidationWarningRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class ConnectorConfigurationValidator {

    private final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    private final Validator validator = factory.getValidator();

    protected <T> GatewayConnectorValidationResult validateConfiguration(Map<String, Object> connectorConfiguration, Class<T> configClass) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        ValidationDeserializationProblemHandler handler = new ValidationDeserializationProblemHandler();
        mapper.addHandler(handler);
        T configuration = null;
        List<GatewayConnectorValidationErrorRecord> errors = new ArrayList<>();

        try {
            configuration = mapper.convertValue(connectorConfiguration, configClass);
        } catch (IllegalArgumentException e) {
            Throwable cause = e.getCause();
            if (cause instanceof InvalidFormatException) {
                handleInvalidFormatException((InvalidFormatException) cause, errors);
            } else if (cause instanceof JsonMappingException) {
                handleJsonMappingException((JsonMappingException) cause, errors);
            } else {
                errors.add(new GatewayConnectorValidationErrorRecord("", "Invalid configuration format: " + e.getMessage()));
            }
        } catch (Exception e) {
            errors.add(new GatewayConnectorValidationErrorRecord("", "Invalid configuration format: " + e.getMessage()));
        }

        errors.addAll(handler.getErrors());

        if (configuration != null) {
            Set<ConstraintViolation<T>> violations = validator.validate(configuration);

            if (!violations.isEmpty()) {
                errors.addAll(violations.stream()
                        .map(violation -> {
                            String path = "." + violation.getPropertyPath().toString()
                                    .replace("[", "/")
                                    .replace("]", "")
                                    .replace("/", ".");
                            return new GatewayConnectorValidationErrorRecord(path, violation.getMessage());
                        })
                        .toList());
            }
        }

        List<GatewayConnectorValidationWarningRecord> warnings = new ArrayList<>(handler.getWarnings());

        return errors.isEmpty()
                ? new GatewayConnectorValidationResult(true, List.of(), warnings)
                : new GatewayConnectorValidationResult(false, errors, warnings);
    }

    private void handleInvalidFormatException(InvalidFormatException e, List<GatewayConnectorValidationErrorRecord> errors) {
        String path = e.getPathReference();
        String errorMessage = "Invalid value '" + e.getValue() + "' for field '" + path + "'. Expected type: " + e.getTargetType().getSimpleName();
        errors.add(new GatewayConnectorValidationErrorRecord(path, errorMessage));
    }

    private void handleJsonMappingException(JsonMappingException e, List<GatewayConnectorValidationErrorRecord> errors) {
        String path = getPathFromException(e);
        String errorMessage = "Error in field '" + path + "': " + e.getOriginalMessage();
        errors.add(new GatewayConnectorValidationErrorRecord(path, errorMessage));
    }

    private String getPathFromException(JsonMappingException e) {
        return "/" + e.getPath().stream()
                .map(JsonMappingException.Reference::getFieldName)
                .reduce("", (partialPath, element) -> partialPath + "/" + element);
    }

    public abstract ConnectorType getType();

    public abstract GatewayConnectorValidationResult validate(Map<String, Object> connectorConfiguration, String version);
}
