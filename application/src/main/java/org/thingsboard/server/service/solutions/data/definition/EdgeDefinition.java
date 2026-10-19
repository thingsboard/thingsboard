// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.solutions.data.definition;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.EntityType;

import java.util.Collections;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class EdgeDefinition extends CustomerEntityDefinition {

    private String type;
    private String label;
    private String rootRuleChainId;
    private List<String> ruleChainIds = Collections.emptyList();
    private List<EdgeEntityGroupDefinition> userGroups = Collections.emptyList();
    private List<EdgeEntityGroupDefinition> assetGroups = Collections.emptyList();
    private List<EdgeEntityGroupDefinition> deviceGroups = Collections.emptyList();
    private List<EdgeEntityGroupDefinition> dashboardGroups = Collections.emptyList();
    private List<String> schedulerEventIds = Collections.emptyList();
    private List<String> deviceIds = Collections.emptyList();
    private List<String> assetIds = Collections.emptyList();

    @Override
    public EntityType getEntityType() {
        return EntityType.EDGE;
    }

}
