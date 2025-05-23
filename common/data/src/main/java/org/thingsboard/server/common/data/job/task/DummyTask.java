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
package org.thingsboard.server.common.data.job.task;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.job.JobType;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@ToString(callSuper = true)
public class DummyTask extends Task<DummyTaskResult> {

    private int number;
    private long processingTimeMs;
    private List<String> errors; // errors for each attempt
    private boolean failAlways;

    @Override
    public DummyTaskResult toFailed(Throwable error) {
        return DummyTaskResult.failed(this, error);
    }

    @Override
    public DummyTaskResult toDiscarded() {
        return DummyTaskResult.discarded(this);
    }

    @Override
    public EntityId getEntityId() {
        return new DeviceId(UUID.randomUUID());
    }

    @Override
    public JobType getJobType() {
        return JobType.DUMMY;
    }

}
