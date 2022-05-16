package org.thingsboard.server.service.sync.vc.data.request.create;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.EntityId;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class EntityListVersionCreateRequest extends VersionCreateRequest {

    private List<EntityId> entitiesIds;
    private MultipleEntitiesVersionCreateConfig config;

    @Override
    public VersionCreateRequestType getType() {
        return VersionCreateRequestType.ENTITY_LIST;
    }

}
