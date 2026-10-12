// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.edge.exception;

import java.io.Serial;

public class EdgeConnectionException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = -4372754681230555723L;

    public EdgeConnectionException(String message) {
        super(message);
    }

    public EdgeConnectionException(String message, Throwable cause) {
        super(message, cause);
    }

}
