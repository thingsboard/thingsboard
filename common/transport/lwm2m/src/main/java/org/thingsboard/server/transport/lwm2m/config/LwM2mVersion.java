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
package org.thingsboard.server.transport.lwm2m.config;

import lombok.Getter;
import org.eclipse.leshan.core.LwM2m.Version;
import org.eclipse.leshan.core.request.ContentFormat;

public enum LwM2mVersion {
    VERSION_1_0(0, Version.V1_0, ContentFormat.TLV),
    VERSION_1_1(1, Version.V1_1, ContentFormat.TEXT);

    @Getter
    private final int code;
    @Getter
    private final Version version;
    @Getter
    private final ContentFormat contentFormat;

    LwM2mVersion(int code, Version version, ContentFormat contentFormat) {
        this.code = code;
        this.version = version;
        this.contentFormat = contentFormat;
    }

    public static LwM2mVersion fromType(Version version) {
        for (LwM2mVersion to : LwM2mVersion.values()) {
            if (to.version.equals(version)) {
                return to;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported typeLwM2mVersion type : %d", version));
    }

    public static ContentFormat fromContentFormat(String versionStr) {
        for (LwM2mVersion to : LwM2mVersion.values()) {
            if (to.version.toString().equals(versionStr)) {
                return to.contentFormat;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported contentFormatLwM2mVersion version : %d", versionStr));
    }

    public static LwM2mVersion fromCode(int code) {
        for (LwM2mVersion to : LwM2mVersion.values()) {
            if (to.code == code) {
                return to;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported codeLwM2mVersion code : %d", code));
    }
}

