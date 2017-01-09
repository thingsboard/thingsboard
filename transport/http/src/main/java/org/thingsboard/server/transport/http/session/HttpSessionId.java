/**
 * Copyright © 2016-2017 The Thingsboard Authors
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
package org.thingsboard.server.transport.http.session;

import org.thingsboard.server.common.data.id.SessionId;

import java.util.UUID;

/**
 * @author Andrew Shvayka
 */
public class HttpSessionId implements SessionId {

    private final UUID id;

    public HttpSessionId() {
        this.id = UUID.randomUUID();
    }

    @Override
    public String toUidStr() {
        return id.toString();
    }
}
