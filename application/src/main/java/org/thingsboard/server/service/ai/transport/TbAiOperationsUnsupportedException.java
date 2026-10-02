// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai.transport;

public class TbAiOperationsUnsupportedException extends RuntimeException {

    public TbAiOperationsUnsupportedException(String message) {
        super(message);
    }

}
