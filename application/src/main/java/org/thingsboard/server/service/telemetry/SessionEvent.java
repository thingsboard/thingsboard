/**
 * Copyright © 2016-2021 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.telemetry;

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
