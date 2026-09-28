// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.action;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;

@Data
public class TbRemoveFromGroupConfiguration extends TbAbstractGroupActionConfigration implements NodeConfiguration<TbRemoveFromGroupConfiguration> {

    @Override
    public TbRemoveFromGroupConfiguration defaultConfiguration() {
        TbRemoveFromGroupConfiguration configuration = new TbRemoveFromGroupConfiguration();
        configuration.setGroupNamePattern("");
        configuration.setGroupCacheExpiration(300);
        return configuration;
    }

}
