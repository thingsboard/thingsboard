// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class InternalErrorException extends Exception implements ToErrorResponseEntity {

    public InternalErrorException(String message) {
        super(message);
    }

    @Override
    public ResponseEntity<String> toErrorResponseEntity() {
        return new ResponseEntity<>(getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
