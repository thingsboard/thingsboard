// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.security;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ValidationResult<V> {

    private final ValidationResultCode resultCode;
    private final String message;
    private final V v;

    public static <V> ValidationResult<V> ok(V v) {
        return new ValidationResult<>(ValidationResultCode.OK, "Ok", v);
    }

    public static <V> ValidationResult<V> accessDenied(String message) {
        return new ValidationResult<>(ValidationResultCode.ACCESS_DENIED, message, null);
    }

    public static <V> ValidationResult<V> entityNotFound(String message) {
        return new ValidationResult<>(ValidationResultCode.ENTITY_NOT_FOUND, message, null);
    }

    public static <V> ValidationResult<V> unauthorized(String message) {
        return new ValidationResult<>(ValidationResultCode.UNAUTHORIZED, message, null);
    }

    public static <V> ValidationResult<V> internalError(String message) {
        return new ValidationResult<>(ValidationResultCode.INTERNAL_ERROR, message, null);
    }

}
