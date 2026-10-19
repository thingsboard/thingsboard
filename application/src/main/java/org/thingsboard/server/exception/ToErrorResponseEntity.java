// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.exception;

import org.springframework.http.ResponseEntity;

import java.io.Serializable;

public interface ToErrorResponseEntity extends Serializable {

    ResponseEntity<String> toErrorResponseEntity();

}
