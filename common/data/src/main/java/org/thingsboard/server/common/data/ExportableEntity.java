// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data;

import io.swagger.v3.oas.annotations.media.Schema;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;

public interface ExportableEntity<I extends EntityId> extends HasId<I>, HasName {

    void setId(I id);

    @Schema(description = "JSON object with External Id from the VCS", accessMode = Schema.AccessMode.READ_ONLY, hidden = true)
    I getExternalId();

    void setExternalId(I externalId);

    long getCreatedTime();

    void setCreatedTime(long createdTime);

    void setTenantId(TenantId tenantId);

}
