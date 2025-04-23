/**
 * Copyright © 2016-2025 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.job;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ProfileEntityIdInfo;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.job.CfReprocessingJobConfiguration;
import org.thingsboard.server.common.data.job.CfReprocessingTask;
import org.thingsboard.server.common.data.job.Job;
import org.thingsboard.server.common.data.job.JobType;
import org.thingsboard.server.common.data.job.Task;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.device.DeviceService;

import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class CfReprocessingJobProcessor extends JobProcessor {

    private final DeviceService deviceService;
    private final AssetService assetService;

    @Override
    public void process(Job job, Consumer<Task> taskConsumer) {
        CfReprocessingJobConfiguration configuration = job.getConfiguration();

        CalculatedField calculatedField = configuration.getCalculatedField();
        EntityId entityId = calculatedField.getEntityId();

        if (entityId.getEntityType().isOneOf(EntityType.DEVICE, EntityType.ASSET)) {
            taskConsumer.accept(createTask(job, configuration, entityId));
        } else {
            PageDataIterable<ProfileEntityIdInfo> entities;
            if (entityId.getEntityType() == EntityType.DEVICE_PROFILE) {
                entities = new PageDataIterable<>(pageLink -> deviceService.findProfileEntityIdInfosByTenantId(job.getTenantId(), pageLink), 512);
            } else if (entityId.getEntityType() == EntityType.ASSET_PROFILE) {
                entities = new PageDataIterable<>(pageLink -> assetService.findProfileEntityIdInfosByTenantId(job.getTenantId(), pageLink), 512);
            } else {
                throw new IllegalArgumentException("Unsupported CF entity type " + entityId.getEntityType());
            }
            entities.forEach(device -> {
                taskConsumer.accept(createTask(job, configuration, device.getEntityId()));
            });
        }
    }

    private Task createTask(Job job, CfReprocessingJobConfiguration configuration, EntityId entityId) {
        return CfReprocessingTask.builder()
                .tenantId(job.getTenantId())
                .jobId(job.getId())
                .key(entityId.getEntityType().getNormalName() + " " + entityId.getId())
                .calculatedField(configuration.getCalculatedField())
                .entityId(entityId)
                .startTs(configuration.getStartTs())
                .endTs(configuration.getEndTs())
                .build();
    }

    @Override
    public JobType getType() {
        return JobType.CF_REPROCESSING;
    }

}
