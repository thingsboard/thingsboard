// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.api.exception;

import java.io.Serial;

public class IntegrationFeatureDisabledException extends RuntimeException {

    public static final String DEFAULT_MESSAGE = "Integration feature is disabled due to API limits!";

    @Serial
    private static final long serialVersionUID = 4320590259670380738L;

    public IntegrationFeatureDisabledException() {
        super(DEFAULT_MESSAGE);
    }

    public IntegrationFeatureDisabledException(String message) {
        super(message);
    }

}
