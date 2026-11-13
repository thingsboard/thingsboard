// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.agent.config;

import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.AppConfigMergeCtx;
import org.thingsboard.server.common.data.agent.HasAgentAppConfig;

public interface AppConfigMergeRule {

    default boolean isAppliedOnSave() {
        return false;
    }

    boolean supports(HasAgentAppConfig hasAgentAppConfig, AppConfigMergeCtx ctx);

    void apply(HasAgentAppConfig hasAgentAppConfig, AppConfigMergeCtx ctx);

    static AgentApplicationType resolveAppType(HasAgentAppConfig data) {
        if (data instanceof AgentApplication app) {
            return app.getAppType();
        } else if (data instanceof AgentAppProfile profile) {
            return profile.getAppType();
        }
        return null;
    }
}
