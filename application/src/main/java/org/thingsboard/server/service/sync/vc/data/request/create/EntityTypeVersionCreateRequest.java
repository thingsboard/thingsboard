package org.thingsboard.server.service.sync.vc.data.request.create;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.EntityType;

import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class EntityTypeVersionCreateRequest extends VersionCreateRequest {

    private Map<EntityType, MultipleEntitiesVersionCreateConfig> entityTypes;

    @Override
    public VersionCreateRequestType getType() {
        return VersionCreateRequestType.ENTITY_TYPE;
    }

}
