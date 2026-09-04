// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
