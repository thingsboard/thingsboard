// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.event;

import lombok.Getter;

public enum EventType {
    ERROR("error_event", "ERROR"),
    LC_EVENT("lc_event", "LC_EVENT"),
    STATS("stats_event", "STATS"),
    DEBUG_RULE_NODE("rule_node_debug_event", "DEBUG_RULE_NODE", true),
    DEBUG_RULE_CHAIN("rule_chain_debug_event", "DEBUG_RULE_CHAIN", true),
    DEBUG_CALCULATED_FIELD("cf_debug_event", "DEBUG_CALCULATED_FIELD", true);

    @Getter
    private final String table;
    @Getter
    private final String oldName;
    @Getter
    private final boolean debug;

    EventType(String table, String oldName) {
        this(table, oldName, false);
    }

    EventType(String table, String oldName, boolean debug) {
        this.table = table;
        this.oldName = oldName;
        this.debug = debug;
    }

}