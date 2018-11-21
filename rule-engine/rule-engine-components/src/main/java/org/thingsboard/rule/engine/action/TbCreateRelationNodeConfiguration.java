package org.thingsboard.rule.engine.action;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;

@Data
public class TbCreateRelationNodeConfiguration implements NodeConfiguration<TbCreateRelationNodeConfiguration> {

    private String direction;
    private String entityId;
    private String entityType;
    private String relationType;


    @Override
    public TbCreateRelationNodeConfiguration defaultConfiguration() {
        TbCreateRelationNodeConfiguration configuration = new TbCreateRelationNodeConfiguration();
        configuration.setDirection(EntitySearchDirection.FROM.name());
        configuration.setRelationType("Contains");
        return configuration;
    }
}
