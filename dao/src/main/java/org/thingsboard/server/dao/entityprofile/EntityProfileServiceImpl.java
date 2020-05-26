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
package org.thingsboard.server.dao.entityprofile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.entityprofile.EntityProfile;
import org.thingsboard.server.common.data.id.EntityProfileId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EntityProfileServiceImpl implements EntityProfileService {
    private final EntityProfileDao dao;

    @Override
    public List<EntityProfile> findAll(TenantId tenantId) {
        log.trace("findAll for tenantId = {}", tenantId);
        return dao.find(tenantId);
    }

    @Override
    public EntityProfile findById(TenantId tenantId, EntityProfileId id) {
        log.trace("findById for tenantId = {} and id = {}", tenantId, id);
        return dao.findById(tenantId, id);
    }

    @Override
    public EntityProfile save(TenantId tenantId, EntityProfile entityProfile) {
        log.trace("save for tenantId = {} and entityProfile = {}", tenantId, entityProfile);
        return dao.save(tenantId, entityProfile);
    }

    @Override
    public void delete(TenantId tenantId, EntityProfileId id) {
        log.trace("delete for tenantId = {} and id = {}", tenantId, id);
        dao.removeById(tenantId, id);
    }
}
