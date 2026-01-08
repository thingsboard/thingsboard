/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.dao.job;

import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.JobId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.job.Job;
import org.thingsboard.server.common.data.job.JobFilter;
import org.thingsboard.server.common.data.job.JobStatus;
import org.thingsboard.server.common.data.job.JobType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;

public interface JobDao extends Dao<Job> {

    PageData<Job> findByTenantIdAndFilter(TenantId tenantId, JobFilter filter, PageLink pageLink);

    Job findByIdForUpdate(TenantId tenantId, JobId jobId);

    Job findLatestByTenantIdAndKey(TenantId tenantId, String key);

    boolean existsByTenantAndKeyAndStatusOneOf(TenantId tenantId, String key, JobStatus... statuses);

    boolean existsByTenantIdAndTypeAndStatusOneOf(TenantId tenantId, JobType type, JobStatus... statuses);

    boolean existsByTenantIdAndEntityIdAndStatusOneOf(TenantId tenantId, EntityId entityId, JobStatus... statuses);

    Job findOldestByTenantIdAndTypeAndStatusForUpdate(TenantId tenantId, JobType type, JobStatus status);

    void removeByTenantId(TenantId tenantId);

    int removeByEntityId(TenantId tenantId, EntityId entityId);

}
