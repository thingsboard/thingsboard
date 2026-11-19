// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.extension;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.thingsboard.server.common.data.BaseDataWithAdditionalInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasVersion;
import org.thingsboard.server.common.data.TenantEntity;
import org.thingsboard.server.common.data.id.ExtensionId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

@Data
@Schema
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Extension extends BaseDataWithAdditionalInfo<ExtensionId> implements TenantEntity, HasName, HasVersion, ExportableEntity<ExtensionId> {

    private static final long serialVersionUID = 1L;

    @Schema(description = "JSON object with Tenant Id.", accessMode = Schema.AccessMode.READ_ONLY)
    private TenantId tenantId;

    @NoXss
    @Length(fieldName = "name")
    @Schema(description = "Human-readable name, unique within the tenant.", requiredMode = Schema.RequiredMode.REQUIRED, example = "Acme Billing")
    private String name;

    @Schema(description = "System-wide-unique routing slug, e.g. acme-billing-9f3a. " +
            "Immutable; server-allocated as <slugBase>-<4hex>.", accessMode = Schema.AccessMode.READ_ONLY, example = "acme-billing-9f3a")
    private String slug;

    @Schema(description = "Lifecycle state.", accessMode = Schema.AccessMode.READ_ONLY)
    private ExtensionState state;

    @Valid
    @Schema(description = "Container / deployment spec.")
    private ExtensionContainerSpec containerSpec;

    @Schema(description = "Last error message; populated when state=FAILED/DEGRADED.", accessMode = Schema.AccessMode.READ_ONLY)
    private String lastError;

    @Schema(description = "Timestamp (ms) of the last error.", accessMode = Schema.AccessMode.READ_ONLY)
    private Long lastErrorAt;

    @Schema(description = "Optimistic-locking version; increments on every change.", accessMode = Schema.AccessMode.READ_ONLY, defaultValue = "1")
    private Long version;

    private ExtensionId externalId;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Schema(description = "Optional base for slug allocation on create; defaults to slugify(name). " +
            "Write-only: consumed to allocate the immutable slug, never stored or returned.",
            accessMode = Schema.AccessMode.WRITE_ONLY, example = "acme-billing")
    private transient String slugBase;

    public Extension() {
        super();
    }

    public Extension(ExtensionId id) {
        super(id);
    }

    public Extension(Extension extension) {
        super(extension);
        this.tenantId = extension.getTenantId();
        this.name = extension.getName();
        this.slug = extension.getSlug();
        this.state = extension.getState();
        this.containerSpec = extension.getContainerSpec();
        this.lastError = extension.getLastError();
        this.lastErrorAt = extension.getLastErrorAt();
        this.version = extension.getVersion();
        this.externalId = extension.getExternalId() == null ? null : new ExtensionId(extension.getExternalId().getId());
    }

    @Schema(description = "JSON object with the Extension Id. Specify this field to update the Extension. " +
            "Omit this field to create new Extension.", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public ExtensionId getId() {
        return super.getId();
    }

    @Schema(description = "Timestamp of the extension creation, in milliseconds.", example = "1609459200000", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    @JsonIgnore
    @Override
    public EntityType getEntityType() {
        return EntityType.EXTENSION;
    }

}
