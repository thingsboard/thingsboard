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
package org.thingsboard.server.common.data.job;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.id.EntityId;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@ToString(callSuper = true)
public class CfReprocessingTask extends Task {

    private CalculatedField calculatedField;
    private EntityId entityId;
    private long startTs;
    private long endTs;

    @Override
    public Object getKey() {
        return entityId;
    }

    @Override
    public TaskFailure toFailure(Throwable error) {
        return new CfReprocessingTaskFailure(entityId, error.getMessage());
    }

    @Override
    public JobType getJobType() {
        return JobType.CF_REPROCESSING;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    @NoArgsConstructor
    public static class CfReprocessingTaskFailure extends TaskFailure {

        private EntityId entityId;

        public CfReprocessingTaskFailure(EntityId entityId, String error) {
            super(error);
            this.entityId = entityId;
        }

        @Override
        public JobType getJobType() {
            return JobType.CF_REPROCESSING;
        }

    }

}
