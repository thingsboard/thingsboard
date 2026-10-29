// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.script.api.tbel;

import java.io.Serial;

/**
 * Thrown by the TBEL {@code raiseError(message)} function. The message is written by the script author
 * and is reported to the user as is.
 */
public class TbelRaisedErrorException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 4818203937493207465L;

    public TbelRaisedErrorException(String message) {
        super(message);
    }

}
