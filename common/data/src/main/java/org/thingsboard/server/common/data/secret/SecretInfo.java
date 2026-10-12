// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.secret;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasOwnerId;
import org.thingsboard.server.common.data.SecretType;
import org.thingsboard.server.common.data.TenantEntity;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.SecretId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

import java.io.Serial;

@Schema
@Data
@EqualsAndHashCode(callSuper = true)
public class SecretInfo extends BaseData<SecretId> implements TenantEntity, HasName, HasOwnerId {

    @Serial
    private static final long serialVersionUID = 4356095580465337566L;

    @Schema(description = "JSON object with Tenant Id. Tenant Id of the secret cannot be changed.", accessMode = Schema.AccessMode.READ_ONLY)
    private TenantId tenantId;

    @Schema(description = "JSON object with Customer Id. NULL_UUID sentinel indicates the secret is owned by the tenant (or sysadmin if tenant is SYS_TENANT_ID). Cannot be changed after creation.", accessMode = Schema.AccessMode.READ_ONLY)
    private CustomerId customerId = new CustomerId(EntityId.NULL_UUID);

    @NoXss
    @NotBlank
    @Length(fieldName = "name")
    @Schema(description = "Secret name", requiredMode = Schema.RequiredMode.REQUIRED, example = "Secret")
    private String name;

    @NotNull
    @Schema(description = "Secret type.", requiredMode = RequiredMode.REQUIRED, example = "TEXT")
    private SecretType type;

    @NoXss
    @Length(fieldName = "description")
    @Schema(description = "Secret description.", example = "Secret description")
    private String description;

    @Schema(description = "JSON object with the Secret Id. " +
            "Specify this field to update the Secret. " +
            "Referencing non-existing Secret Id will cause error. " +
            "Omit this field to create new Secret.")
    @Override
    public SecretId getId() {
        return super.getId();
    }

    public SecretInfo() {
        super();
    }

    public SecretInfo(SecretId id) {
        super(id);
    }

    public SecretInfo(SecretInfo secretInfo) {
        super(secretInfo);
        this.tenantId = secretInfo.getTenantId();
        this.customerId = secretInfo.getCustomerId();
        this.name = secretInfo.getName();
        this.type = secretInfo.getType();
        this.description = secretInfo.getDescription();
    }

    @JsonIgnore
    @Override
    public EntityId getOwnerId() {
        return customerId != null && !customerId.isNullUid() ? customerId : tenantId;
    }

    @JsonIgnore
    @Override
    public void setOwnerId(EntityId entityId) {
        if (EntityType.CUSTOMER.equals(entityId.getEntityType())) {
            this.customerId = new CustomerId(entityId.getId());
        } else {
            this.customerId = new CustomerId(EntityId.NULL_UUID);
        }
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.SECRET;
    }

}
