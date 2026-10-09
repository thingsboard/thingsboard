// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.exception;

import lombok.Getter;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.setup.SystemSetupState;

/**
 * Thrown while the first-time setup of the instance is not complete and the management plane is therefore locked.
 * Carries the current {@link SystemSetupState} so the activation UI knows which setup step to render.
 */
@Getter
public class SystemSetupIncompleteException extends ThingsboardRuntimeException {

    private final SystemSetupState setupState;

    public SystemSetupIncompleteException(SystemSetupState setupState) {
        super(messageFor(setupState), ThingsboardErrorCode.SETUP_INCOMPLETE);
        this.setupState = setupState;
    }

    private static String messageFor(SystemSetupState setupState) {
        return setupState == SystemSetupState.NON_PRODUCTION_CONFIRMATION_REQUIRED
                ? DataConstants.NON_PRODUCTION_CONFIRMATION_PROMPT
                : "System setup is not complete";
    }

}
