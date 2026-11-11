// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.transform;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.rule.engine.api.NodeConfiguration;

@Data
@EqualsAndHashCode(callSuper = true)
public class TbDuplicateMsgToRelatedNodeConfiguration extends TbAbstractTransformNodeConfigurationWithRelationQuery implements NodeConfiguration<TbDuplicateMsgToRelatedNodeConfiguration> {

    @Override
    public TbDuplicateMsgToRelatedNodeConfiguration defaultConfiguration() {
        var configuration = new TbDuplicateMsgToRelatedNodeConfiguration();
        configuration.setRelationsQuery(getDefaultRelationQuery());
        return configuration;
    }

}
