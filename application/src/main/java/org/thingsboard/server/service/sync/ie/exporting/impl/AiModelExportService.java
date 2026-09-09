// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.sync.ie.exporting.impl;

import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ai.AiModel;
import org.thingsboard.server.common.data.id.AiModelId;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.Set;

@Service
@TbCoreComponent
class AiModelExportService extends BaseEntityExportService<AiModelId, AiModel, EntityExportData<AiModel>> {

    @Override
    public Set<EntityType> getSupportedEntityTypes() {
        return Set.of(EntityType.AI_MODEL);
    }

}
