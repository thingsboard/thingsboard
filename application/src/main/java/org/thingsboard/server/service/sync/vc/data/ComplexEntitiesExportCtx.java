// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.sync.vc.data;

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.sync.ie.EntityExportSettings;
import org.thingsboard.server.common.data.sync.vc.request.create.ComplexVersionCreateRequest;

import java.util.HashMap;
import java.util.Map;

public class ComplexEntitiesExportCtx extends EntitiesExportCtx<ComplexVersionCreateRequest> {

    private final Map<EntityType, EntityExportSettings> settings = new HashMap<>();

    public ComplexEntitiesExportCtx(User user, CommitGitRequest commit, ComplexVersionCreateRequest request) {
        super(user, commit, request);
        request.getEntityTypes().forEach((type, config) -> settings.put(type, buildExportSettings(config)));
    }

    public EntityExportSettings getSettings(EntityType entityType) {
        return settings.get(entityType);
    }

    @Override
    public EntityExportSettings getSettings() {
        throw new RuntimeException("Not implemented. Use EntityTypeExportCtx instead!");
    }
}
