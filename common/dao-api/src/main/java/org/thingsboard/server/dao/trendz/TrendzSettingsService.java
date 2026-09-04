// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.trendz;

import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.trendz.TrendzSettings;

public interface TrendzSettingsService {

    void saveTrendzSettings(TenantId tenantId, TrendzSettings settings);

    TrendzSettings findTrendzSettings(TenantId tenantId);

    void deleteTrendzSettings(TenantId tenantId);

}
