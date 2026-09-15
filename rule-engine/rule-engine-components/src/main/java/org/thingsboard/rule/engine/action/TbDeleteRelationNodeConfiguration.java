// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.action;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;

@Data
@EqualsAndHashCode(callSuper = true)
public class TbDeleteRelationNodeConfiguration extends TbAbstractRelationActionNodeConfiguration implements NodeConfiguration<TbDeleteRelationNodeConfiguration> {

    private boolean deleteForSingleEntity;

    @Override
    public TbDeleteRelationNodeConfiguration defaultConfiguration() {
        TbDeleteRelationNodeConfiguration configuration = new TbDeleteRelationNodeConfiguration();
        configuration.setDeleteForSingleEntity(false);
        configuration.setDirection(EntitySearchDirection.FROM);
        configuration.setRelationType(EntityRelation.CONTAINS_TYPE);
        configuration.setEntityNamePattern("");
        return configuration;
    }
}
