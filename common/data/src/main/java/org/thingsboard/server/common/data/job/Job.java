// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.job;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.JobId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Job extends BaseData<JobId> implements HasTenantId {

    @NotNull
    private TenantId tenantId;
    @NotNull
    private JobType type;
    @NotBlank
    private String key;
    @NotNull
    private EntityId entityId;
    private String entityName; // read-only
    @NotNull
    private JobStatus status;
    @NotNull
    @Valid
    private JobConfiguration configuration;
    @NotNull
    private JobResult result;

    public static final Set<EntityType> SUPPORTED_ENTITY_TYPES = Set.of(
            EntityType.DEVICE, EntityType.ASSET, EntityType.DEVICE_PROFILE, EntityType.ASSET_PROFILE
    );

    @Builder(toBuilder = true)
    public Job(TenantId tenantId, JobType type, String key, EntityId entityId, JobConfiguration configuration) {
        this.tenantId = tenantId;
        this.type = type;
        this.key = key;
        this.entityId = entityId;
        this.configuration = configuration;
        this.configuration.setTasksKey(UUID.randomUUID().toString());
        presetResult();
    }

    public void presetResult() {
        this.result = switch (type) {
            case DUMMY -> new DummyJobResult();
        };
    }

    @SuppressWarnings("unchecked")
    public <C extends JobConfiguration> C getConfiguration() {
        return (C) configuration;
    }

}
