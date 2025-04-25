/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.dao.service.validator;

import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.Output;
import org.thingsboard.server.common.data.cf.configuration.OutputType;

import java.util.Map;

@Component
public class CalculatedFieldReprocessingValidator {

    public CFReprocessingValidationResponse validate(CalculatedField calculatedField) {
        CFReprocessingValidationResponse argsCheck = checkArguments(calculatedField.getConfiguration().getArguments());
        if (!argsCheck.isValid()) {
            return argsCheck;
        }

        CFReprocessingValidationResponse outputCheck = checkOutput(calculatedField.getConfiguration().getOutput());
        if (!outputCheck.isValid()) {
            return outputCheck;
        }

        return CFReprocessingValidationResponse.valid();
    }

    private CFReprocessingValidationResponse checkOutput(Output output) {
        if (output == null) {
            return CFReprocessingValidationResponse.invalid("Calculated field has no output defined.");
        }

        if (OutputType.ATTRIBUTES.equals(output.getType())) {
            return CFReprocessingValidationResponse.invalid("Calculated field with output type ATTRIBUTE cannot be reprocessed.");
        }

        return CFReprocessingValidationResponse.valid();
    }

    private CFReprocessingValidationResponse checkArguments(Map<String, Argument> arguments) {
        if (arguments == null || arguments.isEmpty()) {
            return CFReprocessingValidationResponse.invalid("Calculated field has no arguments defined.");
        }

        boolean containsTelemetry = arguments.values().stream()
                .anyMatch(arg -> ArgumentType.TS_LATEST.equals(arg.getRefEntityKey().getType()) ||
                        ArgumentType.TS_ROLLING.equals(arg.getRefEntityKey().getType()));

        if (!containsTelemetry) {
            return CFReprocessingValidationResponse.invalid("Calculated field must contain at least one time series based argument (TS_LATEST or TS_ROLLING).");
        }

        return CFReprocessingValidationResponse.valid();
    }

    public record CFReprocessingValidationResponse(boolean isValid, String message) {

        public static CFReprocessingValidationResponse valid() {
            return new CFReprocessingValidationResponse(true, null);
        }

        public static CFReprocessingValidationResponse invalid(String message) {
            return new CFReprocessingValidationResponse(false, message);
        }

    }

}
