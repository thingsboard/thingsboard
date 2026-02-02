/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.sync.vc.autocommit;

import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.TbTransactionalCache;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.sync.vc.AutoCommitSettings;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.vc.TbAbstractVersionControlSettingsService;

@Service
@TbCoreComponent
public class DefaultTbAutoCommitSettingsService extends TbAbstractVersionControlSettingsService<AutoCommitSettings> implements TbAutoCommitSettingsService {

    public static final String SETTINGS_KEY = "autoCommitSettings";

    public DefaultTbAutoCommitSettingsService(AdminSettingsService adminSettingsService, TbTransactionalCache<TenantId, AutoCommitSettings> cache) {
        super(adminSettingsService, cache, AutoCommitSettings.class, SETTINGS_KEY);
    }

}
