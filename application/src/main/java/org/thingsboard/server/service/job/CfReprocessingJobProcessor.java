// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.job;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.job.CfReprocessingJobConfiguration;
import org.thingsboard.server.common.data.job.Job;
import org.thingsboard.server.common.data.job.JobType;
import org.thingsboard.server.common.data.job.task.CfReprocessingTask;
import org.thingsboard.server.common.data.job.task.CfReprocessingTaskResult;
import org.thingsboard.server.common.data.job.task.CfReprocessingTaskResult.CfReprocessingTaskFailure;
import org.thingsboard.server.common.data.job.task.Task;
import org.thingsboard.server.common.data.job.task.TaskResult;
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
    public int process(Job job, Consumer<Task<?>> taskConsumer) throws Exception {
        CfReprocessingJobConfiguration configuration = job.getConfiguration();

        TenantId tenantId = job.getTenantId();
        CalculatedField calculatedField = calculatedFieldService.findById(tenantId, configuration.getCalculatedFieldId());
        EntityId cfEntityId = calculatedField.getEntityId();

        int tasksCount = 0;
        Iterable<EntityInfo> entities;
        switch (cfEntityId.getEntityType()) {
            case DEVICE -> {
                entities = List.of(deviceService.findDeviceEntityInfoById(tenantId, (DeviceId) cfEntityId));
            }
            case ASSET -> {
                entities = List.of(assetService.findAssetEntityInfoById(tenantId, (AssetId) cfEntityId));
            }
            case DEVICE_PROFILE -> {
                entities = new PageDataIterable<>(pageLink -> deviceService.findDeviceEntityInfosByTenantIdAndDeviceProfileId(tenantId, (DeviceProfileId) cfEntityId, pageLink), 512);
            }
            case ASSET_PROFILE -> {
                entities = new PageDataIterable<>(pageLink -> assetService.findAssetEntityInfosByTenantIdAndAssetProfileId(tenantId, (AssetProfileId) cfEntityId, pageLink), 512);
            }
            default -> {
                throw new IllegalArgumentException("Unsupported CF entity type " + cfEntityId.getEntityType());
            }
        }

        for (EntityInfo entityInfo : entities) {
            taskConsumer.accept(createTask(job, configuration, calculatedField, entityInfo));
            tasksCount++;
        }
        return tasksCount;
    }

    @Override
    public void reprocess(Job job, List<TaskResult> taskFailures, Consumer<Task<?>> taskConsumer) throws Exception {
        CfReprocessingJobConfiguration configuration = job.getConfiguration();
        CalculatedField calculatedField = calculatedFieldService.findById(job.getTenantId(), configuration.getCalculatedFieldId());

        for (TaskResult taskFailure : taskFailures) {
            CfReprocessingTaskFailure failure = ((CfReprocessingTaskResult) taskFailure).getFailure();
            EntityInfo entityInfo = failure.getEntityInfo();
            taskConsumer.accept(createTask(job, job.getConfiguration(), calculatedField, entityInfo));
        }
    }

    private CfReprocessingTask createTask(Job job, CfReprocessingJobConfiguration configuration, CalculatedField calculatedField, EntityInfo entityInfo) {
        return CfReprocessingTask.builder()
                .tenantId(job.getTenantId())
                .jobId(job.getId())
                .key(configuration.getTasksKey())
                .retries(0) // only 1 attempt
                .calculatedField(calculatedField)
                .entityInfo(entityInfo)
                .startTs(configuration.getStartTs())
                .endTs(configuration.getEndTs())
                .build();
    }

    @Override
    public JobType getType() {
        return JobType.CF_REPROCESSING;
    }

}
