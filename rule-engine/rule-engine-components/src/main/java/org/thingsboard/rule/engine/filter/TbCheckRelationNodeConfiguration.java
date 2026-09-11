// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.filter;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;

/**
 * Created by ashvayka on 19.01.18.
 */
@Data
public class TbCheckRelationNodeConfiguration implements NodeConfiguration<TbCheckRelationNodeConfiguration> {

    private String direction;
    private String entityId;
    private String entityType;
    private String relationType;
    private boolean checkForSingleEntity;

    @Override
    public TbCheckRelationNodeConfiguration defaultConfiguration() {
        var configuration = new TbCheckRelationNodeConfiguration();
        configuration.setDirection(EntitySearchDirection.FROM.name());
        configuration.setRelationType(EntityRelation.CONTAINS_TYPE);
        configuration.setCheckForSingleEntity(true);
        return configuration;
    }
}
