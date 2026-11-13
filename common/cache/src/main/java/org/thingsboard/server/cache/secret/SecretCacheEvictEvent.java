// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.cache.secret;

import org.thingsboard.server.common.data.id.TenantId;

public record SecretCacheEvictEvent(TenantId tenantId, String name) {

}
