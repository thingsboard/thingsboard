// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.job;

import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.JobId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.job.Job;
import org.thingsboard.server.common.data.job.JobFilter;
import org.thingsboard.server.common.data.job.JobStats;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.EntityDaoService;

public interface JobService extends EntityDaoService {

    Job saveJob(TenantId tenantId, Job job);

    Job findJobById(TenantId tenantId, JobId jobId);

    void cancelJob(TenantId tenantId, JobId jobId);

    void markAsFailed(TenantId tenantId, JobId jobId, String error);

    void processStats(TenantId tenantId, JobId jobId, JobStats jobStats);

    PageData<Job> findJobsByFilter(TenantId tenantId, JobFilter filter, PageLink pageLink);

    Job findLatestJobByKey(TenantId tenantId, String key);

    void deleteJob(TenantId tenantId, JobId jobId);

    int deleteJobsByEntityId(TenantId tenantId, EntityId entityId);

}
