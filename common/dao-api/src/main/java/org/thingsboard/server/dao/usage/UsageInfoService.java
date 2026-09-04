// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.usage;

import org.thingsboard.server.common.data.UsageInfo;
import org.thingsboard.server.common.data.id.TenantId;

public interface UsageInfoService {

    UsageInfo getUsageInfo(TenantId tenantId);

}
