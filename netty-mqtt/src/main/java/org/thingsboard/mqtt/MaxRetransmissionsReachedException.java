// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.mqtt;

public class MaxRetransmissionsReachedException extends RuntimeException {

    public MaxRetransmissionsReachedException(String message) {
        super(message);
    }

}
