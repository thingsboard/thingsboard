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
package org.thingsboard.server.dao.sql.task;

import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.JobId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.job.Job;
import org.thingsboard.server.common.data.job.JobStatus;
import org.thingsboard.server.common.data.job.JobType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.JobEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.task.JobDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.Arrays;
import java.util.UUID;

@Component
@SqlDao
@RequiredArgsConstructor
public class JpaJobDao extends JpaAbstractDao<JobEntity, Job> implements JobDao {

    private final JobRepository jobRepository;

    @Override
    public PageData<Job> findByTenantId(TenantId tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(jobRepository.findByTenantIdAndSearchText(tenantId.getId(), Strings.emptyToNull(pageLink.getTextSearch()), DaoUtil.toPageable(pageLink)));
    }

    @Override
    public boolean reportTaskSuccess(JobId jobId, int tasksCount) {
        return jobRepository.reportTaskSuccess(jobId.getId(), tasksCount);
    }

    @Override
    public boolean reportTaskFailure(JobId jobId, String taskKey, String error) {
        return jobRepository.reportTaskFailure(jobId.getId(), taskKey, error);
    }

    @Override
    public boolean existsByKeyAndStatusOneOf(String key, JobStatus... statuses) {
        return jobRepository.existsByKeyAndStatusIn(key, Arrays.stream(statuses).toList());
    }

    @Override
    public boolean existsByTenantIdAndTypeAndStatusOneOf(TenantId tenantId, JobType type, JobStatus... statuses) {
        return jobRepository.existsByTenantIdAndTypeAndStatusIn(tenantId.getId(), type, Arrays.stream(statuses).toList());
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.JOB;
    }

    @Override
    protected Class<JobEntity> getEntityClass() {
        return JobEntity.class;
    }

    @Override
    protected JpaRepository<JobEntity, UUID> getRepository() {
        return jobRepository;
    }

}
