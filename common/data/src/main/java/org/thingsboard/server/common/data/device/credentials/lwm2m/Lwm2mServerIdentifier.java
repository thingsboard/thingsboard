/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.common.data.device.credentials.lwm2m;

/**
 * Enum representing predefined LwM2M Short Server Identifiers.
 * <p>
 * See OMA Lightweight M2M Specification for details about the server identifier space.
 */
public enum Lwm2mServerIdentifier {

    /**
     * Not used for identifying an LwM2M Server (0).
     */
    NOT_USED_IDENTIFYING_LWM2M_SERVER_MIN(0, "Bootstrap Short Server ID", false),

    /**
     * Primary LwM2M Server Short Server ID (1).
     * Upper boundary for valid LwM2M Server Identifiers (1–65534).
     */
    PRIMARY_LWM2M_SERVER(1, "LwM2M Server Short Server ID", true),

    /**
     * Maximum valid LwM2M Server ID (65534).
     * Upper boundary for valid LwM2M Server Identifiers (1–65534).
     */
    LWM2M_SERVER_MAX(65534, "LwM2M Server Short Server ID", true),

    /**
     * Not used for identifying an LwM2M Server (65535).
     * Reserved sentinel value representing "no server associated" or "invalid ID".
     * MUST NOT be assigned to any LwM2M Server according to OMA-TS-LightweightM2M-Core, §6.2.1.
     * OMA LwM2M Core / v1.2: Server / Short Server ID): «MAX_ID 65535 is a reserved value and MUST NOT be used for identifying an Object»
     */
    NOT_USED_IDENTIFYING_LWM2M_SERVER_MAX(65535, "Reserved sentinel value (no active server)", false);

    private final Integer id;
    private final String description;
    private final boolean isLwm2mServer;

    Lwm2mServerIdentifier(Integer id, String description, boolean isLwm2mServer) {
        this.id = id;
        this.description = description;
        this.isLwm2mServer = isLwm2mServer;
    }

    /**
     * @return the integer value of this Short Server ID.
     */
    public Integer getId() {
        return id;
    }

    /**
     * @return a human-readable description of this Server ID.
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return true if this ID represents a Lwm2m Server.
     */
    public boolean isLwm2mServer() {
        return isLwm2mServer;
    }

    /**
     * Checks whether a given ID represents a valid LwM2M Server (1–65534).
     * @param id Short Server ID value.
     * @return true if the ID belongs to a standard LwM2M Server.
     */
    public static boolean isLwm2mServer(Integer id) {
        return id != null && id >= PRIMARY_LWM2M_SERVER.id && id <= LWM2M_SERVER_MAX.id;
    }
    public static boolean isNotLwm2mServer(Integer id) {
        return id == null || id < PRIMARY_LWM2M_SERVER.id || id > LWM2M_SERVER_MAX.id;
    }

    /**
     * Returns a {@link Lwm2mServerIdentifier} instance matching the given ID.
     * @param id numeric ID.
     * @return corresponding enum constant.
     * @throws IllegalArgumentException if no constant matches the given ID.
     */
    public static Lwm2mServerIdentifier fromId(Integer id) {
        for (Lwm2mServerIdentifier s : values()) {
            if (s.id == id) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown Lwm2mServerIdentifier: " + id);
    }

    @Override
    public String toString() {
        return name() + "(" + id + ") - " + description;
    }
}
