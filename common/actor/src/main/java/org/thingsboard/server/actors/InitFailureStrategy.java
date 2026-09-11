// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.actors;

import lombok.Getter;
import lombok.ToString;

@ToString
public class InitFailureStrategy {

    @Getter
    private boolean stop;
    @Getter
    private long retryDelay;

    private InitFailureStrategy(boolean stop, long retryDelay) {
        this.stop = stop;
        this.retryDelay = retryDelay;
    }

    public static InitFailureStrategy retryImmediately() {
        return new InitFailureStrategy(false, 0);
    }

    public static InitFailureStrategy retryWithDelay(long ms) {
        return new InitFailureStrategy(false, ms);
    }

    public static InitFailureStrategy stop() {
        return new InitFailureStrategy(true, 0);
    }
}
