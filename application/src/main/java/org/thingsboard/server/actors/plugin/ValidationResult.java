/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
package org.thingsboard.server.actors.plugin;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ValidationResult {

    private final ValidationResultCode resultCode;
    private final String message;

    public static ValidationResult ok() {
        return new ValidationResult(ValidationResultCode.OK, "Ok");
    }

    public static ValidationResult accessDenied(String message) {
        return new ValidationResult(ValidationResultCode.ACCESS_DENIED, message);
    }

    public static ValidationResult entityNotFound(String message) {
        return new ValidationResult(ValidationResultCode.ENTITY_NOT_FOUND, message);
    }

    public static ValidationResult unauthorized(String message) {
        return new ValidationResult(ValidationResultCode.UNAUTHORIZED, message);
    }

    public static ValidationResult internalError(String message) {
        return new ValidationResult(ValidationResultCode.INTERNAL_ERROR, message);
    }

}
