/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
