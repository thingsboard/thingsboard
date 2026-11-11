// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.sync.vc.data;

import lombok.AccessLevel;
import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.sync.ie.EntityExportSettings;
import org.thingsboard.server.common.data.sync.vc.request.create.EntityTypeVersionCreateConfig;
import org.thingsboard.server.common.data.sync.vc.request.create.SyncStrategy;
import org.thingsboard.server.common.data.sync.vc.request.create.VersionCreateRequest;

import java.util.LinkedList;
import java.util.Queue;

public class EntityTypeExportCtx extends EntitiesExportCtx<VersionCreateRequest> {

    @Getter
    private final EntityType entityType;
    @Getter
    private final boolean overwrite;
    @Getter
    private final EntityExportSettings settings;
    @Getter(value = AccessLevel.PRIVATE)
    private final Queue<EntityTypeExportTask> tasks;

    public EntityTypeExportCtx(EntitiesExportCtx<?> parent, EntityTypeVersionCreateConfig config, SyncStrategy defaultSyncStrategy, EntityType entityType) {
        super(parent);
        this.entityType = entityType;
        this.settings = EntityExportSettings.builder()
                .exportRelations(config.isSaveRelations())
                .exportAttributes(config.isSaveAttributes())
                .exportCredentials(config.isSaveCredentials())
                .exportCalculatedFields(config.isSaveCalculatedFields())
                .exportGroupEntities(config.isSaveGroupEntities())
                .exportPermissions(config.isSavePermissions())
                .build();
        this.overwrite = ObjectUtils.defaultIfNull(config.getSyncStrategy(), defaultSyncStrategy) == SyncStrategy.OVERWRITE;
        this.tasks = new LinkedList<>();
    }

    public void addTask(EntityTypeExportTask task) {
        tasks.add(task);
    }

    public EntityTypeExportTask pollTask() {
        return tasks.poll();
    }

}
