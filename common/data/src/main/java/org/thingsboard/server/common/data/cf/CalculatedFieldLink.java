// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.cf;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.CalculatedFieldLinkId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;

@Schema
@Data
@EqualsAndHashCode(callSuper = true)
public class CalculatedFieldLink extends BaseData<CalculatedFieldLinkId> {

    private static final long serialVersionUID = 6492846246722091530L;

    private TenantId tenantId;
    private EntityId entityId;

    @Schema(description = "JSON object with the Calculated Field Id. ", accessMode = Schema.AccessMode.READ_ONLY)
    private CalculatedFieldId calculatedFieldId;

    public CalculatedFieldLink() {
        super();
    }

    public CalculatedFieldLink(CalculatedFieldLinkId id) {
        super(id);
    }

    public CalculatedFieldLink(TenantId tenantId, EntityId entityId, CalculatedFieldId calculatedFieldId) {
        this.tenantId = tenantId;
        this.entityId = entityId;
        this.calculatedFieldId = calculatedFieldId;
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("CalculatedFieldLink[")
                .append("tenantId=").append(tenantId)
                .append(", entityId=").append(entityId)
                .append(", calculatedFieldId=").append(calculatedFieldId)
                .append(", createdTime=").append(createdTime)
                .append(", id=").append(id).append(']')
                .toString();
    }

}
