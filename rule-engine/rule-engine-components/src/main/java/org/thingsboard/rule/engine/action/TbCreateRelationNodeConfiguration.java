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
public class TbCreateRelationNodeConfiguration extends TbAbstractRelationActionNodeConfiguration implements NodeConfiguration<TbCreateRelationNodeConfiguration> {

    private boolean createEntityIfNotExists;
    private boolean changeOriginatorToRelatedEntity;
    private boolean removeCurrentRelations;

    @Override
    public TbCreateRelationNodeConfiguration defaultConfiguration() {
        TbCreateRelationNodeConfiguration configuration = new TbCreateRelationNodeConfiguration();
        configuration.setDirection(EntitySearchDirection.FROM);
        configuration.setRelationType(EntityRelation.CONTAINS_TYPE);
        configuration.setEntityNamePattern("");
        configuration.setCreateEntityIfNotExists(false);
        configuration.setRemoveCurrentRelations(false);
        configuration.setChangeOriginatorToRelatedEntity(false);
        return configuration;
    }
}
