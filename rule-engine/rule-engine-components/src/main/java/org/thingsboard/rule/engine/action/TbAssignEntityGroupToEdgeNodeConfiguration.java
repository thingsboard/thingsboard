// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.action;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.rule.engine.api.NodeConfiguration;

import java.util.Collections;

@Data
@EqualsAndHashCode(callSuper = true)
public class TbAssignEntityGroupToEdgeNodeConfiguration extends TbAbstractEdgeEntityGroupActionNodeConfiguration
        implements NodeConfiguration<TbAssignEntityGroupToEdgeNodeConfiguration> {

    private boolean createGroupIfNotExists;

    @Override
    public TbAssignEntityGroupToEdgeNodeConfiguration defaultConfiguration() {
        var configuration = new TbAssignEntityGroupToEdgeNodeConfiguration();
        configuration.setEdgeSource(EdgeSource.MESSAGE_ORIGINATOR);
        configuration.setEntityGroups(Collections.emptyList());
        configuration.setCreateGroupIfNotExists(false);
        return configuration;
    }

}
