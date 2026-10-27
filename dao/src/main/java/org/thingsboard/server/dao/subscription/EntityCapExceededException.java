// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.subscription;

/**
 * Thrown when an instance holds more entities than its licence covers, in the one situation where that is a
 * refusal rather than a soft cap. Its own type so the startup gate and the upgrade pre-flight can report the
 * operator-facing message on its own, without a stack trace.
 */
public class EntityCapExceededException extends RuntimeException {

    public EntityCapExceededException(String message) {
        super(message);
    }

}
