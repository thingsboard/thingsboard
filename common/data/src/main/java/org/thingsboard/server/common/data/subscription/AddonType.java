// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.subscription;

import lombok.Getter;

public enum AddonType {

    EDGE("Edge Computing add-on"),
    TRENDZ("Trendz Analytics add-on"),
    WHITE_LABELING("White labeling"),
    PROFESSIONAL_UPGRADE("Professional Pack");

    @Getter
    private final String addonName;

    AddonType(String addonName) {
        this.addonName = addonName;
    }

}
