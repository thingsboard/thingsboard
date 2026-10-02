// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai.transport;

/**
 * TB AI refused the channel handshake with 401 or 403: an error for the user, not a reason to fall back to HTTP.
 */
public class TbAiChannelRejectedException extends RuntimeException {

    public TbAiChannelRejectedException(String reason) {
        super(reason);
    }

}
