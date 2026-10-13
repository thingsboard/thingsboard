// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.job.task;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.job.JobType;
import org.thingsboard.server.common.data.job.task.CfReprocessingTask;
import org.thingsboard.server.common.data.job.task.CfReprocessingTaskResult;
import org.thingsboard.server.queue.task.TaskProcessor;
import org.thingsboard.server.queue.util.TbRuleEngineComponent;
import org.thingsboard.server.service.cf.CalculatedFieldReprocessingService;

@TbRuleEngineComponent
@Component
@RequiredArgsConstructor
public class CfReprocessingTaskProcessor extends TaskProcessor<CfReprocessingTask, CfReprocessingTaskResult> {

    @Autowired
    @Lazy
    private CalculatedFieldReprocessingService cfReprocessingService;

    @Value("${queue.calculated_fields.reprocessing_timeout:300000}")
    private int timeoutMs;

    @Override
    public CfReprocessingTaskResult process(CfReprocessingTask task) throws Exception {
        cfReprocessingService.reprocess(task);
        return CfReprocessingTaskResult.success(task);
    }

    @Override
    public long getProcessingTimeout(CfReprocessingTask task) {
        return timeoutMs;
    }

    @Override
    public JobType getJobType() {
        return JobType.CF_REPROCESSING;
    }

}
