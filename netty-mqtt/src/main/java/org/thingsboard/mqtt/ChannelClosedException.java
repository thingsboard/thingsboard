// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.mqtt;

import java.io.Serial;

public class ChannelClosedException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 6266638352424706909L;

    public ChannelClosedException() {
    }

    public ChannelClosedException(String message) {
        super(message);
    }

    public ChannelClosedException(String message, Throwable cause) {
        super(message, cause);
    }

    public ChannelClosedException(Throwable cause) {
        super(cause);
    }

    public ChannelClosedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
