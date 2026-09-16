// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.server.client;

import lombok.Getter;

public class LwM2MClientStateException extends Exception {

    private static final long serialVersionUID = 3307690997951364046L;

    @Getter
    private final LwM2MClientState state;

    public LwM2MClientStateException(LwM2MClientState state, String message) {
        super(message);
        this.state = state;
    }
}
