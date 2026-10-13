// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.action;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;

@Data
public class TbAddToGroupConfiguration extends TbAbstractGroupActionConfigration implements NodeConfiguration<TbAddToGroupConfiguration> {

    private boolean createGroupIfNotExists;
    private boolean removeFromCurrentGroups;

    @Override
    public TbAddToGroupConfiguration defaultConfiguration() {
        TbAddToGroupConfiguration configuration = new TbAddToGroupConfiguration();
        configuration.setGroupNamePattern("");
        configuration.setCreateGroupIfNotExists(false);
        configuration.setRemoveFromCurrentGroups(false);
        configuration.setGroupCacheExpiration(300);
        return configuration;
    }
}