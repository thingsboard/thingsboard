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
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.JobId;
import org.thingsboard.server.common.data.job.Job;
import org.thingsboard.server.common.data.job.JobConfiguration;
import org.thingsboard.server.common.data.job.JobResult;
import org.thingsboard.server.common.data.job.JobStatus;
import org.thingsboard.server.common.data.job.JobType;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Entity
@Table(name = ModelConstants.JOB_TABLE_NAME)
public class JobEntity extends BaseSqlEntity<Job> {

    @Column(name = ModelConstants.TENANT_ID_PROPERTY, nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.JOB_TYPE_PROPERTY, nullable = false)
    private JobType type;

    @Column(name = ModelConstants.JOB_KEY_PROPERTY, nullable = false)
    private String key;

    @Column(name = ModelConstants.JOB_ENTITY_ID_PROPERTY, nullable = false)
    private UUID entityId;

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.JOB_ENTITY_TYPE_PROPERTY, nullable = false)
    private EntityType entityType;

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.JOB_STATUS_PROPERTY, nullable = false)
    private JobStatus status;

    @Convert(converter = JsonConverter.class)
    @Column(name = ModelConstants.JOB_CONFIGURATION_PROPERTY, nullable = false)
    private JsonNode configuration;

    @Convert(converter = JsonConverter.class)
    @Column(name = ModelConstants.JOB_RESULT_PROPERTY)
    private JsonNode result;

    public JobEntity(Job job) {
        super(job);
        this.tenantId = getTenantUuid(job.getTenantId());
        this.type = job.getType();
        this.key = job.getKey();
        this.entityId = job.getEntityId().getId();
        this.entityType = job.getEntityId().getEntityType();
        this.status = job.getStatus();
        this.configuration = toJson(job.getConfiguration());
        this.result = toJson(job.getResult());
    }

    @Override
    public Job toData() {
        Job job = new Job();
        job.setId(new JobId(id));
        job.setCreatedTime(createdTime);
        job.setTenantId(getTenantId(tenantId));
        job.setType(type);
        job.setKey(key);
        job.setEntityId(EntityIdFactory.getByTypeAndUuid(entityType, entityId));
        job.setStatus(status);
        job.setConfiguration(fromJson(configuration, JobConfiguration.class));
        job.setResult(fromJson(result, JobResult.class));
        return job;
    }

}
