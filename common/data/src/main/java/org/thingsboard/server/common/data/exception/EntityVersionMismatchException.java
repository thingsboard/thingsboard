// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.exception;

import org.thingsboard.server.common.data.EntityType;

public class EntityVersionMismatchException extends RuntimeException {

    public EntityVersionMismatchException(String message, Throwable cause) {
        super(message, cause);
    }

    public EntityVersionMismatchException(EntityType entityType, Throwable cause) {
        this((entityType != null ? entityType.getNormalName() : "Entity") + " was already changed by someone else", cause);
    }

}
