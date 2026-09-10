// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.msg.tools;

import lombok.Getter;

public class MaxPayloadSizeExceededException extends RuntimeException {

    @Getter
    private final long limit;

    public MaxPayloadSizeExceededException(long limit) {
        super("Payload size exceeds the limit of " + limit + " bytes");
        this.limit = limit;
    }

}
