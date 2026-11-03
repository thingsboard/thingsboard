// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.ws;

import lombok.Getter;
import lombok.ToString;

import java.util.Optional;

@ToString
public class SessionEvent {

    public enum SessionEventType {
        ESTABLISHED, CLOSED, ERROR
    };

    @Getter
    private final SessionEventType eventType;
    @Getter
    private final Optional<Throwable> error;

    private SessionEvent(SessionEventType eventType, Throwable error) {
        super();
        this.eventType = eventType;
        this.error = Optional.ofNullable(error);
    }

    public static SessionEvent onEstablished() {
        return new SessionEvent(SessionEventType.ESTABLISHED, null);
    }

    public static SessionEvent onClosed() {
        return new SessionEvent(SessionEventType.CLOSED, null);
    }

    public static SessionEvent onError(Throwable t) {
        return new SessionEvent(SessionEventType.ERROR, t);
    }

}
