// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.DomainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;

@EqualsAndHashCode(callSuper = true)
@Data
@ToString
public class Domain extends BaseData<DomainId> implements HasTenantId, HasName {

    @Schema(description = "JSON object with Tenant Id")
    private TenantId tenantId;
    @Schema(description = "Domain name. Cannot be empty", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Length(fieldName = "name")
    private String name;
    @Schema(description = "Whether OAuth2 settings are enabled or not")
    private boolean oauth2Enabled;
    @Schema(description = "Whether OAuth2 settings are enabled on Edge or not")
    private boolean propagateToEdge;

    public Domain() {
        super();
    }

    public Domain(DomainId id) {
        super(id);
    }

    public Domain(Domain domain) {
        super(domain);
        this.tenantId = domain.tenantId;
        this.name = domain.name;
        this.oauth2Enabled = domain.oauth2Enabled;
        this.propagateToEdge = domain.propagateToEdge;
    }

}
