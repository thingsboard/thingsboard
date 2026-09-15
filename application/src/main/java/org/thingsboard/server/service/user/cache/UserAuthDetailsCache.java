// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.user.cache;

import org.thingsboard.server.common.data.UserAuthDetails;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;

public interface UserAuthDetailsCache {

    UserAuthDetails getUserAuthDetails(TenantId tenantId, UserId userId);

}
