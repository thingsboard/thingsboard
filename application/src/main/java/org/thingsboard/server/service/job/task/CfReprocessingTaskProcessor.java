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
package org.thingsboard.server.service.job.task;

import com.google.common.util.concurrent.SettableFuture;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.server.actors.calculatedField.CalculatedFieldReprocessingService;
import org.thingsboard.server.common.data.job.CfReprocessingTask;
import org.thingsboard.server.common.data.job.JobType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.queue.task.TaskProcessor;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class CfReprocessingTaskProcessor extends TaskProcessor<CfReprocessingTask> {

    private final CalculatedFieldReprocessingService cfReprocessingService;

    @Override
    protected void process(CfReprocessingTask task) throws Exception {
        SettableFuture<Void> future = SettableFuture.create();
        cfReprocessingService.reprocess(task, new TbCallback() {
            @Override
            public void onSuccess() {
                future.set(null);
            }

            @Override
            public void onFailure(Throwable t) {
                future.setException(t);
            }
        });
        future.get(1, TimeUnit.MINUTES);
    }

    @Override
    public JobType getJobType() {
        return JobType.CF_REPROCESSING;
    }

}
