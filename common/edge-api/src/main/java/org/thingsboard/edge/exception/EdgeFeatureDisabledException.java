// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.edge.exception;

import java.io.Serial;

public class EdgeFeatureDisabledException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = -4719918663724404639L;

    public EdgeFeatureDisabledException(String message) {
        super(message);
    }

}
