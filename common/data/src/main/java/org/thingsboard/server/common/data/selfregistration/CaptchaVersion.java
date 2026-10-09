// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.selfregistration;

import lombok.Getter;

public enum CaptchaVersion {

    V_3("v3"),
    V_2("v2"),
    ENTERPRISE("enterprise");

    @Getter
    private final String name;

    CaptchaVersion(String name) {
        this.name = name;
    }
}
