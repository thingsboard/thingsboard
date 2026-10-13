// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.transform;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.group.EntityGroup;

@Data
public class TbDuplicateMsgToGroupByNameNodeConfiguration implements NodeConfiguration<TbDuplicateMsgToGroupByNameNodeConfiguration> {

    private boolean searchEntityGroupForTenantOnly;
    private boolean considerMessageOriginatorAsAGroupOwner;
    private EntityType groupType;
    private String groupName;

    @Override
    public TbDuplicateMsgToGroupByNameNodeConfiguration defaultConfiguration() {
        var configuration = new TbDuplicateMsgToGroupByNameNodeConfiguration();
        configuration.setSearchEntityGroupForTenantOnly(false);
        configuration.setConsiderMessageOriginatorAsAGroupOwner(true);
        configuration.setGroupType(EntityType.USER);
        configuration.setGroupName(EntityGroup.GROUP_ALL_NAME);
        return configuration;
    }
}
