// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data;

import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;

public interface ExportableNoTenantIdEntity<I extends EntityId> extends ExportableEntity<I> {

    default void setTenantId(TenantId tenantId){};

}
