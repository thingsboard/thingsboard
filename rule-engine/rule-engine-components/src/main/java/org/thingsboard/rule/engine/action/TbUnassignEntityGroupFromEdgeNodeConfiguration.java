// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.action;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.rule.engine.api.NodeConfiguration;

import java.util.Collections;

@Data
@EqualsAndHashCode(callSuper = true)
public class TbUnassignEntityGroupFromEdgeNodeConfiguration extends TbAbstractEdgeEntityGroupActionNodeConfiguration implements NodeConfiguration<TbUnassignEntityGroupFromEdgeNodeConfiguration> {

    @Override
    public TbUnassignEntityGroupFromEdgeNodeConfiguration defaultConfiguration() {
        var configuration = new TbUnassignEntityGroupFromEdgeNodeConfiguration();
        configuration.setEdgeSource(EdgeSource.MESSAGE_ORIGINATOR);
        configuration.setEntityGroups(Collections.emptyList());
        return configuration;
    }

}
