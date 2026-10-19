// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.ota;

import lombok.Getter;

public enum OtaPackageType {

    FIRMWARE("fw"), SOFTWARE("sw");

    @Getter
    private final String keyPrefix;

    OtaPackageType(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }
}
