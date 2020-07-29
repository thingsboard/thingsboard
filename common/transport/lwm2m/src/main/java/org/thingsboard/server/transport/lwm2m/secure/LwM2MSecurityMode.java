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
package org.thingsboard.server.transport.lwm2m.secure;

public enum LwM2MSecurityMode {

    PSK(0, "psk"),
    RPK(1, "rpk"),
    X509(2, "x509"),
    NO_SEC(3, "no_sec"),
    X509_EST(4, "x509_est"),
    REDIS(7, "redis"),
    DEFAULT_MODE(255, "default_mode");

    public int code;
    public String  subEndpoint;

    LwM2MSecurityMode(int code, String subEndpoint) {
        this.code = code;
        this.subEndpoint = subEndpoint;
    }

    public static LwM2MSecurityMode fromSecurityMode(long code) {
        return fromSecurityMode((int) code);
    }

    public static LwM2MSecurityMode fromSecurityMode(int code) {
        for (LwM2MSecurityMode sm : LwM2MSecurityMode.values()) {
            if (sm.code == code) {
                return sm;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported security code : %d", code));
    }


    public static LwM2MSecurityMode fromSecurityMode(String  subEndpoint) {
        for (LwM2MSecurityMode sm : LwM2MSecurityMode.values()) {
            if (sm.subEndpoint.equals(subEndpoint)) {
                return sm;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported security subEndpoint : %d", subEndpoint));
    }

}
