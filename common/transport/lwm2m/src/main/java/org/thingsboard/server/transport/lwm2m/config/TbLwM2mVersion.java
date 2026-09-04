// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.config;

import lombok.Getter;
import org.eclipse.leshan.core.LwM2m.LwM2mVersion;
import org.eclipse.leshan.core.request.ContentFormat;

public enum TbLwM2mVersion {
    VERSION_1_0(0, LwM2mVersion.V1_0, ContentFormat.TLV, false),
    VERSION_1_1(1, LwM2mVersion.V1_1, ContentFormat.TEXT, true);

    @Getter
    private final int code;
    @Getter
    private final LwM2mVersion version;
    @Getter
    private final ContentFormat contentFormat;
    @Getter
    private final boolean composite;

    TbLwM2mVersion(int code, LwM2mVersion version, ContentFormat contentFormat, boolean composite) {
        this.code = code;
        this.version = version;
        this.contentFormat = contentFormat;
        this.composite = composite;
    }

    public static TbLwM2mVersion fromVersion(LwM2mVersion version) {
        for (TbLwM2mVersion to : TbLwM2mVersion.values()) {
            if (to.version.equals(version)) {
                return to;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported typeLwM2mVersion type : %s", version));
    }

    public static TbLwM2mVersion fromVersionStr(String versionStr) {
        for (TbLwM2mVersion to : TbLwM2mVersion.values()) {
            if (to.version.toString().equals(versionStr)) {
                return to;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported contentFormatLwM2mVersion version : %s", versionStr));
    }

    public static TbLwM2mVersion fromCode(int code) {
        for (TbLwM2mVersion to : TbLwM2mVersion.values()) {
            if (to.code == code) {
                return to;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported codeLwM2mVersion code : %d", code));
    }
}

