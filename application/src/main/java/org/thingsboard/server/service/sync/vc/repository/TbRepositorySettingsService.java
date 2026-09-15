// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.sync.vc.repository;

import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.sync.vc.RepositorySettings;

public interface TbRepositorySettingsService {

    RepositorySettings restore(TenantId tenantId, RepositorySettings versionControlSettings);

    RepositorySettings get(TenantId tenantId);

    RepositorySettings save(TenantId tenantId, RepositorySettings versionControlSettings);

    boolean delete(TenantId tenantId);

}
