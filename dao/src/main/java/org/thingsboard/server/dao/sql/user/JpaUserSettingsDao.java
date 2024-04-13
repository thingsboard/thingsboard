/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.dao.sql.user;

import io.hypersistence.utils.hibernate.type.array.StringArrayType;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.query.TypedParameterValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.settings.UserSettings;
import org.thingsboard.server.common.data.settings.UserSettingsCompositeKey;
import org.thingsboard.server.common.data.settings.UserSettingsType;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.UserSettingsEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDaoListeningExecutorService;
import org.thingsboard.server.dao.user.UserSettingsDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;

@Slf4j
@Component
@SqlDao
public class JpaUserSettingsDao extends JpaAbstractDaoListeningExecutorService implements UserSettingsDao {

    @Autowired
    private UserSettingsRepository userSettingsRepository;

    @Override
    public UserSettings save(TenantId tenantId, UserSettings userSettings) {
        return DaoUtil.getData(userSettingsRepository.save(new UserSettingsEntity(userSettings)));
    }

    @Override
    public UserSettings findById(TenantId tenantId, UserSettingsCompositeKey id) {
        return DaoUtil.getData(userSettingsRepository.findById(id));
    }

    @Override
    public void removeById(TenantId tenantId, UserSettingsCompositeKey id) {
        userSettingsRepository.deleteById(id);
    }

    @Override
    public List<UserSettings> findByTypeAndPath(TenantId tenantId, UserSettingsType type, String... path) {
        return DaoUtil.convertDataList(userSettingsRepository.findByTypeAndPathExisting(type.name(), new TypedParameterValue(StringArrayType.INSTANCE, path)));
    }

}
