// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.thingsboard.server.service.security.ValidationResultCode;
import org.thingsboard.server.service.telemetry.exception.ValidationException;

public class EntityNotFoundException extends ValidationException implements ToErrorResponseEntity {

    public EntityNotFoundException(String message) {
        super(message);
    }

    @Override
    public ValidationResultCode getValidationResultCode() {
        return ValidationResultCode.ENTITY_NOT_FOUND;
    }

    @Override
    public ResponseEntity<String> toErrorResponseEntity() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(MediaType.TEXT_PLAIN).body(getMessage());
    }
}
