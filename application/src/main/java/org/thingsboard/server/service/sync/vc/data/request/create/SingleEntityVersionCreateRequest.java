package org.thingsboard.server.service.sync.vc.data.request.create;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.EntityId;

@Data
@EqualsAndHashCode(callSuper = true)
public class SingleEntityVersionCreateRequest extends VersionCreateRequest {

    private EntityId entityId;
    private VersionCreateConfig config;

    @Override
    public VersionCreateRequestType getType() {
        return VersionCreateRequestType.SINGLE_ENTITY;
    }

}
