// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.cache.customer;

import org.thingsboard.server.common.data.id.TenantId;

public record CustomerCacheEvictEvent(TenantId tenantId, String newTitle, String oldTitle) {
}
