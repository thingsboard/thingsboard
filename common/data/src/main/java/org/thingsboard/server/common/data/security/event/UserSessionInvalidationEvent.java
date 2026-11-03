// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.security.event;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
public class UserSessionInvalidationEvent extends UserAuthDataChangedEvent {
    private final String sessionId;
    private final long ts;

    public UserSessionInvalidationEvent(String sessionId) {
        this.sessionId = sessionId;
        this.ts = System.currentTimeMillis();
    }

    @Override
    public String getId() {
        return sessionId;
    }

    @Override
    public long getTs() {
        return ts;
    }
}
