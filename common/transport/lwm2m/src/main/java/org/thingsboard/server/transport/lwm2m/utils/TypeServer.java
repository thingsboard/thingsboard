/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.transport.lwm2m.utils;

public enum TypeServer {
    BOOTSTRAP(0, "bootstrap"),
    SERVER(1, "server");

    public int code;
    public String type;

    TypeServer(int code, String type) {
        this.code = code;
        this.type = type;
    }

    public static TypeServer fromTypeServer(int code) {
        for (TypeServer sm : TypeServer.values()) {
            if (sm.code == code) {
                return sm;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported type server : %d", code));
    }
}
