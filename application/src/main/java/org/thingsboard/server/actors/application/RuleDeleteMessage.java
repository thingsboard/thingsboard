package org.thingsboard.server.actors.application;

import lombok.Getter;
import org.thingsboard.server.common.data.id.RuleId;

public class RuleDeleteMessage {
    @Getter
    private final RuleId ruleId;

    public RuleDeleteMessage(RuleId ruleId) {
        this.ruleId = ruleId;
    }


}
