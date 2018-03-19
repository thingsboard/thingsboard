package org.thingsboard.rule.engine.metadata;

import lombok.Data;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;

@Data
public class TbGetRelatedAttrNodeConfiguration {

    private String relationType;
    private EntitySearchDirection direction;
}
