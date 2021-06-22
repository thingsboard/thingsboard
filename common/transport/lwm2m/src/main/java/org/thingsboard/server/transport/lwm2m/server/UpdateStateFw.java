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
package org.thingsboard.server.transport.lwm2m.server;

/**
 * /** State R
 * 0: Idle (before downloading or after successful updating)
 * 1: Downloading (The data sequence is on the way)
 * 2: Downloaded
 * 3: Updating
 */
public enum UpdateStateFw {
    IDLE(0, "Idle"),
    DOWNLOADING(1, "Downloading"),
    DOWNLOADED(2, "Downloaded"),
    UPDATING(3, "Updating");

    public int code;
    public String type;

    UpdateStateFw(int code, String type) {
        this.code = code;
        this.type = type;
    }

    public static UpdateStateFw fromStateFwByType(String type) {
        for (UpdateStateFw to : UpdateStateFw.values()) {
            if (to.type.equals(type)) {
                return to;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported FW State type  : %s", type));
    }

    public static UpdateStateFw fromStateFwByCode(int code) {
        for (UpdateStateFw to : UpdateStateFw.values()) {
            if (to.code == code) {
                return to;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported FW State code : %s", code));
    }
}
