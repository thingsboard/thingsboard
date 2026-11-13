// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai.transport;

public class TbAiChannelUnavailableException extends RuntimeException {

    public TbAiChannelUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

}
