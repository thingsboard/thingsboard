// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.security.event;

import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.UserId;

@EqualsAndHashCode(callSuper = true)
public class UserCredentialsInvalidationEvent extends UserAuthDataChangedEvent {
    private final UserId userId;
    private final long ts;

    public UserCredentialsInvalidationEvent(UserId userId) {
        this.userId = userId;
        this.ts = System.currentTimeMillis();
    }

    @Override
    public String getId() {
        return userId.toString();
    }

    @Override
    public long getTs() {
        return ts;
    }
}
