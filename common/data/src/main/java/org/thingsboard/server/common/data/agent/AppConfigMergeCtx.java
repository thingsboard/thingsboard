// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.id.EntityId;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AppConfigMergeCtx {

    private AgentAppTemplate template;
    private String selectedComposeType;
    private EntityId relatedEntityId;
    private boolean setHostValues;
    private AgentAppEventActionType actionType;
    private String baseUrl;

    public static AppConfigMergeCtx empty() {
        return new AppConfigMergeCtx();
    }
}
