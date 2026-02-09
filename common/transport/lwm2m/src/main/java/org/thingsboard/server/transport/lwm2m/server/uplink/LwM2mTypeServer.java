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
package org.thingsboard.server.transport.lwm2m.server.uplink;

public enum LwM2mTypeServer {
    BOOTSTRAP(0, "bootstrap"),
    CLIENT(1, "client");

    public int code;
    public String type;

    LwM2mTypeServer(int code, String type) {
        this.code = code;
        this.type = type;
    }

    public static LwM2mTypeServer fromLwM2mTypeServer(String type) {
        for (LwM2mTypeServer sm : LwM2mTypeServer.values()) {
            if (sm.type.equals(type)) {
                return sm;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported typeServer type : %d", type));
    }
}

