// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data;

import org.thingsboard.server.common.data.id.TenantId;

public interface HasTenantId {

    TenantId getTenantId();
}
