package org.thingsboard.server.service.sync.vc.data.request.load;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.EntityId;

@Data
@EqualsAndHashCode(callSuper = true)
public class SingleEntityVersionLoadRequest extends VersionLoadRequest {

    private EntityId externalEntityId;

    private EntityVersionLoadConfig config;

    @Override
    public VersionLoadRequestType getType() {
        return VersionLoadRequestType.SINGLE_ENTITY;
    }

}
