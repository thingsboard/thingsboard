// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data;

import org.thingsboard.server.common.data.id.TenantId;

public interface HasTenantId {

    TenantId getTenantId();

    void setTenantId(TenantId tenantId);

}
