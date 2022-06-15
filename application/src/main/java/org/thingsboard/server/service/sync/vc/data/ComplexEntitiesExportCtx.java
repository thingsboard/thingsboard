package org.thingsboard.server.service.sync.vc.data;

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.sync.ie.EntityExportSettings;
import org.thingsboard.server.common.data.sync.vc.request.create.ComplexVersionCreateRequest;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.HashMap;
import java.util.Map;

public class ComplexEntitiesExportCtx extends EntitiesExportCtx<ComplexVersionCreateRequest> {

    private final Map<EntityType, EntityExportSettings> settings = new HashMap<>();

    public ComplexEntitiesExportCtx(SecurityUser user, CommitGitRequest commit, ComplexVersionCreateRequest request) {
        super(user, commit, request);
        request.getEntityTypes().forEach((type, config) -> settings.put(type, buildExportSettings(config)));
    }

    public EntityExportSettings getSettings(EntityType entityType) {
        return settings.get(entityType);
    }
}
