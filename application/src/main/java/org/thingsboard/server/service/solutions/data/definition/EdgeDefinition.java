// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
    private List<String> deviceIds = Collections.emptyList();
    private List<String> assetIds = Collections.emptyList();
    private List<String> dashboardIds = Collections.emptyList();

    @Override
    public EntityType getEntityType() {
        return EntityType.EDGE;
    }

}
