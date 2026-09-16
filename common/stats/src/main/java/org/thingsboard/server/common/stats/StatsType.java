// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.stats;

public enum StatsType {
    RULE_ENGINE("ruleEngine"),
    CORE("core"),
    TRANSPORT("transport"),
    JS_INVOKE("jsInvoke"),
    TBEL_INVOKE("tbelInvoke"),
    RATE_EXECUTOR("rateExecutor"),
    HOUSEKEEPER("housekeeper"),
    EDGE("edge"),
    EDQS("edqs");

    private final String name;

    StatsType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
