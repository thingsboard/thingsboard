/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.dao.device;

import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.device.provision.ProvisionProfile;

/**
 * The Interface ProvisionProfileDao.
 */
public interface ProvisionProfileDao extends Dao<ProvisionProfile> {

    /**
     * Save or update provision profile object
     *
     * @param tenantId the profile tenant id
     * @param provisionProfile the provision profile object
     * @return saved provision profile object
     */
    ProvisionProfile save(TenantId tenantId, ProvisionProfile provisionProfile);

    /**
     * Find provision profile by key.
     *
     * @param key the profile key
     * @return the provision profile object
     */
    ProvisionProfile findByKey(TenantId tenantId, String key);

}
