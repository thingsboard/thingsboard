// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.exception;

import lombok.Getter;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;

@Getter
public class AiCreditsExceededException extends ThingsboardRuntimeException {

    public static final String TENANT_LIMIT = "TENANT";

    private final String limitType;

    public AiCreditsExceededException(String limitType) {
        super("Out of AI credits", ThingsboardErrorCode.TOO_MANY_REQUESTS);
        this.limitType = limitType;
    }

}
