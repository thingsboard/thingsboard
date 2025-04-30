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
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.job.CfReprocessingJobConfiguration;
import org.thingsboard.server.common.data.job.CfReprocessingTask;
import org.thingsboard.server.common.data.job.CfReprocessingTask.CfReprocessingTaskFailure;
import org.thingsboard.server.common.data.job.Job;
import org.thingsboard.server.common.data.job.JobType;
import org.thingsboard.server.common.data.job.Task;
import org.thingsboard.server.common.data.job.TaskFailure;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.cf.CalculatedFieldService;
import org.thingsboard.server.dao.device.DeviceService;

import java.util.List;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class CfReprocessingJobProcessor implements JobProcessor {

    private final CalculatedFieldService calculatedFieldService;
    private final DeviceService deviceService;
    private final AssetService assetService;

    @Override
    public int process(Job job, Consumer<Task> taskConsumer) throws Exception {
        CfReprocessingJobConfiguration configuration = job.getConfiguration();

        CalculatedField calculatedField = calculatedFieldService.findById(job.getTenantId(), configuration.getCalculatedFieldId());
        EntityId cfEntityId = calculatedField.getEntityId();

        int tasksCount = 0;
        if (cfEntityId.getEntityType().isOneOf(EntityType.DEVICE, EntityType.ASSET)) {
            taskConsumer.accept(createTask(job, configuration, calculatedField, cfEntityId));
            tasksCount++;
        } else {
            PageDataIterable<? extends EntityId> entities;
            if (cfEntityId.getEntityType() == EntityType.DEVICE_PROFILE) {
                entities = new PageDataIterable<>(pageLink -> deviceService.findDeviceIdsByTenantIdAndDeviceProfileId(job.getTenantId(), (DeviceProfileId) cfEntityId, pageLink), 512);
            } else if (cfEntityId.getEntityType() == EntityType.ASSET_PROFILE) {
                entities = new PageDataIterable<>(pageLink -> assetService.findAssetIdsByTenantIdAndAssetProfileId(job.getTenantId(), (AssetProfileId) cfEntityId, pageLink), 512);
            } else {
                throw new IllegalArgumentException("Unsupported CF entity type " + cfEntityId.getEntityType());
            }
            for (EntityId entityId : entities) {
                taskConsumer.accept(createTask(job, configuration, calculatedField, entityId));
                tasksCount++;
            }
        }
        return tasksCount;
    }

    @Override
    public void reprocess(Job job, List<TaskFailure> failures, Consumer<Task> taskConsumer) throws Exception {
        CfReprocessingJobConfiguration configuration = job.getConfiguration();
        CalculatedField calculatedField = calculatedFieldService.findById(job.getTenantId(), configuration.getCalculatedFieldId());

        for (TaskFailure failure : failures) {
            CfReprocessingTaskFailure taskFailure = (CfReprocessingTaskFailure) failure;
            EntityId entityId = taskFailure.getEntityId();
            taskConsumer.accept(createTask(job, job.getConfiguration(), calculatedField, entityId));
        }
    }

    private Task createTask(Job job, CfReprocessingJobConfiguration configuration, CalculatedField calculatedField, EntityId entityId) {
        return CfReprocessingTask.builder()
                .tenantId(job.getTenantId())
                .jobId(job.getId())
                .retries(2) // 3 attempts in total
                .calculatedField(calculatedField)
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
