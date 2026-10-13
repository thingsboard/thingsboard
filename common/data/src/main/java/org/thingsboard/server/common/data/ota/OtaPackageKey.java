// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data.ota;

import lombok.Getter;

public enum OtaPackageKey {

    TITLE("title"), VERSION("version"), TS("ts"), STATE("state"), SIZE("size"), CHECKSUM("checksum"), CHECKSUM_ALGORITHM("checksum_algorithm"), URL("url"), ID("id"), TAG("tag");

    @Getter
    private final String value;

    OtaPackageKey(String value) {
        this.value = value;
    }
}
