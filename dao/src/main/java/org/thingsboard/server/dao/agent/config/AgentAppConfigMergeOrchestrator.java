// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.agent.config;

import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.agent.AppConfigMergeCtx;
import org.thingsboard.server.common.data.agent.HasAgentAppConfig;

import java.util.List;
import java.util.function.Predicate;

@Component
public class AgentAppConfigMergeOrchestrator {

    private final List<AppConfigMergeRule> rules;

    public AgentAppConfigMergeOrchestrator(List<AppConfigMergeRule> rules) {
        this.rules = rules;
    }
    
    public void mergeOnSave(HasAgentAppConfig hasAgentAppConfig, AppConfigMergeCtx ctx) {
        merge(hasAgentAppConfig, ctx,
                rule -> rule.supports(hasAgentAppConfig, ctx) && rule.isAppliedOnSave());
    }

    public void merge(HasAgentAppConfig hasAgentAppConfig, AppConfigMergeCtx ctx) {
        merge(hasAgentAppConfig, ctx, rule -> rule.supports(hasAgentAppConfig, ctx));
    }

    private void merge(HasAgentAppConfig hasAgentAppConfig, AppConfigMergeCtx ctx,
                      Predicate<AppConfigMergeRule> predicate) {
        if (rules == null || rules.isEmpty()) {
            return;
        }
        for (AppConfigMergeRule rule : rules) {
            if (predicate.test(rule)) {
                rule.apply(hasAgentAppConfig, ctx);
            }
        }
    }
}
