// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.action;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.server.common.data.EntityType;

@Data
public class TbChangeOwnerNodeConfiguration implements NodeConfiguration<TbChangeOwnerNodeConfiguration> {

    private String ownerNamePattern;
    private EntityType ownerType;

    private boolean createOwnerIfNotExists;
    private boolean createOwnerOnOriginatorLevel;

    @Override
    public TbChangeOwnerNodeConfiguration defaultConfiguration() {
        TbChangeOwnerNodeConfiguration configuration = new TbChangeOwnerNodeConfiguration();
        configuration.setOwnerType(EntityType.TENANT);
        configuration.setCreateOwnerIfNotExists(false);
        configuration.setCreateOwnerOnOriginatorLevel(false);
        return configuration;
    }
}
