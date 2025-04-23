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

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.JobId;
import org.thingsboard.server.common.data.id.TenantId;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Job extends BaseData<JobId> implements HasTenantId {

    private TenantId tenantId;
    private JobType type;
    private String key;
    private JobStatus status;
    private JobConfiguration configuration;
    private JobResult result;

    @Builder
    public Job(TenantId tenantId, JobType type, String key, JobConfiguration configuration) {
        this.tenantId = tenantId;
        this.type = type;
        this.key = key;
        this.configuration = configuration;
        this.status = JobStatus.PENDING;
        this.result = switch (type) {
            case CF_REPROCESSING -> new CfReprocessingJobResult();
        };
    }

    @SuppressWarnings("unchecked")
    public <C extends JobConfiguration> C getConfiguration() {
        return (C) configuration;
    }

}
