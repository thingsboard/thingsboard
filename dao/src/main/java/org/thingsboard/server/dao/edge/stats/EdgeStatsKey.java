// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.edge.stats;

import lombok.Getter;

@Getter
public enum EdgeStatsKey {
    DOWNLINK_MSGS_ADDED("downlinkMsgsAdded"),
    DOWNLINK_MSGS_PUSHED("downlinkMsgsPushed"),
    DOWNLINK_MSGS_PERMANENTLY_FAILED("downlinkMsgsPermanentlyFailed"),
    DOWNLINK_MSGS_TMP_FAILED("downlinkMsgsTmpFailed"),
    DOWNLINK_MSGS_LAG("downlinkMsgsLag");

    private final String key;

    EdgeStatsKey(String key) {
        this.key = key;
    }

}
