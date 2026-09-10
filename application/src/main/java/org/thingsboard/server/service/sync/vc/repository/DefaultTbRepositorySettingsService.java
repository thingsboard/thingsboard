// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.sync.vc.repository;

import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.TbTransactionalCache;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.sync.vc.RepositoryAuthMethod;
import org.thingsboard.server.common.data.sync.vc.RepositorySettings;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.vc.TbAbstractVersionControlSettingsService;

@Service
@TbCoreComponent
public class DefaultTbRepositorySettingsService extends TbAbstractVersionControlSettingsService<RepositorySettings> implements TbRepositorySettingsService {

    public static final String SETTINGS_KEY = "entitiesVersionControl";

    public DefaultTbRepositorySettingsService(AdminSettingsService adminSettingsService, TbTransactionalCache<TenantId, RepositorySettings> cache) {
        super(adminSettingsService, cache, RepositorySettings.class, SETTINGS_KEY);
    }

    @Override
    public RepositorySettings restore(TenantId tenantId, RepositorySettings settings) {
        RepositorySettings storedSettings = get(tenantId);
        if (storedSettings != null) {
            RepositoryAuthMethod authMethod = settings.getAuthMethod();
            if (RepositoryAuthMethod.USERNAME_PASSWORD.equals(authMethod) && settings.getPassword() == null) {
                settings.setPassword(storedSettings.getPassword());
            } else if (RepositoryAuthMethod.PRIVATE_KEY.equals(authMethod) && settings.getPrivateKey() == null) {
                settings.setPrivateKey(storedSettings.getPrivateKey());
                if (settings.getPrivateKeyPassword() == null) {
                    settings.setPrivateKeyPassword(storedSettings.getPrivateKeyPassword());
                }
            }
        }
        return settings;
    }

    @Override
    public RepositorySettings get(TenantId tenantId) {
        RepositorySettings settings = super.get(tenantId);
        if (settings != null) {
            settings = new RepositorySettings(settings);
        }
        return settings;
    }

}
