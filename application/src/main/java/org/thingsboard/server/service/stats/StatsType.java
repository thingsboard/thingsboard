package org.thingsboard.server.service.stats;

public enum StatsType {
    RULE_ENGINE("ruleEngine"), CORE("core");

    private String name;

    StatsType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
